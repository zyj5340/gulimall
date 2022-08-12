package com.zyj.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.zyj.common.exception.NoStockException;
import com.zyj.common.to.mq.OrderTo;
import com.zyj.common.to.mq.StockDetailTo;
import com.zyj.common.to.mq.StockLockedTo;
import com.zyj.common.utils.R;
import com.zyj.gulimall.ware.config.MyRabbitConfig;
import com.zyj.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.zyj.gulimall.ware.entity.WareOrderTaskEntity;
import com.zyj.gulimall.ware.feign.OrderFeignService;
import com.zyj.gulimall.ware.feign.ProductFeignService;
import com.zyj.gulimall.ware.service.WareOrderTaskService;
import com.zyj.gulimall.ware.vo.*;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyj.common.utils.PageUtils;
import com.zyj.common.utils.Query;

import com.zyj.gulimall.ware.dao.WareSkuDao;
import com.zyj.gulimall.ware.entity.WareSkuEntity;
import com.zyj.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private OrderFeignService orderFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WareOrderTaskService orderTaskService;

    @Autowired
    private WareOrderTaskDetailServiceImpl orderTaskDetailService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        /**
         * wareId: 123,//仓库id
         * skuId: 123//商品id
         */

        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId)){
            wrapper.eq("sku_id",skuId);
        }
        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId)){
            wrapper.eq("ware_id",wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }



    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //判断是否存在库存记录
        List<WareSkuEntity> wareSkuEntities = baseMapper.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId)
                .eq("ware_id", wareId));
        if (wareSkuEntities==null || wareSkuEntities.size()==0){
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            //远程查询skuname 如果失败无需回滚
            try {
                R info = productFeignService.info(skuId);
                if (info.getCode()==0){
                    Map<String,Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            }catch (Exception e){

            }
            baseMapper.insert(wareSkuEntity);
        }else {
            baseMapper.addStock(skuId,wareId,skuNum);
        }
    }



    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {


        List<SkuHasStockVo> collect = skuIds.stream().map(skuid -> {
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();
            //查询sku库存
            Long count = baseMapper.getSkuStock(skuid);
            skuHasStockVo.setSkuId(skuid);
            skuHasStockVo.setHasStock(count==null?false:count>0);
            return skuHasStockVo;
        }).collect(Collectors.toList());

        return collect;
    }

    /**
     * 为某个订单锁定库存
     * @param vo
     * @return
     */
    @Override
    @Transactional
    public Boolean orderLockStock(WareSkuLockVo vo) {
        //0.保存库存工作单详情 追溯
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(taskEntity);


        //1.找到每个商品在哪个仓库有库存
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> stocks = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            //查询商品在哪里有库存
            List<Long> wareId = baseMapper.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareId);
            stock.setNum(item.getCount());
            stock.setSkuId(skuId);
            return stock;
        }).collect(Collectors.toList());


        Boolean allLock = true;
        //2.锁定库存
        for (SkuWareHasStock stock : stocks) {
            Boolean skuStocked = false;
            Long skuId = stock.getSkuId();
            List<Long> wareIds = stock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                //没有仓库有库存
                throw new NoStockException(skuId);
            }

            for (Long wareId : wareIds) {
                //成功返回1，否则返回0
                /**
                 * 可能出现前几个商品库存锁定成功并发出mq而后面失败导致整体回滚
                 * 因此库存没有锁定而发出了解锁mq
                 */
                Long count = baseMapper.lockSkuStock(skuId,wareId,stock.getNum());
                if (count == 1){
                    skuStocked = true;
                    //TODO 告诉mq库存锁定成功 触发自动解锁逻辑
                    WareOrderTaskDetailEntity orderTaskDetail = new WareOrderTaskDetailEntity(null,skuId,null, stock.getNum(), taskEntity.getId(),wareId,1 );
                    orderTaskDetailService.save(orderTaskDetail);

                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(taskEntity.getId());
                    StockDetailTo detail = new StockDetailTo();
                    BeanUtils.copyProperties(orderTaskDetail,detail);
                    stockLockedTo.setDetail(detail);

                    rabbitTemplate.convertAndSend(MyRabbitConfig.STOCK_EVENT_EXCHANGE,MyRabbitConfig.STOCK_LOCKED_ROUTING_KEY,stockLockedTo);
                    break;
                }else {
                    //当前仓库锁失败 重试下一个仓库

                }
            }
            if (skuStocked == false){
                //所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }
        //全部锁定成功

        return null;
    }

    /**
     * 自动解锁库存
     * @param to
     */
    @Override
    public void unlockStock(StockLockedTo to){
        System.out.println("收到解锁库存的消息：" + to.getId());
        Long id = to.getId();//库存工作单id
        StockDetailTo detail = to.getDetail();
        Long skuId = detail.getSkuId();
        Long detailId = detail.getId();
        //解锁
        //1.查询数据库关于这个订单的锁定库存信息
        //库存锁定失败 工作单没有 整体回滚，无需解锁
        WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detailId);
        if (byId != null){
            //解锁
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            R r = orderFeignService.getOrderStatus(orderSn);// 由于拦截器拦截session而feign调用不带session会导致返回页面， 在拦截器中放行请求
            if (r.getCode() == 0){
                //订单数据返回成功
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });
                if (data == null || data.getStatus() == 4){
                    //订单已经被取消
                    if (byId.getLockStatus() == 1) {
                        //只有当前库存单处于锁定状态且未解锁才能解锁
                        unLock(skuId, detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            }else {
                //远程服务失败 消息拒绝，重新放入队列
                throw new RuntimeException("远程服务失败");
            }
        }else {
            //无需解锁
        }
    }


    /**
     *防止由于服务器卡顿  导致订单状态没有改变  库存消息优先到期 Wms无法解锁库存
     * @param orderTo
     */
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        System.out.println("收到解锁库存的消息：" + orderTo.getId());
        String orderSn = orderTo.getOrderSn();
        //查询最新库存解锁状态，防止重复解锁库存
        WareOrderTaskEntity task = orderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = task.getId();
        //按照工作单查询所有没有解锁的库存进行解锁
        List<WareOrderTaskDetailEntity> entities = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("task_id", id).
                eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : entities) {
            unLock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum(),entity.getTaskId());
        }


    }


    /**
     * 解锁商品
     * @param skuId
     * @param wareId
     */
    private void unLock(Long skuId, Long wareId,Integer num,Long taskDetail){
        //库存解锁
        baseMapper.unLock(skuId,wareId,num);
        //更新库存工作单的状态
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetail);
        entity.setLockStatus(2);
        orderTaskDetailService.updateById(entity);
    }

    @Data
    class SkuWareHasStock{
        private Long skuId;
        private List<Long> wareId;
        private Integer num;
    }
}
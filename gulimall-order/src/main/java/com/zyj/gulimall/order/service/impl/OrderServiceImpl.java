package com.zyj.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zyj.common.exception.NoStockException;
import com.zyj.common.to.mq.OrderTo;
import com.zyj.common.to.mq.SeckillOrderTo;
import com.zyj.common.utils.R;
import com.zyj.common.vo.MemberRespVo;
import com.zyj.gulimall.order.config.MyMQConfig;
import com.zyj.gulimall.order.constant.OrderConstant;
import com.zyj.gulimall.order.entity.OrderItemEntity;
import com.zyj.gulimall.order.entity.PaymentInfoEntity;
import com.zyj.gulimall.order.enume.OrderStatusEnum;
import com.zyj.gulimall.order.feign.CartFeignService;
import com.zyj.gulimall.order.feign.MemberFeignService;
import com.zyj.gulimall.order.feign.ProductFeignService;
import com.zyj.gulimall.order.feign.WmsFeignService;
import com.zyj.gulimall.order.interceptor.LoginUserInterceptor;
import com.zyj.gulimall.order.service.OrderItemService;
import com.zyj.gulimall.order.service.PaymentInfoService;
import com.zyj.gulimall.order.to.OrderCreateTo;
import com.zyj.gulimall.order.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyj.common.utils.PageUtils;
import com.zyj.common.utils.Query;

import com.zyj.gulimall.order.dao.OrderDao;
import com.zyj.gulimall.order.entity.OrderEntity;
import com.zyj.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private WmsFeignService wmsFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 订单确认页需要返回的数据
     *
     * @return
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo vo = new OrderConfirmVo();
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();


        //1.远程查询所有地址信息
        CompletableFuture<Void> getAddress = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> addresses = memberFeignService.getAddress(loginUser.getId());
            vo.setAddress(addresses);
        }, executor);

        //2.远程查询购物车选中的购物项
        /**
         * feign调用请求头丢失
         * 创建feign远程调用拦截器
         */
        CompletableFuture<Void> getItems = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            vo.setItems(items);
        }, executor).thenRunAsync(()->{
            List<Long> list = vo.getItems().stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R hasStock = wmsFeignService.getSkusHasStock(list);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null){
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::isHasStock));
                vo.setStocks(map);
            }
        },executor);

        //3.查询用户积分
        Integer integration = loginUser.getIntegration();
        vo.setIntegration(integration);

        //4.其他自动计算

        //TODO 5.防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        vo.setOrderToken(token);
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+loginUser.getId(),token,30, TimeUnit.MINUTES);
        CompletableFuture.allOf(getAddress,getItems).get();

        return vo;
    }

    /**
     * 下单
     *
     * @param vo
     * @return
     */
    @GlobalTransactional//seata分布式事物注解
    @Override
    @Transactional//本地事物
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {

        confirmVoThreadLocal.set(vo);

        //创建订单，令牌验证，价格验证，锁定库存...
        SubmitOrderResponseVo resp = new SubmitOrderResponseVo();
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();
        //1.验证令牌[令牌的对比和删除必须保证原子性]
        String token = vo.getOrderToken();
        resp.setCode(0);
        // 通过使用lua脚本进行原子性删除
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId()), token);
        if (result == 0L){
            //验证失败
            resp.setCode(1);
            return resp;
        }else {
            //验证成功
            //创建订单，令牌验证，价格验证，锁定库存...
            OrderCreateTo order = createOrder();
            BigDecimal payAmount = order.getOrder().getPayAmount();
            if (Math.abs(payAmount.subtract(vo.getPayPrice()).doubleValue()) < 0.01){
                //金额对比成功

                //TODO 3.保存订单
                saveOrder(order);

                //4.库存锁定,出现异常回滚订单数据
                //订单号，订单项
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemEntity> orderItems = order.getOrderItems();
                List<OrderItemVo> locks = orderItems.stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(locks);
                //TODO 4.远程锁库存
                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0){
                    //锁成功
                    resp.setOrder(order.getOrder());

                    //TODO 5.远程扣减积分

                    //TODO 订单创建成功向MQ发消息
                    rabbitTemplate.convertAndSend(MyMQConfig.ORDER_EVENT_EXCHANGE,MyMQConfig.ORDER_CREATE_ROUTING_KEY,order.getOrder());

                    return resp;
                }else {
                    //失败
                    resp.setCode(3);
                    throw new NoStockException();//抛异常回滚
                }
            }else {
                resp.setCode(2);
                return resp;
            }
        }

        /**
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();
        String rawToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId());
        if (token != null && token.equals(rawToken)){
            //令牌验证通过


            return resp;
        }else {

            return resp;
        }
        */
    }


    /**
     * 根据orderSn 查取订单信息
     * @param orderSn
     * @return
     */
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity entity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return entity;
    }


    /**
     * 订单保存
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
//        orderEntity.setStatus(4);
        this.save(orderEntity);

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);

    }


    /**
     * 创建订单
     * @return
     */
    private OrderCreateTo createOrder(){
        OrderCreateTo to = new OrderCreateTo();
        //1.生成订单号
        OrderEntity orderEntity = buildOrder();

        //2.获取订单项信息
        List<OrderItemEntity> itemEntities = buildOrderItems(orderEntity.getOrderSn());

        //3.计算价格,积分相关信息
        computePrice(orderEntity,itemEntities);
        to.setOrder(orderEntity);
        to.setOrderItems(itemEntities);
        return to;
    }

    /**
     * 计算订单价格
     * @param orderEntity
     * @param itemEntities
     */
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        //订单总额，叠加每一个订单项的总额信息
        for (OrderItemEntity entity : itemEntities) {
            BigDecimal realAmount = entity.getRealAmount();
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            total = total.add(realAmount);
            gift = gift.add(new BigDecimal(entity.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(entity.getGiftGrowth().toString()));

        }
        //1.订单价格相关
        orderEntity.setTotalAmount(total);
        //应付金额
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));

        orderEntity.setPromotionAmount(promotion);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);

        //设置积分信息
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());
        orderEntity.setDeleteStatus(0);//0代表未删除
    }


    /**
     * 构建订单信息
     * @return
     */
    private OrderEntity buildOrder() {
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();
        OrderEntity entity = new OrderEntity();
        String orderSn = IdWorker.getTimeId();
        entity.setOrderSn(orderSn);
        entity.setMemberId(loginUser.getId());

        //设置运费信息
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        R fare = wmsFeignService.getFare(submitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        entity.setFreightAmount(fareResp.getFare());

        //获取收货地址信息
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverName(fareResp.getAddress().getName());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());

        //设置订单的相关状态信息
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);


        return entity;
    }


    /**
     * 构建所有订单项
     * @param
     * @param orderSn
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0){
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }


    /**
     * 构建每一个订单项
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();

        //1.订单信息，订单号

        //2.商品的SPU信息
        Long skuId = cartItem.getSkuId();
        R resp = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = resp.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());

        //3.商品的SKU信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());


        //4.优惠信息(暂不做)


        //5.积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());

        //6.设置价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0.0"));
        itemEntity.setCouponAmount(new BigDecimal("0.0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0.0"));
        BigDecimal origin = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        BigDecimal real = origin.subtract(itemEntity.getPromotionAmount()).subtract(itemEntity.getCouponAmount()).subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(real);

        return itemEntity;
    }


    /**
     * 关闭订单
     *
     * @param entity
     */
    @Override
    public void closeOrder(OrderEntity entity) {
        //查询订单最新状态
        OrderEntity orderEntity = this.getById(entity.getId());
        if (orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()){
            //关闭订单
            OrderEntity update = new OrderEntity();
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            update.setId(orderEntity.getId());
            this.updateById(update);
            OrderTo to = new OrderTo();
            BeanUtils.copyProperties(orderEntity,to);

            //发送MQ
            try {
                //保证消息一定会被发送，每个发送的消息做日志记录（数据库保存详细信息 以及状态信息）
                rabbitTemplate.convertAndSend(MyMQConfig.ORDER_EVENT_EXCHANGE,MyMQConfig.ORDER_RELEASE_OTHER,to);
            } catch (AmqpException e) {
                //将没法送成功的消息进行重试操作
            }

        }
    }

    /**
     * 获取当前订单的支付信息
     *
     * @param orderSn
     * @return
     */
    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity entity = this.getOrderByOrderSn(orderSn);
        List<OrderItemEntity> items = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));

        BigDecimal bigDecimal = entity.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(bigDecimal.toString());
        payVo.setOut_trade_no(entity.getOrderSn());
        OrderItemEntity item = items.get(0);
        payVo.setSubject(item.getSkuName());
        payVo.setBody(item.getSkuAttrsVals());
        return payVo;
    }

    /**
     * 根据用户查询订单项信息
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id",loginUser.getId())
                        .orderByDesc("id")
        );

        List<OrderEntity> collect = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(itemEntities);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(collect);

        return new PageUtils(page);
    }

    /**
     * 处理支付宝返回数据
     *
     * @param vo
     * @return
     */
    @Override
    public String handleResult(PayAsyncVo vo) {
        //1.保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(infoEntity);

        //2.修改订单状态信息
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")){
            //支付成功状态
            String orderSn = vo.getOut_trade_no();
            baseMapper.updateOrderStatus(orderSn,OrderStatusEnum.PAYED.getCode());
        }

        return "success";
    }

    /**
     * 创建秒杀单详细信息
     *
     */
    @Override
    public void createSeckillOredr(SeckillOrderTo to) {
        //TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(to.getOrderSn());
        orderEntity.setMemberId(to.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal amount = to.getNum().multiply(to.getSeckillPrice());
        orderEntity.setPayAmount(amount);
        this.save(orderEntity);

        //TODO 保存订单项信息
        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setOrderSn(to.getOrderSn());
        itemEntity.setRealAmount(amount);
        itemEntity.setSkuQuantity(to.getNum().intValue());

        orderItemService.save(itemEntity);
    }

    @RabbitListener
    public void testListener(){

    }
}
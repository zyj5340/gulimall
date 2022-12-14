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
     * ????????????????????????????????????
     *
     * @return
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo vo = new OrderConfirmVo();
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();


        //1.??????????????????????????????
        CompletableFuture<Void> getAddress = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> addresses = memberFeignService.getAddress(loginUser.getId());
            vo.setAddress(addresses);
        }, executor);

        //2.???????????????????????????????????????
        /**
         * feign?????????????????????
         * ??????feign?????????????????????
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

        //3.??????????????????
        Integer integration = loginUser.getIntegration();
        vo.setIntegration(integration);

        //4.??????????????????

        //TODO 5.????????????
        String token = UUID.randomUUID().toString().replace("-", "");
        vo.setOrderToken(token);
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+loginUser.getId(),token,30, TimeUnit.MINUTES);
        CompletableFuture.allOf(getAddress,getItems).get();

        return vo;
    }

    /**
     * ??????
     *
     * @param vo
     * @return
     */
    @GlobalTransactional//seata?????????????????????
    @Override
    @Transactional//????????????
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {

        confirmVoThreadLocal.set(vo);

        //?????????????????????????????????????????????????????????...
        SubmitOrderResponseVo resp = new SubmitOrderResponseVo();
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();
        //1.????????????[?????????????????????????????????????????????]
        String token = vo.getOrderToken();
        resp.setCode(0);
        // ????????????lua???????????????????????????
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId()), token);
        if (result == 0L){
            //????????????
            resp.setCode(1);
            return resp;
        }else {
            //????????????
            //?????????????????????????????????????????????????????????...
            OrderCreateTo order = createOrder();
            BigDecimal payAmount = order.getOrder().getPayAmount();
            if (Math.abs(payAmount.subtract(vo.getPayPrice()).doubleValue()) < 0.01){
                //??????????????????

                //TODO 3.????????????
                saveOrder(order);

                //4.????????????,??????????????????????????????
                //?????????????????????
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
                //TODO 4.???????????????
                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0){
                    //?????????
                    resp.setOrder(order.getOrder());

                    //TODO 5.??????????????????

                    //TODO ?????????????????????MQ?????????
                    rabbitTemplate.convertAndSend(MyMQConfig.ORDER_EVENT_EXCHANGE,MyMQConfig.ORDER_CREATE_ROUTING_KEY,order.getOrder());

                    return resp;
                }else {
                    //??????
                    resp.setCode(3);
                    throw new NoStockException();//???????????????
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
            //??????????????????


            return resp;
        }else {

            return resp;
        }
        */
    }


    /**
     * ??????orderSn ??????????????????
     * @param orderSn
     * @return
     */
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity entity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return entity;
    }


    /**
     * ????????????
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
     * ????????????
     * @return
     */
    private OrderCreateTo createOrder(){
        OrderCreateTo to = new OrderCreateTo();
        //1.???????????????
        OrderEntity orderEntity = buildOrder();

        //2.?????????????????????
        List<OrderItemEntity> itemEntities = buildOrderItems(orderEntity.getOrderSn());

        //3.????????????,??????????????????
        computePrice(orderEntity,itemEntities);
        to.setOrder(orderEntity);
        to.setOrderItems(itemEntities);
        return to;
    }

    /**
     * ??????????????????
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
        //??????????????????????????????????????????????????????
        for (OrderItemEntity entity : itemEntities) {
            BigDecimal realAmount = entity.getRealAmount();
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            total = total.add(realAmount);
            gift = gift.add(new BigDecimal(entity.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(entity.getGiftGrowth().toString()));

        }
        //1.??????????????????
        orderEntity.setTotalAmount(total);
        //????????????
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));

        orderEntity.setPromotionAmount(promotion);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);

        //??????????????????
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());
        orderEntity.setDeleteStatus(0);//0???????????????
    }


    /**
     * ??????????????????
     * @return
     */
    private OrderEntity buildOrder() {
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();
        OrderEntity entity = new OrderEntity();
        String orderSn = IdWorker.getTimeId();
        entity.setOrderSn(orderSn);
        entity.setMemberId(loginUser.getId());

        //??????????????????
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        R fare = wmsFeignService.getFare(submitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        entity.setFreightAmount(fareResp.getFare());

        //????????????????????????
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverName(fareResp.getAddress().getName());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());

        //?????????????????????????????????
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);


        return entity;
    }


    /**
     * ?????????????????????
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
     * ????????????????????????
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();

        //1.????????????????????????

        //2.?????????SPU??????
        Long skuId = cartItem.getSkuId();
        R resp = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = resp.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());

        //3.?????????SKU??????
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());


        //4.????????????(?????????)


        //5.????????????
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());

        //6.??????????????????
        itemEntity.setPromotionAmount(new BigDecimal("0.0"));
        itemEntity.setCouponAmount(new BigDecimal("0.0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0.0"));
        BigDecimal origin = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        BigDecimal real = origin.subtract(itemEntity.getPromotionAmount()).subtract(itemEntity.getCouponAmount()).subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(real);

        return itemEntity;
    }


    /**
     * ????????????
     *
     * @param entity
     */
    @Override
    public void closeOrder(OrderEntity entity) {
        //????????????????????????
        OrderEntity orderEntity = this.getById(entity.getId());
        if (orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()){
            //????????????
            OrderEntity update = new OrderEntity();
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            update.setId(orderEntity.getId());
            this.updateById(update);
            OrderTo to = new OrderTo();
            BeanUtils.copyProperties(orderEntity,to);

            //??????MQ
            try {
                //??????????????????????????????????????????????????????????????????????????????????????????????????? ?????????????????????
                rabbitTemplate.convertAndSend(MyMQConfig.ORDER_EVENT_EXCHANGE,MyMQConfig.ORDER_RELEASE_OTHER,to);
            } catch (AmqpException e) {
                //?????????????????????????????????????????????
            }

        }
    }

    /**
     * ?????????????????????????????????
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
     * ?????????????????????????????????
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
     * ???????????????????????????
     *
     * @param vo
     * @return
     */
    @Override
    public String handleResult(PayAsyncVo vo) {
        //1.??????????????????
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(infoEntity);

        //2.????????????????????????
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")){
            //??????????????????
            String orderSn = vo.getOut_trade_no();
            baseMapper.updateOrderStatus(orderSn,OrderStatusEnum.PAYED.getCode());
        }

        return "success";
    }

    /**
     * ???????????????????????????
     *
     */
    @Override
    public void createSeckillOredr(SeckillOrderTo to) {
        //TODO ??????????????????
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(to.getOrderSn());
        orderEntity.setMemberId(to.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal amount = to.getNum().multiply(to.getSeckillPrice());
        orderEntity.setPayAmount(amount);
        this.save(orderEntity);

        //TODO ?????????????????????
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
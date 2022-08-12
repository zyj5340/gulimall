package com.zyj.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyj.common.to.mq.SeckillOrderTo;
import com.zyj.common.utils.PageUtils;
import com.zyj.gulimall.order.entity.OrderEntity;
import com.zyj.gulimall.order.vo.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author YongjieZhang
 * @email 534054720@qq.com
 * @date 2022-04-30 17:23:28
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 订单确认页需要返回的数据
     * @return
     */
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    /**
     * 下单
     * @param vo
     * @return
     */
    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    OrderEntity getOrderByOrderSn(String orderSn);


    /**
     * 关闭订单
     * @param orderEntity
     */
    void closeOrder(OrderEntity orderEntity);


    /**
     * 获取当前订单的支付信息
     * @param orderSn
     * @return
     */
    PayVo getOrderPay(String orderSn);


    /**
     * 根据用户查询订单项信息
     * @param params
     * @return
     */
    PageUtils queryPageWithItem(Map<String, Object> params);


    /**
     * 处理支付宝返回数据
     * @param vo
     * @return
     */
    String handleResult(PayAsyncVo vo);

    /**
     * 创建秒杀单详细信息
     * @param seckillOrderTo
     */
    void createSeckillOredr(SeckillOrderTo seckillOrderTo);
}


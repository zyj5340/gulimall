package com.zyj.gulimall.order.listener;

import com.rabbitmq.client.Channel;
import com.zyj.gulimall.order.config.MyMQConfig;
import com.zyj.gulimall.order.entity.OrderEntity;
import com.zyj.gulimall.order.service.OrderService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author Zyj
 * @date 2022/7/14
 */
@Service
@RabbitListener(queues = MyMQConfig.ORDER_RELEASE_QUEUE)
public class OrderCloseListener {

    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void closeOrder(OrderEntity orderEntity, Channel channel, Message message) throws IOException {
        try {
            System.out.println("收到过期订单,准备关闭：" + orderEntity.getOrderSn());
            orderService.closeOrder(orderEntity);
            //手动调用支付宝收单 防止在最后时刻用户支付而订单已经关闭

            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }
}

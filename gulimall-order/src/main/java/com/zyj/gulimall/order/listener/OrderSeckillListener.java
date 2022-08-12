package com.zyj.gulimall.order.listener;

import com.rabbitmq.client.Channel;
import com.zyj.common.to.mq.SeckillOrderTo;
import com.zyj.gulimall.order.entity.OrderEntity;
import com.zyj.gulimall.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author Zyj
 * @date 2022/7/17
 */
@Slf4j
@Component
@RabbitListener(queues = "order.seckill.order.queue")
public class OrderSeckillListener {

    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void seckillOrder(SeckillOrderTo seckillOrderTo, Channel channel, Message message) throws IOException {
        try {
            log.info("准备创建秒杀订单详细信息...");
            orderService.createSeckillOredr(seckillOrderTo);
            //手动调用支付宝收单 防止在最后时刻用户支付而订单已经关闭

            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }
}

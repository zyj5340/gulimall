package com.zyj.gulimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.zyj.common.to.mq.OrderTo;
import com.zyj.common.to.mq.StockDetailTo;
import com.zyj.common.to.mq.StockLockedTo;
import com.zyj.common.utils.R;
import com.zyj.gulimall.ware.config.MyRabbitConfig;
import com.zyj.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.zyj.gulimall.ware.entity.WareOrderTaskEntity;
import com.zyj.gulimall.ware.service.WareSkuService;
import com.zyj.gulimall.ware.vo.OrderVo;
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
@RabbitListener(queues = MyRabbitConfig.STOCK_RELEASE_QUEUE)
public class StockReleaseListener {

    @Autowired
    private WareSkuService wareSkuService;


    /**
     *
     * 1.下单成功 库存锁定成功 接下来的业务调用失败导致订单回滚 因此之前锁定的库存需要解锁
     * 2.下单失败，锁库存失败
     * @param to
     * @param message
     */
    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        try {
            //当前消息是否被第二次及以后重新派发
//            Boolean redelivered = message.getMessageProperties().getRedelivered();
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }

    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("收到订单关闭的消息：" + message.getMessageProperties().getDeliveryTag());
        try {
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);

        }
    }
}

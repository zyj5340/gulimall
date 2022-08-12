package com.zyj.gulimall.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Zyj
 * @date 2022/7/14
 */
@Configuration
public class MyMQConfig {

    public static final String ORDER_RELEASE_OTHER = "order.release.other.#";

    public static final String ORDER_CREATE_ROUTING_KEY = "order.create.order";

    public static final String ORDER_EVENT_EXCHANGE = "order-event-exchange";

    public static final String ORDER_RELEASE_QUEUE = "order.release.order.queue";

    //    public Queue(String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments)
    @Bean
    public Queue orderDelayQueue(){
        Map<String,Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange","order-event-exchange");
        arguments.put("x-dead-letter-routing-key","order.release.order");
        arguments.put("x-message-ttl",60000);
        return new Queue("order.delay.queue",true,false,false,arguments);
    }

    @Bean
    public Queue orderReleaseOrderQueue(){
        return new Queue("order.release.order.queue",true,false,false);

    }


    //    public TopicExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments)
    @Bean
    public Exchange orderEventExchange(){
        return new TopicExchange("order-event-exchange",true,false);
    }


    //    public Binding(String destination, Binding.DestinationType destinationType, String exchange, String routingKey, Map<String, Object> arguments)
    @Bean
    public Binding orderCreateBinding(){
        return new Binding("order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.create.order",
                null);
    }

    @Bean
    public Binding orderReleaseBinding(){
        return new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order",
                null);
    }

    /**
     * 订单释放和库存释放进行绑定
     */
    @Bean
    public Binding orderReleaseOther(){
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                ORDER_EVENT_EXCHANGE,
                "order.release.other.#",
                null);
    }

    /**
     * 秒杀削峰队列
     * @return
     */
    @Bean
    public Queue orderSeckillOrderQueue(){
        return new Queue("order.seckill.order.queue",true,false,false);
    }

    @Bean
    public Binding orderSeckillOrderQueueBinding(){
        return new Binding("order.seckill.order.queue",
                Binding.DestinationType.QUEUE,
                ORDER_EVENT_EXCHANGE,
                "order.seckill.order",
                null);
    }
}

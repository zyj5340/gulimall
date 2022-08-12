package com.zyj.gulimall.ware.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Zyj
 * @date 2022/7/2
 */
@Configuration
public class MyRabbitConfig {
    public static final String STOCK_EVENT_EXCHANGE = "stock-event-exchange";

    public static final String STOCK_LOCKED_ROUTING_KEY = "stock.locked";

    public static final String STOCK_RELEASE_QUEUE = "stock.release.stock.queue";

    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }


    @Bean
    public Exchange stockEventExchange(){
        return new TopicExchange("stock-event-exchange",
                true,
                false);
    }


    @Bean
    public Queue stockReleaseStockQueue(){
        return new Queue("stock.release.stock.queue",
                true,
                false,
                false);
    }

    @Bean
    public Queue stockDelayQueue(){
        Map<String,Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange","stock-event-exchange");
        arguments.put("x-dead-letter-routing-key","stock.release");
        arguments.put("x-message-ttl",90000);
        return new Queue("stock.delay.queue",
                true,
                false,
                false,
                arguments);
    }

    @Bean
    public Binding stockReleaseBinding(){
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.release.#",
                null);
    }

    @Bean
    public Binding stockLockedBinding(){
        return new Binding("stock.delay.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.locked",
                null);
    }

    @RabbitListener
    public void testListener(){

    }
}

package com.zyj.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author Zyj
 * @date 2022/7/2
 */
@Configuration
public class MyRabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制rabbittemplate
     */
    @PostConstruct//对象创建完成后，执行该方法
    public void initRabbitTemplate(){
        /**
         * 消息抵达交换机回调
         * ====消息可靠投递=====
         * 1.消息确认机制（publisher，consumer【手动ACK】）
         * 2.每个消息都在数据库做好记录 定期将失败的消息重新发送
         */
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                //服务器收到
                System.out.println("confirm...correlataionData["+correlationData+"] ===>ack["+b+"]====>cause:"+s);
            }
        });

        /**
         * 消息抵达队列回调
         */
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            //失败回调
            @Override
            public void returnedMessage(Message message, int i, String s, String s1, String s2) {
                //报错。修改数据库当前消息的状态
                System.out.println("fail message:["+message+"],replyCode:["+i+"]===>"+s+s1+s2);
            }
        });

    }
}

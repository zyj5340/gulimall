package com.zyj.gulimall.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Zyj
 * @date 2022/5/19
 */
@Configuration
public class MyRedissonConfig {

    @Bean
    public RedissonClient redissonClient(){

        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.81.133:6379");
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}

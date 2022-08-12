package com.zyj.gulimall.seckill.sechedule;

import com.zyj.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author Zyj
 * @date 2022/7/16
 * 秒杀商品定时上架
 */
@Service
@Slf4j
public class SeckillSkuSchedule {
    
    public static final String upload_lock = "seckill:upload:lock";
    
    @Autowired
    private SeckillService seckillService;

    @Autowired
    private RedissonClient redissonClient;

    @Scheduled(cron = "0/30 * * * * ?")
    public void uploadSeckillSkuLatest3Days(){
        //TODO 商品上架幂等性
        //加分布式锁
        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10, TimeUnit.SECONDS);
        log.info("秒杀商品上架...");
        try {
            seckillService.uploadSeckillSkuLatest3Days();
        } finally {
            lock.unlock();
        }
    }
}

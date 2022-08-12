package com.zyj.gulimall.seckill.sechedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * @author Zyj
 * @date 2022/7/16
 */
//开启定时任务
//@EnableScheduling
//@Component
@Slf4j
//@EnableAsync
public class HelloSchedule {

    @Async
    @Scheduled(cron = "* * * * * ?")
    public void hello(){
        log.info("hello...");
    }
}

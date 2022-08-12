package com.zyj.gulimall.seckill.service;

import com.zyj.gulimall.seckill.to.SeckillSkuRedisTo;

import java.util.List;

/**
 * @author Zyj
 * @date 2022/7/16
 */
public interface SeckillService {


    /**
     * 上架最近3内的商品
     */
    void uploadSeckillSkuLatest3Days();

    /**
     * 获取当前可以参与秒杀的商品
     * @return
     */
    List<SeckillSkuRedisTo> getCurrentSeckillSkus();


    /**
     * 获取对应sku秒杀信息
     * @param skuId
     * @return
     */
    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);


    /**
     * 秒杀业务
     * @param killId
     * @param key
     * @param num
     * @return
     */
    String kill(String killId, String key, Integer num);
}

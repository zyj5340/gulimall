package com.zyj.gulimall.seckill.to;

import com.zyj.gulimall.seckill.vo.SkuInfoVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Zyj
 * @date 2022/7/16
 */
@Data
public class SeckillSkuRedisTo {
    /**
     * id
     */
    private Long id;
    /**
     * 活动id
     */
    private Long promotionId;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private BigDecimal seckillCount;
    /**
     * 每人限购数量
     */
    private BigDecimal seckillLimit;
    /**
     * 排序
     */
    private Integer seckillSort;

    /**
     * sku详细信息
     */
    private SkuInfoVo skuInfoVo;


    /**
     * 秒杀随机码
     */
    private String randomCode;


    //当前商品秒杀的开始时间和结束时间
    private Long startTime;

    private Long endTime;
}

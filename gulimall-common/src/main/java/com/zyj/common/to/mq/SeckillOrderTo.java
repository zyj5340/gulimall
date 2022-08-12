package com.zyj.common.to.mq;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Zyj
 * @date 2022/7/17
 */
@Data
public class SeckillOrderTo {
    private String orderSn;//订单号
    private Long promotionSessionId;//场次id
    private Long skuId;//
    private BigDecimal seckillPrice;//秒杀价格
    private BigDecimal num ;//数量
    private Long memberId;//会员id

}

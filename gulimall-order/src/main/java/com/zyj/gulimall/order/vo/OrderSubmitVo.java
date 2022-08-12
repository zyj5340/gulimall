package com.zyj.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Zyj
 * @date 2022/7/12
 * 封装订单提交数据
 */
@Data
public class OrderSubmitVo {
    private Long addrId;

    private Integer payType;

    //去购物车获取商品

    private String OrderToken;

    private BigDecimal payPrice;

    //用户相关信息去session中获取

    private String note;//订单备注
}

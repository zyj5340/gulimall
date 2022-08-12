package com.zyj.gulimall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Zyj
 * @date 2022/7/3
 * 订单确认页需要的数据
 */

public class OrderConfirmVo {

    //收货地址
    @Getter @Setter
    private List<MemberAddressVo> address;

    //选中的购物项
    @Getter @Setter
    private List<OrderItemVo> items;

    //发票记录

    //优惠券信息
    @Getter @Setter
    private Integer integration;

    //防止重复提交 防重令牌
    @Getter @Setter
    private String orderToken;

    private Integer count;

    @Getter @Setter
    private Map<Long,Boolean> stocks;

    //订单总额
    private BigDecimal total;

    public BigDecimal getTotal(){
        BigDecimal total = new BigDecimal("0");
        if (items!=null) {
            for (OrderItemVo item : items) {
                BigDecimal totalPrice = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                total = total.add(totalPrice);
            }
        }
        return total;
    }

    //应付价格
    private BigDecimal payPrice;

    public BigDecimal getPayPrice(){
        BigDecimal total = new BigDecimal("0");
        if (items!=null) {
            for (OrderItemVo item : items) {
                BigDecimal totalPrice = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                total = total.add(totalPrice);
            }
        }
        return total;
    }

    public Integer getCount(){
        Integer i = 0;
        if (items!=null) {
            for (OrderItemVo item : items) {
                i += item.getCount();
            }
        }
        return i;
    }
}

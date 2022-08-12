package com.zyj.gulimall.order.to;

import com.zyj.gulimall.order.entity.OrderEntity;
import com.zyj.gulimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Zyj
 * @date 2022/7/12
 */
@Data
public class OrderCreateTo {

    private OrderEntity order;

    private List<OrderItemEntity> orderItems;

    private BigDecimal payPrice;//应付价格

    private BigDecimal fare;//运费
}

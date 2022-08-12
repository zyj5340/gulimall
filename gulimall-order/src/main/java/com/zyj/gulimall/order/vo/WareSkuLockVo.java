package com.zyj.gulimall.order.vo;

import lombok.Data;

import java.util.List;

/**
 * @author Zyj
 * @date 2022/7/13
 */
@Data
public class WareSkuLockVo {

    private String orderSn;//订单号

    private List<OrderItemVo> locks;

}

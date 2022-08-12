package com.zyj.gulimall.order.vo;

import lombok.Data;

/**
 * @author Zyj
 * @date 2022/7/6
 */
@Data
public class SkuStockVo {
    private Long skuId;
    private boolean hasStock;
}

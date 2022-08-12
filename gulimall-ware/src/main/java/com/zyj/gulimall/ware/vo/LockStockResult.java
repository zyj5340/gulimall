package com.zyj.gulimall.ware.vo;

import lombok.Data;

/**
 * @author Zyj
 * @date 2022/7/13
 */
@Data
public class LockStockResult {

    private Long skuId;

    private Integer num;

    private Boolean locked;
}

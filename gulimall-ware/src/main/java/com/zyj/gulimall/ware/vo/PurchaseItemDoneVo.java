package com.zyj.gulimall.ware.vo;

import lombok.Data;

/**
 * @author Stone
 * @date 2022/5/9
 */
@Data
public class PurchaseItemDoneVo {

    private Long itemId;

    private Integer status;

    private String reason;
}

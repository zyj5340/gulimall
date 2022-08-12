package com.zyj.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @author Stone
 * @date 2022/5/9
 */
@Data
public class MergeVo {
    private Long purchaseId;
    private List<Long> items;
}

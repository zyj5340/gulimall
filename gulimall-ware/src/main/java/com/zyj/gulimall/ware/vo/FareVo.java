package com.zyj.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Zyj
 * @date 2022/7/11
 */
@Data
public class FareVo {
    private MemberAddressVo address;
    private BigDecimal fare;
}

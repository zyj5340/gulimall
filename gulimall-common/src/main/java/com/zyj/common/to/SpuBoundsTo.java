package com.zyj.common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Stone
 * @date 2022/5/8
 */
@Data
public class SpuBoundsTo {
    private Long spuId;
    private BigDecimal buyBounds;
    private BigDecimal growBounds;
}

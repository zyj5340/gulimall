package com.zyj.common.to.mq;

import lombok.Data;

import java.util.List;

/**
 * @author Zyj
 * @date 2022/7/14
 */
@Data
public class StockLockedTo {

    private Long id;//库存工作单id

    private StockDetailTo detail;//工作单详情id
}

package com.zyj.gulimall.order.vo;

import com.zyj.gulimall.order.entity.OrderEntity;
import lombok.Data;

/**
 * @author Zyj
 * @date 2022/7/12
 */
@Data
public class SubmitOrderResponseVo {
    private OrderEntity order;
    private Integer code;//0成功 其他失败
}

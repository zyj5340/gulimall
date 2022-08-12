package com.zyj.gulimall.order.dao;

import com.zyj.gulimall.order.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项信息
 * 
 * @author YongjieZhang
 * @email 534054720@qq.com
 * @date 2022-04-30 17:23:28
 */
@Mapper
public interface OrderItemDao extends BaseMapper<OrderItemEntity> {
	
}

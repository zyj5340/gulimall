package com.zyj.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyj.common.to.mq.OrderTo;
import com.zyj.common.to.mq.StockLockedTo;
import com.zyj.common.utils.PageUtils;
import com.zyj.gulimall.ware.entity.WareSkuEntity;
import com.zyj.gulimall.ware.vo.LockStockResult;
import com.zyj.gulimall.ware.vo.SkuHasStockVo;
import com.zyj.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author YongjieZhang
 * @email 534054720@qq.com
 * @date 2022-04-30 17:28:38
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo vo);


    void unlockStock(StockLockedTo to);

    void unlockStock(OrderTo orderTo);

}


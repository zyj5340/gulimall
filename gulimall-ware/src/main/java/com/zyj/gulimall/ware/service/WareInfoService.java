package com.zyj.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyj.common.utils.PageUtils;
import com.zyj.gulimall.ware.entity.WareInfoEntity;
import com.zyj.gulimall.ware.vo.FareVo;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 仓库信息
 *
 * @author YongjieZhang
 * @email 534054720@qq.com
 * @date 2022-04-30 17:28:38
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 根据收货地址计算运费
     * @param addrId
     * @return
     */
    FareVo getFare(Long addrId);
}


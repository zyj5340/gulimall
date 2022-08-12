package com.zyj.gulimall.product.service.impl;

import com.zyj.gulimall.product.vo.SkuItemVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyj.common.utils.PageUtils;
import com.zyj.common.utils.Query;

import com.zyj.gulimall.product.dao.SkuSaleAttrValueDao;
import com.zyj.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.zyj.gulimall.product.service.SkuSaleAttrValueService;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }


    /**
     * 获取skuitem销售属性
     * @param spuId
     * @return
     */
    @Override
    public List<SkuItemVo.SkuItemSaleAttrsVo> getSaleAttrsBySpuId(Long spuId) {
        SkuSaleAttrValueDao skuSaleAttrValueDao = getBaseMapper();
        List<SkuItemVo.SkuItemSaleAttrsVo> salesAttrVos = skuSaleAttrValueDao.getSaleAttrsBySpuId(spuId);
        return salesAttrVos;
    }



    @Override
    public List<String> getSkuSaleAttrValuesAsStringList(Long skuId) {
        List<String> list = baseMapper.getSkuSaleAttrValuesAsStringList(skuId);
        return list;
    }

}
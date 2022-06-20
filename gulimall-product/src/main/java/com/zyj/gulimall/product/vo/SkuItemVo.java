package com.zyj.gulimall.product.vo;

import com.zyj.gulimall.product.entity.SkuImagesEntity;
import com.zyj.gulimall.product.entity.SkuInfoEntity;
import com.zyj.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @author Zyj
 * @date 2022/6/8
 */
@Data
public class SkuItemVo {
    //1.sku基本信息
    SkuInfoEntity info;
    //2.sku图片信息
    List<SkuImagesEntity> images;
    //3.获取spu销售属性
    List<SkuItemSaleAttrsVo> saleAttr;

    //4.获取spu介绍
    SpuInfoDescEntity desp;

    //5.获取spu规格参数
    List<SpuItemAttrGroupVo> groupAttrs;

    //6.是否有货
    boolean hasStock = true;

    @Data
    public static class SkuItemSaleAttrsVo {
        private Long attrId;
        private String attrName;
        private List<AttrValueWithAttrIdVo> attrValues;

    }

    @Data
    public static class AttrValueWithAttrIdVo{
        private String attrValue;
        private String skuIds;
    }

    @ToString
    @Data
    public static class SpuItemAttrGroupVo{
        private String groupName;//属性分组名称
        private List<SpuBaseAttrVo> attrs;

    }

    @ToString
    @Data
    public static class SpuBaseAttrVo{
        private String attrName;
        private String attrValue;
    }
}

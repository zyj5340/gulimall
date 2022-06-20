package com.zyj.gulimall.search.vo;

import com.zyj.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Zyj
 * @date 2022/5/21
 */
@Data
public class SearchResult {

    //商品信息
    private List<SkuEsModel> products;

    //分页信息
    private Integer pageNum;//当前页面
    private Long total;//总记录数
    private Integer totalPages;//总页码
    private  List<Integer> pageNavs;//导航页
    private List<BrandVo> brands;//当前查询所有涉及到的品牌
    private List<CatalogVo> catalogs;
    private List<AttrVo> attrs;

    private List<NavVo> navs = new ArrayList<>();//面包屑导航
    private List<Long> attrIds = new ArrayList<>();
    //==================================>以上返给页面的所有信息

    @Data
    public static class NavVo{
        private String navName;
        private String navValue;
        private String link;
    }


    @Data
    public static class BrandVo{
        private Long brandId;
        private String brandName;
        private String brandImg;
    }


    @Data
    public static class AttrVo{
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }

    @Data
    public static class CatalogVo{
        private Long catalogId;
        private String catalogName;
    }
}

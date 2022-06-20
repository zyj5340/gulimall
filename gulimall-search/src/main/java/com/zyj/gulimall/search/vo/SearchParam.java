package com.zyj.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装查询条件
 * @author Zyj
 * @date 2022/5/20
 */
@Data
public class SearchParam {
    private String keyword;//全文匹配关键字
    private Long catalog3Id;//三级分类Id
    /**
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     */
    private String sort;//排序条件

    /**
     * 过滤条件
     * hasStock, skuPrice, brandId, catalog3Id, attrs
     */
    private Integer hasStock;
    private String skuPrice;
    private List<Long> brandId;
    private List<String> attrs;
    private Integer pageNum = 1;

    private String _queryString;//原生的所有查询条件
}

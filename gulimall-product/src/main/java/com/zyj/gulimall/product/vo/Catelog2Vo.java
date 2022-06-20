package com.zyj.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Stone
 * @date 2022/5/17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Catelog2Vo {
    private String catalog1Id;  //一级父分类
    private List<Catelog3Vo> catalog3List;  //三级子分类
    private String id;
    private String name;


    /**
     * 三级分类Vo
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Catelog3Vo{
        private String catalog2Id;
        private String id;
        private String name;
    }
}

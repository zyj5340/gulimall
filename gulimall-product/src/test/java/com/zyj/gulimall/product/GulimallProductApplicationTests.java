package com.zyj.gulimall.product;

import com.zyj.gulimall.product.dao.AttrGroupDao;
import com.zyj.gulimall.product.dao.SkuSaleAttrValueDao;
import com.zyj.gulimall.product.entity.BrandEntity;
import com.zyj.gulimall.product.service.BrandService;
import com.zyj.gulimall.product.vo.SkuItemVo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GulimallProductApplicationTests {
    @Autowired
    BrandService brandService;
    @Autowired
    private AttrGroupDao attrGroupDao;
    @Autowired
    private SkuSaleAttrValueDao skuSaleAttrValueDao;


    @Test
    public void testForAttrGroupDao(){
        List<SkuItemVo.SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(4L, 225L);
        System.out.println(attrGroupWithAttrsBySpuId);
    }

    @Test
    public void testForSkuSaleDao(){
        List<SkuItemVo.SkuItemSaleAttrsVo> res = skuSaleAttrValueDao.getSaleAttrsBySpuId(4L);
        System.out.println(res);
    }
}

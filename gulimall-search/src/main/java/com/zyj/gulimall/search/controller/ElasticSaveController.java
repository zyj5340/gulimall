package com.zyj.gulimall.search.controller;

import com.zyj.common.exception.BizCodeEnume;
import com.zyj.common.to.es.SkuEsModel;
import com.zyj.common.utils.R;
import com.zyj.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * @author Stone
 * @date 2022/5/13
 */
@Slf4j
@RestController
@RequestMapping("/search")
public class ElasticSaveController {
    @Autowired
    private ProductSaveService productSaveService;

    //上架商品
    @PostMapping("/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels){

        boolean flag = true;
        try {
            flag = productSaveService.productSatusUp(skuEsModels);
        } catch (IOException e) {
            log.error("ElasticSaveController 商品上架错误：{}",e);
            return R.error(BizCodeEnume.PRODUCT_UP_EXCEPTION.getCode()
                    , BizCodeEnume.PRODUCT_UP_EXCEPTION.getMsg());
        }
        if(!flag){
            return R.ok();
        }
        return R.error(BizCodeEnume.PRODUCT_UP_EXCEPTION.getCode()
                , BizCodeEnume.PRODUCT_UP_EXCEPTION.getMsg());
    }
}

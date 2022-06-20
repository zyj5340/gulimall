package com.zyj.gulimall.product.feign;

import com.zyj.common.to.es.SkuEsModel;
import com.zyj.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author Stone
 * @date 2022/5/13
 */
@FeignClient("gulimall-search")
public interface SearchFeignService {
    @PostMapping("/search/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels);
}

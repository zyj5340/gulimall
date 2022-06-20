package com.zyj.gulimall.search.feign;

import com.zyj.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author Zyj
 * @date 2022/6/6
 */
@Service
@FeignClient("gulimall-product")
public interface ProductFeignService {

    @RequestMapping("product/attr/info/{attrId}")
    public R attrInfo(@PathVariable("attrId") Long attrId);

    @GetMapping("product/brand/infos")
    public R brandInfos(@RequestParam("brandIds") List<Long> brandIds);
}

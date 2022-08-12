package com.zyj.gulimall.product.feign;

import com.zyj.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author Zyj
 * @date 2022/7/17
 */
@FeignClient("gulimall-seckill")
public interface SeckillFeignService {

    @GetMapping("/sku/seckill/{skuId}")
    public R skuSeckillInfo(@PathVariable("skuId")Long skuId);

}

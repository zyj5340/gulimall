package com.zyj.gulimall.feign;

import com.zyj.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.websocket.server.PathParam;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author Zyj
 * @date 2022/6/19
 */
@FeignClient("gulimall-product")
@Service
public interface ProductFeignService {
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R getSkuInfo(@PathVariable("skuId") Long skuId);

    @GetMapping("/product/skusaleattrvalue/stringlist/{skuId}")
    public List<String> getSkuSaleAttr(@PathVariable("skuId")Long skuId);

    @GetMapping("/product/skuinfo/{skuId}/price")
    public String getPrice(@PathVariable("skuId") Long skuId);
}

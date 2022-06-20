package com.zyj.gulimall.product.web;

import com.zyj.gulimall.product.service.SkuInfoService;
import com.zyj.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.websocket.server.PathParam;
import java.util.concurrent.ExecutionException;

/**
 * @author Zyj
 * @date 2022/6/7
 */
@Controller
public class ItemController {

    @Autowired
    private SkuInfoService skuInfoService;

    /**`
     * 前往详情页
     * @return
     */
    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId, Model model) throws ExecutionException, InterruptedException {
        SkuItemVo vo = skuInfoService.item(skuId);
        model.addAttribute("item",vo);
//        System.out.println("查询 {"+skuId+"} 的详情");
        return "item";
    }
}

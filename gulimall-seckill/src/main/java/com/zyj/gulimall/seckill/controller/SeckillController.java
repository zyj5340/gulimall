package com.zyj.gulimall.seckill.controller;

import com.zyj.common.utils.R;
import com.zyj.gulimall.seckill.service.SeckillService;
import com.zyj.gulimall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Zyj
 * @date 2022/7/17
 */
@RestController
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @GetMapping("/kill")
    public R secKill(@RequestParam(value = "killId",required = true)String killId,
                     @RequestParam(value = "key",required = true) String key,
                     @RequestParam("num") Integer num){
        //1.判断是否登陆
        String orderSn = seckillService.kill(killId,key,num);

        return R.ok().setData(orderSn);
    }

    /**
     * 返回当前时间可以参与秒杀的商品信息
     * @return
     */
    @GetMapping("/getCurrentSeckillSkus")
    public R getCurrentSeckillSkus(){
        List<SeckillSkuRedisTo> vos = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(vos);
    }


    @GetMapping("/sku/seckill/{skuId}")
    public R skuSeckillInfo(@PathVariable("skuId")Long skuId){
        SeckillSkuRedisTo to = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().setData(to);
    }
}

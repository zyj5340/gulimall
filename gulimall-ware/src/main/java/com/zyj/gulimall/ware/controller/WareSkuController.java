package com.zyj.gulimall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.zyj.common.exception.BizCodeEnume;
import com.zyj.common.exception.NoStockException;
import com.zyj.gulimall.ware.vo.LockStockResult;
import com.zyj.gulimall.ware.vo.SkuHasStockVo;
import com.zyj.gulimall.ware.vo.WareSkuLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.zyj.gulimall.ware.entity.WareSkuEntity;
import com.zyj.gulimall.ware.service.WareSkuService;
import com.zyj.common.utils.PageUtils;
import com.zyj.common.utils.R;



/**
 * 商品库存
 *
 * @author YongjieZhang
 * @email 534054720@qq.com
 * @date 2022-04-30 17:28:38
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;


    /**
     * 订单提交锁定库存
     * @param vo
     * @return
     */
    @PostMapping("/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo vo){
        Boolean res = null;
        try {
            res = wareSkuService.orderLockStock(vo);
            return R.ok();
        } catch (NoStockException e) {
            e.printStackTrace();
            return R.error(BizCodeEnume.NO_STOCK_EXCEPTION.getCode(),BizCodeEnume.NO_STOCK_EXCEPTION.getMsg());
        }
    }


    /**
     * 判断sku是否有库存
     * @return
     */
    @PostMapping("/hasstock")
    public R getSkusHasStock(@RequestBody List<Long> skuIds){
        List<SkuHasStockVo> data = wareSkuService.getSkusHasStock(skuIds);

        return R.ok().setData(data);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}

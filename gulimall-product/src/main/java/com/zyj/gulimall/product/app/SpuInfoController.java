package com.zyj.gulimall.product.app;

import java.util.Arrays;
import java.util.Map;

import com.zyj.gulimall.product.vo.SpuSaveVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.zyj.gulimall.product.entity.SpuInfoEntity;
import com.zyj.gulimall.product.service.SpuInfoService;
import com.zyj.common.utils.PageUtils;
import com.zyj.common.utils.R;



/**
 * spu信息
 *
 * @author YongjieZhang
 * @email 534054720@qq.com
 * @date 2022-04-30 15:12:59
 */
@RestController
@RequestMapping("product/spuinfo")
public class SpuInfoController {
    @Autowired
    private SpuInfoService spuInfoService;

    ///product/spuinfo/{spuId}/up
    /**
     * 商品上架
     * @param spuId
     * @return
     */

    @PostMapping("/{spuId}/up")
    public R spuUp(@PathVariable("spuId")Long spuId){
        spuInfoService.up(spuId);
        return R.ok();
    }
    /**
     * 按条件查询
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = spuInfoService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody SpuSaveVo vo){
//		spuInfoService.save(spuInfo);
        spuInfoService.saveSpuInfo(vo);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody SpuInfoEntity spuInfo){
		spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		spuInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }


    @GetMapping("/skuId/{skuId}")
    public R getSpuInfoBySkuId(@PathVariable("skuId")Long skuId){
        SpuInfoEntity entity = spuInfoService.getSpuInfoBySkuId(skuId);
        return R.ok().setData(entity);
    }
}

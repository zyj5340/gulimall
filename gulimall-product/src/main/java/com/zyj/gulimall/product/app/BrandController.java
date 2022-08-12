package com.zyj.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.zyj.common.valid.AddGroup;
import com.zyj.common.valid.UpdateGroup;
import com.zyj.common.valid.UpdateStatusGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.zyj.gulimall.product.entity.BrandEntity;
import com.zyj.gulimall.product.service.BrandService;
import com.zyj.common.utils.PageUtils;
import com.zyj.common.utils.R;


/**
 * 品牌
 *
 * @author YongjieZhang
 * @email 534054720@qq.com
 * @date 2022-04-30 15:12:59
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;


    /**
     * 查询品牌名称
     */
    @GetMapping("/infos")
    public R infos(@RequestParam("brandIds") List<Long> brandIds){
        List<BrandEntity> brands = brandService.getBrandsByIds(brandIds);
        return R.ok().put("brand", brands);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    public R info(@PathVariable("brandId") Long brandId){
		BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@Validated({AddGroup.class}) @RequestBody BrandEntity brand /*BindingResult bindingResult*/){//BindingResult封装校验结果

//        if(bindingResult.hasErrors()){
//            Map<String,String> map = new HashMap<>();
//            //获取校验结果
//            bindingResult.getFieldErrors().forEach(item->{
//                //获取的错误提示
//                String message = item.getDefaultMessage();
//                //获取字段名
//                String field = item.getField();
//
//                map.put(field,message);
//            });
//            return R.error(400,"提交的数据不合法").put("data",map);
//        } else {
//            brandService.save(brand);
//        }
        brandService.save(brand);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@Validated({UpdateGroup.class}) @RequestBody BrandEntity brand){
		brandService.updateDetail(brand);

        return R.ok();
    }

    /**
     * 修改状态
     * @param brand
     * @return
     */
    @RequestMapping("/update/status")
    public R updateStatus(@Validated({UpdateStatusGroup.class}) @RequestBody BrandEntity brand){
        brandService.updateById(brand);

        return R.ok();
    }


    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] brandIds){
		brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}

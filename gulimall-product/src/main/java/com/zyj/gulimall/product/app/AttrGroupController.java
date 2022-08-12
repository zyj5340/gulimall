package com.zyj.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.zyj.gulimall.product.entity.AttrEntity;
import com.zyj.gulimall.product.service.AttrAttrgroupRelationService;
import com.zyj.gulimall.product.service.AttrService;
import com.zyj.gulimall.product.service.CategoryService;
import com.zyj.gulimall.product.vo.AttrGroupRelationVo;
import com.zyj.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.zyj.gulimall.product.entity.AttrGroupEntity;
import com.zyj.gulimall.product.service.AttrGroupService;
import com.zyj.common.utils.PageUtils;
import com.zyj.common.utils.R;



/**
 * 属性分组
 *
 * @author YongjieZhang
 * @email 534054720@qq.com
 * @date 2022-04-30 15:12:59
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private AttrAttrgroupRelationService attrAttrgroupRelationService;

    ///product/attrgroup/{catelogId}/withattr
    /**
     * 获取分类下所有分组&关联属性
     * @param catelogId
     * @return
     */
    @GetMapping("/{catelogId}/withattr")
    public R getAttrGroupWithAttrs(@PathVariable("catelogId")Long catelogId){
        List<AttrGroupWithAttrsVo> vos = attrGroupService.getAttrGroupWithAttrs(catelogId);
        return R.ok().put("data",vos);
    }

    ///product/attrgroup/attr/relation
    /**
     * 保存关联关系
     * @param Vos
     * @return
     */
    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVo> Vos){
        attrAttrgroupRelationService.saveBatch(Vos);
        return R.ok();
    }


    ///product/attrgroup/{attrgroupId}/attr/relation
    /**
     * 查询分组关联的所有属性
     * @param attrgroupId
     * @return
     */
    @GetMapping("/{attrgroupId}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupId")Long attrgroupId){
        List<AttrEntity> entities = attrService.getRelationAttr(attrgroupId);
        return R.ok().put("data",entities);
    }

    // /product/attrgroup/{attrgroupId}/noattr/relation
    /**
     * 查询分组没有关联的所有属性
     * @param attrgroupId
     * @return
     */
    @GetMapping("/{attrgroupId}/noattr/relation")
    public R attrNoRelation(@PathVariable("attrgroupId")Long attrgroupId,
                            @RequestParam Map<String, Object> params){
        PageUtils pageUtils = attrService.getNoRelationAttr(params,attrgroupId);
        return R.ok().put("page",pageUtils);
    }

    /**
     * 列表
     */
    @RequestMapping("/list/{catelogId}")
    public R list(@RequestParam Map<String, Object> params,
                  @PathVariable("catelogId")Long catelogId){
//        PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params,catelogId);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
        Long catelogId = attrGroup.getCatelogId();
        Long[] path = categoryService.findCatelogPath(catelogId);
        attrGroup.setCatelogPath(path);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    ///product/attrgroup/attr/relation/delete
    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVo[] vos){
        attrService.deleteRelation(vos);
        return R.ok();
    }



    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}

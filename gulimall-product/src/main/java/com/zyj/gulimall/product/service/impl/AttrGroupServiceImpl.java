package com.zyj.gulimall.product.service.impl;

import com.zyj.gulimall.product.entity.AttrEntity;
import com.zyj.gulimall.product.service.AttrService;
import com.zyj.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.zyj.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyj.common.utils.PageUtils;
import com.zyj.common.utils.Query;

import com.zyj.gulimall.product.dao.AttrGroupDao;
import com.zyj.gulimall.product.entity.AttrGroupEntity;
import com.zyj.gulimall.product.service.AttrGroupService;
import org.springframework.util.StringUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {
    @Autowired
    private AttrService attrService;
    @Autowired
    private AttrGroupDao attrGroupDao;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {

        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if(!StringUtils.isEmpty(key)){
            wrapper.and((obj)->{
                obj.like("attr_group_id",key).or().like("attr_group_name",key);
            });
        }
        if (catelogId==0){
            IPage<AttrGroupEntity> iPage = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(iPage);
        }else {
            wrapper.eq("catelog_id",catelogId);
            IPage<AttrGroupEntity> iPage = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(iPage);
        }
    }

    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrs(Long catelogId) {
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>()
                .eq("catelog_id", catelogId));
        List<AttrGroupWithAttrsVo> collect = attrGroupEntities.stream().map(attrGroupEntity -> {
            AttrGroupWithAttrsVo attrGroupWithAttrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(attrGroupEntity, attrGroupWithAttrsVo);
            List<AttrEntity> relationAttr = attrService.getRelationAttr(attrGroupWithAttrsVo.getAttrGroupId());
            attrGroupWithAttrsVo.setAttrs(relationAttr);
            return attrGroupWithAttrsVo;
        }).collect(Collectors.toList());

        return collect;
    }

    @Override
    public List<SkuItemVo.SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        //1.查询当前spu所有属性的分组信息以及当前分组下所有属性对应的值
        List<SkuItemVo.SpuItemAttrGroupVo> vos  = attrGroupDao.getAttrGroupWithAttrsBySpuId(spuId,catalogId);


        return vos;
    }

}
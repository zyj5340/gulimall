package com.zyj.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.zyj.gulimall.product.service.CategoryBrandRelationService;
import com.zyj.gulimall.product.vo.Catelog2Vo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.jni.Time;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyj.common.utils.PageUtils;
import com.zyj.common.utils.Query;

import com.zyj.gulimall.product.dao.CategoryDao;
import com.zyj.gulimall.product.entity.CategoryEntity;
import com.zyj.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
@Slf4j
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    private CategoryDao categoryDao;
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }


    @Override
    public List<CategoryEntity> listWithTree() {

        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        return categoryEntities;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 检查当前菜单是否被引用
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[0]);
    }

    /**
     * 级联更行所有关联数据
     * @return
     */
    //缓存失效模式
    @Caching(evict = {@CacheEvict(value = "category",key = "'getLevel1Categorys'"),
            @CacheEvict(value = "category",key = "'getCatalogJson'")})
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());

    }

    /**
     * 查询所有一级分类
     * @return
     */
    /**
     * value缓存名 key属性 yml配置文件设置ttl
     * @return
     */
    @Cacheable(value = "category",key = "#root.method.name")//创建缓存分区【业务类型】
    @Override
    public List<CategoryEntity> getLevel1Categorys() {

        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("cat_level", 1));
        log.info("getLevel1Categorys...");
        return categoryEntities;
    }


    //TODO 产生堆外内存溢出
    /**
     * 使用jedis 替换 lettucs
     *
     */
    @Cacheable(value = "category",key = "#root.methodName")
    @Override
    public Map<String,List<Catelog2Vo>> getCatalogJson() {
        log.info("getCatalogJson查询了数据库...");
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //查询一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(0L,selectList);
        //封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> {
            return k.getCatId().toString();
        }, v -> {
            //查询该一级分类的二级分类
            List<CategoryEntity> level2Categorys = getParent_cid(v.getCatId(),selectList);
            List<Catelog2Vo> catelog2Vos = null;
            if (level2Categorys != null) {
                catelog2Vos = level2Categorys.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(),null , l2.getCatId().toString(), l2.getName());
                    //查找当前二级分类的三级分类
                    List<CategoryEntity> level3Categorys = getParent_cid(l2.getCatId(),selectList);
                    if (level3Categorys!=null){
                        List<Catelog2Vo.Catelog3Vo> collect = level3Categorys.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(),l3.getCatId().toString(),l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));

        return parent_cid;
    }


    public Map<String,List<Catelog2Vo>> getCatalogJson2(){

        /**
         * 1.空结果缓存，缓存穿透
         * 2.设置随机过期时间 缓存雪崩
         * 3.加锁 胡村击穿
         */
        //1.加入redis缓存 ,json字符串
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)){
            //2.缓存中没有 查询数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDb();

            return catalogJsonFromDb;
        }
        //json转对象
        Map<String,List<Catelog2Vo>> result = JSON.parseObject(catalogJson,new TypeReference<Map<String,List<Catelog2Vo>>>(){});
        return result;
    }


    /**
     * 缓存一致性问题
     * 1.双写模式
     * 2.失效模式
     * @return
     */
    public Map<String,List<Catelog2Vo>> getCatalogJsonFromDb() {

        RLock lock = redissonClient.getLock("catalogJson-lock");
        lock.lock();
        Map<String, List<Catelog2Vo>> dataFromDb;
            try{
                //加锁成功 执行业务
                log.info("获取分布式锁成功...");
                 dataFromDb = getDataFromDb();
            }finally {
                lock.unlock();
                log.info("删除锁成功...");
            }
        return dataFromDb;

    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        /**
         * 1、优化 一次查询数据库
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //查询一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(0L,selectList);
        //封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> {
            return k.getCatId().toString();
        }, v -> {
            //查询该一级分类的二级分类
            List<CategoryEntity> level2Categorys = getParent_cid(v.getCatId(),selectList);
            List<Catelog2Vo> catelog2Vos = null;
            if (level2Categorys != null) {
                catelog2Vos = level2Categorys.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(),null , l2.getCatId().toString(), l2.getName());
                    //查找当前二级分类的三级分类
                    List<CategoryEntity> level3Categorys = getParent_cid(l2.getCatId(),selectList);
                    if (level3Categorys!=null){
                        List<Catelog2Vo.Catelog3Vo> collect = level3Categorys.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(),l3.getCatId().toString(),l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        //3.转json存入redis
        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJson",s,1, TimeUnit.HOURS);
        return parent_cid;
    }

    private List<CategoryEntity> getParent_cid(Long parentCid,List<CategoryEntity> selectList) {
//        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));

        List<CategoryEntity> list = selectList.stream().filter(item -> {
            return item.getParentCid().equals(parentCid);
        }).collect(Collectors.toList());
        return list;
    }

    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;
    }

}
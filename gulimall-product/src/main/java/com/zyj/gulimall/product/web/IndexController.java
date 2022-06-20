package com.zyj.gulimall.product.web;

import com.zyj.gulimall.product.entity.CategoryEntity;
import com.zyj.gulimall.product.service.CategoryService;
import com.zyj.gulimall.product.vo.Catelog2Vo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @author Stone
 * @date 2022/5/16
 */
@Slf4j
@Controller
public class IndexController {
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CategoryService categoryServicel;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){
        //TODO 查询所有一级分类
        List<CategoryEntity> categoryEntities = categoryServicel.getLevel1Categorys();
        model.addAttribute("categorys",categoryEntities);
        return "index";
    }

    //index/catalog.json
    @GetMapping("/index/catalog.json")
    @ResponseBody
    public Map<String,List<Catelog2Vo>> getCatalogJson(){
        Map<String,List<Catelog2Vo>> map = categoryServicel.getCatalogJson();
        return map;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        RLock lock = redissonClient.getLock("mylock");
        lock.lock();//阻塞式等待
        /**
         * 默认锁30s
         * 自动续期
         * 默认30s删除
         */
        try {
            log.info("IndexController hello加锁成功，执行业务...");
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
            log.info("IndexController hello解锁成功...");
        }
        return "hello";
    }
}

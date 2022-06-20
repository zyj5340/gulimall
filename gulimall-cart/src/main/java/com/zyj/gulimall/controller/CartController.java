package com.zyj.gulimall.controller;

import com.zyj.common.constant.AuthServerConstant;
import com.zyj.gulimall.interceptor.CartInterceptor;
import com.zyj.gulimall.service.CartService;
import com.zyj.gulimall.vo.CartItemVo;
import com.zyj.gulimall.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;

/**
 * @author Zyj
 * @date 2022/6/16
 */

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 前往购物车页面
     * -浏览器存放cookie userkey标示用户身份
     * -第一次访问分配一个临时用户身份userkey 浏览器保存 每次访问携带
     * @param session
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(HttpSession session){
        //1.ThreadLocal -同一线程共享数据
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();



        return "cartList";
    }

    /**
     * 添加商品到购物车
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num,
                            Model model) throws ExecutionException, InterruptedException {
        CartItemVo vo = cartService.addToCart(skuId,num);
        model.addAttribute("item",vo);
        return "success";
    }
}

package com.zyj.gulimall.controller;

import com.zyj.common.constant.AuthServerConstant;
import com.zyj.gulimall.interceptor.CartInterceptor;
import com.zyj.gulimall.service.CartService;
import com.zyj.gulimall.vo.CartItemVo;
import com.zyj.gulimall.vo.CartVo;
import com.zyj.gulimall.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
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
     * 返回当前用户购物车信息
     * @return
     */
    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItemVo> getCurrentUserCartItems(){
        return cartService.getUserCartItems();
    }


    /**
     * 删除购物车商品
     * @param skuId
     * @return
     */
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId")Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    /**
     *更改商品数量
     * @param skuId
     * @param num
     * @return
     */
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId")Long skuId,
                            @RequestParam("num")Integer num){
        cartService.changeItemCount(skuId,num);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    /**
     * 更改购物车勾选状态
     * @param skuId
     * @param check
     * @return
     */
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check){
        cartService.checkItem(skuId,check);
        return "redirect:http://cart.gulimall.com/cart.html";
    }


    /**
     * 前往购物车页面
     * -浏览器存放cookie userkey标示用户身份
     * -第一次访问分配一个临时用户身份userkey 浏览器保存 每次访问携带
     * @param
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        //1.ThreadLocal -同一线程共享数据
//        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        CartVo cart = cartService.getCart();
        model.addAttribute("cart",cart);
        return "cartList";
    }

    /**
     * 添加商品到购物车
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num,
                            RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        cartService.addToCart(skuId, num);
//        model.addAttribute("skuId",item);
        redirectAttributes.addAttribute("skuId",skuId);
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }


    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccess(@RequestParam("skuId") Long skuId,Model model){
        //重定向成功页面 再次查询购物车数据
        CartItemVo item = cartService.getCartItem(skuId);
        model.addAttribute("item",item);
        return "success";
    }
}

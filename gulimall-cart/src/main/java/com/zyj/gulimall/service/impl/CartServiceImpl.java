package com.zyj.gulimall.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.zyj.common.utils.R;
import com.zyj.gulimall.feign.ProductFeignService;
import com.zyj.gulimall.interceptor.CartInterceptor;
import com.zyj.gulimall.service.CartService;
import com.zyj.gulimall.vo.CartItemVo;
import com.zyj.gulimall.vo.CartVo;
import com.zyj.gulimall.vo.SkuInfoVo;
import com.zyj.gulimall.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author Zyj
 * @date 2022/6/16
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    public static final String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItemVo cartItem;

        Object exist = cartOps.get(skuId.toString());
        if (exist != null){
            //购物车有此商品,修改数量
            String s = (String) exist;
            cartItem = JSON.parseObject(s, new TypeReference<CartItemVo>(){});
            cartItem.setCount(cartItem.getCount()+num);
            cartOps.put(skuId.toString(),JSON.toJSONString(cartItem));

        }else {
            cartItem = new CartItemVo();
            //1.远程查询当前要添加的商品信息
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R r = productFeignService.getSkuInfo(skuId);
                SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });

                //2.添加新商品添加到购物车
                cartItem.setSkuId(skuId);
                cartItem.setCheck(true);
                cartItem.setCount(num);/*===================!!!===================*/
                cartItem.setImage(skuInfo.getSkuDefaultImg());
                cartItem.setTitle(skuInfo.getSkuTitle());
                cartItem.setPrice(skuInfo.getPrice());
            }, executor);


            //3.查询sku组合属性信息
            CompletableFuture<Void> getSkuSaleValues = CompletableFuture.runAsync(() -> {
                List<String> values = productFeignService.getSkuSaleAttr(skuId);
                cartItem.setSkuAttr(values);
            }, executor);

            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleValues).get();
            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), s);
        }
        return cartItem;
    }


    /**
     * 获取购物车重定向
     * @param skuId
     * @return
     */
    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String str = (String)cartOps.get(skuId.toString());
        CartItemVo item = JSON.parseObject(str, new TypeReference<CartItemVo>() {
        });

        return item;
    }


    /**
     * 获取购物车
     * @return
     */
    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {

        CartVo cart = new CartVo();
        //1.区分登陆状态
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null){
            //登陆状态
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            //2.判断临时购物车是否有数据
            List<CartItemVo> tempItems = getCartItems(CART_PREFIX + userInfoTo.getUserKey());
            if (tempItems != null && tempItems.size() > 0){
                //需要合并数据
                for (CartItemVo item : tempItems) {
                    Long skuId = item.getSkuId();
                    Integer num = item.getCount();
                    addToCart(skuId,num);
                }
                // 清空临时购物车
                clearCart(CART_PREFIX + userInfoTo.getUserKey());
            }

            //获取登陆后的购物车数据,包含临时购物车数据
            List<CartItemVo> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);

        }else {
            //未登陆
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            //获取临时购物车的所有购物项目
            cart.setItems(getCartItems(cartKey));
        }

        return cart;
    }


    /**
     * 获取需要操作的购物车
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        //1.
        String cartKey = "";
        if (userInfoTo.getUserId() != null){
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        }else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        return operations;
    }


    /**
     * 返回购物车数据
     * @param cartKey
     * @return
     */
    private List<CartItemVo> getCartItems(String cartKey){
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (values != null && values.size() > 0 ){
            List<CartItemVo> items = values.stream().map(obj -> {
                String str = (String) obj;
                CartItemVo cartItem = JSON.parseObject(str, new TypeReference<CartItemVo>() {
                });
                return cartItem;
            }).collect(Collectors.toList());
            return items;
        }
        return null;
    }

    @Override
    public void clearCart(String cartKey){
        redisTemplate.delete(cartKey);
    }

    /**
     * 勾选购物项
     * @param skuId
     * @param check
     */
    @Override
    public void checkItem(Long skuId, Integer check) {
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartItem);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(),s);
    }


    /**
     * 改变购物车商品数量
     *
     * @param skuId
     * @param num
     */
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),s);
    }

    /**
     * 删除购物项
     *
     * @param skuId
     */
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    /**
     * 查询用户购物项
     *
     * @return
     */
    @Override
    public List<CartItemVo> getUserCartItems() {
        UserInfoTo loginUser = CartInterceptor.threadLocal.get();
        if (loginUser == null){
            return null;
        }else {
            String cartKey = CART_PREFIX + loginUser.getUserId();
            List<CartItemVo> cartItems = getCartItems(cartKey);
            //获取所有选中的购物车项
            List<CartItemVo> collect = cartItems.stream().filter(item ->
                    item.getCheck()
            ).map(item->{
                //获取每件商品的最新价格
                BigDecimal price = new BigDecimal(productFeignService.getPrice(item.getSkuId()));
                item.setPrice(price);
                return item;
            }).collect(Collectors.toList());
            return collect;
        }
    }
}

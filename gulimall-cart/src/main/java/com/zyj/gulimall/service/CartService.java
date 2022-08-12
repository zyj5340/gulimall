package com.zyj.gulimall.service;

import com.zyj.gulimall.vo.CartItemVo;
import com.zyj.gulimall.vo.CartVo;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Zyj
 * @date 2022/6/16
 */
public interface CartService {
    /**
     * 添加商品到购物车
     * @param skuId
     * @param num
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;


    /**
     * 查询购物车某个购物项目
     * @param skuId
     * @return
     */
    CartItemVo getCartItem(Long skuId);


    /**
     * 获取购物车
     * @return
     */
    CartVo getCart() throws ExecutionException, InterruptedException;

    /**
     * 清空指定购物车
     * @param cartKey
     */
    public void clearCart(String cartKey);

    /**
     * 勾选购物项
     * @param skuId
     * @param check
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 改变购物车商品数量
     * @param skuId
     * @param num
     */
    void changeItemCount(Long skuId, Integer num);

    /**
     * 删除购物项
     * @param skuId
     */
    void deleteItem(Long skuId);

    /**
     * 查询用户购物项
     * @return
     */
    List<CartItemVo> getUserCartItems();

}

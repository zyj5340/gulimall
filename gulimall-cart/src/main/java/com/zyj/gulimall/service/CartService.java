package com.zyj.gulimall.service;

import com.zyj.gulimall.vo.CartItemVo;

import java.util.concurrent.ExecutionException;

/**
 * @author Zyj
 * @date 2022/6/16
 */
public interface CartService {
    CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

}

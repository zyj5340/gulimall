package com.zyj.gulimall.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Zyj
 * @date 2022/6/16
 * 购物车
 */
public class CartVo {
    List<CartItemVo> items;

    private Integer countNum;//商品数量

    private Integer countType;//商品类型

    private BigDecimal totalAmount;//所有商品总价

    private BigDecimal reduce = new BigDecimal("0");//减免价格

    public List<CartItemVo> getItems() {

        return items;
    }

    public void setItems(List<CartItemVo> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int count = 0;
        if (items != null && items.size() > 0) {
            for (CartItemVo item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    public Integer getCountType() {
        return items.size();
    }

    public BigDecimal getTotalAmount() {
        //1.计算购物项总价
        BigDecimal amount = new BigDecimal("0");
        if (items != null && items.size() > 0) {
            for (CartItemVo item : items) {
                if (item.getCheck() == true) {
                    amount = amount.add(item.getTotalPrice());
                }
            }
        }

        //2.减去优惠
        amount.subtract(getReduce());
        return amount;
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}

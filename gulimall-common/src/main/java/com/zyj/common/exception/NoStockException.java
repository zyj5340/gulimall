package com.zyj.common.exception;

/**
 * @author Zyj
 * @date 2022/7/13
 */
public class NoStockException extends RuntimeException{
    private Long skuId;

    /**
     * Constructs a new runtime exception with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public NoStockException() {
    }

    public NoStockException(Long skuId){
        super("商品id："+skuId+"没有足够库存了");
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
}

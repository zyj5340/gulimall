package com.zyj.gulimall.member.exception;

/**
 * @author Zyj
 * @date 2022/6/12
 */
public class PhoneExistException extends RuntimeException{

    /**
     * Constructs a new runtime exception with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public PhoneExistException() {
        super("手机号已存在");
    }
}

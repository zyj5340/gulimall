package com.zyj.gulimall.vo;

import lombok.Data;

/**
 * @author Zyj
 * @date 2022/6/14
 */
@Data
public class SocialUser {
    private String id;
    private String name;
    private String accessToken;
    private String expiresIn;
}

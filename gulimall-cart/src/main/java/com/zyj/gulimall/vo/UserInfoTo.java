package com.zyj.gulimall.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Zyj
 * @date 2022/6/16
 */
@Data
public class UserInfoTo {
    private Long userId;
    private String userKey;

    private boolean tempUser = false;
}

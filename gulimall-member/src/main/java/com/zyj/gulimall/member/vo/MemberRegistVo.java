package com.zyj.gulimall.member.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * @author Zyj
 * @date 2022/6/12
 */
@Data
public class MemberRegistVo {
    private String userName;

    private String password;

    private String phone;
}

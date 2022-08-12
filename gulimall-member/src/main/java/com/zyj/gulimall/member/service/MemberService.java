package com.zyj.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyj.common.utils.PageUtils;
import com.zyj.gulimall.member.entity.MemberEntity;
import com.zyj.gulimall.member.exception.PhoneExistException;
import com.zyj.gulimall.member.exception.UserNameExistException;
import com.zyj.gulimall.member.vo.MemberLoginVo;
import com.zyj.gulimall.member.vo.MemberRegistVo;
import com.zyj.gulimall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author YongjieZhang
 * @email 534054720@qq.com
 * @date 2022-04-30 17:15:17
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExistException;

    void checkUserNameUnique(String userName) throws UserNameExistException;


    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUser socialUser);
}


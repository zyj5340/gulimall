package com.zyj.gulimall.feign;

import com.zyj.common.utils.R;
import com.zyj.gulimall.vo.SocialUser;
import com.zyj.gulimall.vo.UserLoginVo;
import com.zyj.gulimall.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Zyj
 * @date 2022/6/12
 */
@FeignClient("gulimall-member")
@Service
public interface MemberFeignService {
    @PostMapping("/member/member/regist")
    public R regist(@RequestBody UserRegistVo vo);

    @PostMapping("/member/member/login")
    public R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/oauth2/login")
    public R oauthLogin(@RequestBody SocialUser socialUser);
}

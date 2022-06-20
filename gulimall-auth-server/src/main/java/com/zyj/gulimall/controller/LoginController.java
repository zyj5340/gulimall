package com.zyj.gulimall.controller;

import com.alibaba.fastjson.TypeReference;
import com.zyj.common.constant.AuthServerConstant;
import com.zyj.common.exception.BizCodeEnume;
import com.zyj.common.utils.R;
import com.zyj.common.vo.MemberRespVo;
import com.zyj.gulimall.feign.MemberFeignService;
import com.zyj.gulimall.feign.ThirdPartyFeignService;
import com.zyj.gulimall.vo.UserLoginVo;
import com.zyj.gulimall.vo.UserRegistVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Zyj
 * @date 2022/6/10
 */
@Controller
public class LoginController {
    @Autowired
    private ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object loginUser = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (loginUser == null){
            //未登陆
            return "login";
        }else {
            return "redirect:http://gulimall.com";
        }
    }

    /**
     * 发送验证码
     * @param phone
     * @return
     */
    @GetMapping("/sms/sendcode")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone){
        //TODO  1.接口防刷

        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (redisCode != null) {
            long l = Long.parseLong(redisCode.split("_")[1]);
            //再次调用验证码时间距离上次发送小于60不能发送
            if (System.currentTimeMillis() - l < 60000) {
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        //2.验证码再次校验 redis  key-sms:code:phone - v
        String code = UUID.randomUUID().toString().substring(0, 4)+"_"+System.currentTimeMillis();
        //防止同一个手机号60s内再次发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,code,10, TimeUnit.MINUTES);
        thirdPartyFeignService.sendCode(phone,code.substring(0,4));
        return R.ok();
    }


    /**
     * //TODO 重定向携带数据使用session原理
     * //TODO 分布式session问题
     * 用户注册
     * @param vo
     * @param result 校验的结果
     * @param redirectAttributes 重定向携带数据
     * @return
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes){
        //前置校验
        if (result.hasErrors()){
            /**
            result.getFieldErrors().stream().map(fieldError -> {
                String field = fieldError.getField();
                String msg = fieldError.getDefaultMessage();
                errors.put(field,msg);
            });
             */
            Map<String,String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField,FieldError::getDefaultMessage));
            //校验出错
            redirectAttributes.addFlashAttribute("errors",errors);

            return "redirect:http://auth.gulimall.com/reg.html";//Request method 'POST' not supported 转发的页面不支持POST forward:/reg.html
        }

        //注册
        //1.验证码校验
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (!StringUtils.isEmpty(s)){
            if (code.equals(s.substring(0,4))){
                //删除验证码,令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //验证码通过调用远程服务注册
                R r = memberFeignService.regist(vo);
                if (r.getCode() == 0){
                    //注册成功，回到首页/登陆页
                    return "redirect:http://auth.gulimall.com/login.html";
                }else {
                    Map<String,String> errors = new HashMap<>();
                    errors.put("msg",(String)r.get("msg"));
                    redirectAttributes.addFlashAttribute("errors",errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }

            }else {
                Map<String,String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                //校验出错
                redirectAttributes.addFlashAttribute("errors",errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }else {
            Map<String,String> errors = new HashMap<>();
            errors.put("code","验证码失效");
            //校验出错
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }

    }

    /**
     * 账号密码登陆
     * @param vo
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/login")
    public String login( UserLoginVo vo ,RedirectAttributes redirectAttributes,HttpSession session){

        //调用远程登陆服务
        R r = memberFeignService.login(vo);
        if (r.getCode() == 0){
            //成功
            MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER,data);
            return "redirect:http://gulimall.com";
        }else {
            //失败
            Map<String,String> errors = new HashMap<>();
            errors.put("msg",(String)r.get("msg"));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }

}

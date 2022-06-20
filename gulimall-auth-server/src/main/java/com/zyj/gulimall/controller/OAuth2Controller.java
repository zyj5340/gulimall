package com.zyj.gulimall.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.zyj.common.utils.R;
import com.zyj.gulimall.feign.MemberFeignService;
import com.zyj.common.vo.MemberRespVo;
import com.zyj.gulimall.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.UUID;

/**
 * 处理社交登陆callback请求
 * @author Zyj
 * @date 2022/6/14
 */
@Slf4j
@Controller
public class OAuth2Controller {

    @Autowired
    private MemberFeignService memberFeignService;

    //=======================================>Gitee Oauth2
    @Value("${gitee.oauth.clientid}")
    private String clientId;
    @Value("${gitee.oauth.clientsecret}")
    private String clientSecret;
    @Value("${gitee.oauth.callback}")
    private String callBack;

    /**
     * 请求授权页面
     * @param session
     * @return
     */
    @GetMapping("/auth/gitee")
    public String giteeOauth(HttpSession session){
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        session.setAttribute("state", uuid);
        // Step1：获取Authorization Code
        String url = "https://gitee.com/oauth/authorize?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(callBack) +
                "&state=" + uuid +
                "&scope=user_info";

        return "redirect:"+url;
    }
    //=======================================>Gitee Oauth2

    @GetMapping("/oauth2.0/gitee/success")
    public String gitee(@RequestParam("code")String code,HttpSession session) throws IOException {
        //1.根据code换取access token
        String url = "https://gitee.com/oauth/token?grant_type=authorization_code" +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&code=" + code +
                "&redirect_uri=" + callBack;
        JSONObject accessTokenJson = getAccessToken(url);

        // Step3: 获取用户信息
        url = "https://gitee.com/api/v5/user?access_token=" + accessTokenJson.get("access_token");
        JSONObject jsonObject = getUserInfo(url);

        //判断用户是否已经注册
        String accessToken = accessTokenJson.getString("access_token");//access_token 过期后（有效期为一天）
        String uid = jsonObject.getString("id");
        String name = jsonObject.getString("name");

        //TODO token时间过期时间为一天 ==============待修改=====================
        Date date = new Date(new Date().getTime()+60*60*24);


        SocialUser socialUser = new SocialUser();
        socialUser.setAccessToken(accessToken);
        socialUser.setExpiresIn(date.toString());
        socialUser.setName(name);
        socialUser.setId(uid);
        //调用远程服务登陆or注册
        R r = memberFeignService.oauthLogin(socialUser);
        if (r.getCode() == 0){
            MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
            });
            log.info(("登陆成功，用户信息："+data));
            //TODO 1.默认令牌 SESSION 只能作用在当前域（解决子域session共享问题）
            //TODO 2.使用JSON序列化方式存储到redis


            session.setAttribute("loginUser",data);
            //2.登陆成功回到首页
            return "redirect:http://gulimall.com";
        }else {
            return "redirect:http://auth.gulimall.com/login.hmtl";
        }

    }


    /**
     * 获取Access Token
     * post
     */
    public static JSONObject getAccessToken(String url) throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        HttpResponse response = client.execute(httpPost);
        HttpEntity entity = response.getEntity();//获取请求体
        if (null != entity) {
            String result = EntityUtils.toString(entity, "UTF-8");
            return JSONObject.parseObject(result);
        }
        httpPost.releaseConnection();
        return null;
    }

    /**
     * 获取用户信息
     * get
     */
    public static JSONObject getUserInfo(String url) throws IOException {
        JSONObject jsonObject = null;
        CloseableHttpClient client = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        HttpResponse response = client.execute(httpGet);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            String result = EntityUtils.toString(entity, "UTF-8");
            jsonObject = JSONObject.parseObject(result);
        }

        httpGet.releaseConnection();

        return jsonObject;
    }

}

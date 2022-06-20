package com.zyj.gulimall.thirdparty;

import com.zyj.gulimall.thirdparty.component.SmsComponent;
import com.zyj.gulimall.thirdparty.util.HttpUtils;
import org.apache.http.HttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Zyj
 * @date 2022/6/11
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class SendCodeTest {
    @Autowired
    private SmsComponent smsComponent;

    @Test
    public void sendSmsCode(){
        smsComponent.sendSmsCode("15106237722","5678");
    }

    @Test
    public void sendCode(){
        String host = "https://dfsns.market.alicloudapi.com";
        String path = "/data/send_sms";
        String method = "POST";
        String appcode = "a67f90dafde34a1081b02b36e4a1a326";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        //根据API的要求，定义相对应的Content-Type
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> querys = new HashMap<String, String>();
        Map<String, String> bodys = new HashMap<String, String>();
        bodys.put("content", "code:1234");//验证码
        bodys.put("phone_number", "15106237722");//手机号
        bodys.put("template_id", "TPL_0000");//内容模板样式


        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

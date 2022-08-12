package com.zyj.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.zyj.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    // 应用ID,您的APPID，收款账号既是您的APPID对应支付宝账号
    public static String app_id = "2021000121626475";

    // 商户私钥，您的PKCS8格式RSA2私钥
    public static String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDRZHXlpLgF0h/WoowSGeGo4+lR5bbQTWzcIVVww5TJLHwjRtWnSrl/G3ehe2zZSU+Ey30hcy++BuexNY4JCEjecPOOBgVE2gQ16E+aKHPDnuWrwv1tkS+Qs84zDWNu1SCau862bOTXKAuY6IeJ8quN838CzXJGAUObOCxHQuMb52CSTeeaBEFwQ001Q8O/z7IPnKhUFjJcrGI1fptQ4zhC8kDgT08pQ9WGnhKnMz7oNsfY4CWGc0ahQ1V1NwFk9w54uXmbkNN5tAWwwlHGKwvw9l0Xuk+y9dD6SUyVfFzfRaNw7PlUmwzStNaiu/jJvLdV3ygD3vr9A5sWNzMyiTJLAgMBAAECggEACcvDYDz2hv8vkiEEoIwpbHdNIRG4HcKhhyLgFmhv+4FjRVs4/5yVVQb578oQa5HscG/8qKX049T63eV9gZqrngx2uHw7nt78N3Fo1/NwhwRWBlUW+htCuGhRCz5jGnpKDjMfFpW9lvs+n6axQJjjwb5UgNMPt4qzmP1lxOeyLEnvGCPuPK5pkczQlPio524Rvb5S9StXgsw0r3v+0uRyKWTCBIqFlfqj//8lw7u43JLeme/54V31pSUlV0AkDR+TmZ9uSx27fmUMUW3HChU4192zZhg1IwQzw4dcQXJIBDMC0OueC1CJqg+AF49g20Z4QQmTG3Xj4UvJ855Y0hrrMQKBgQD5CJILiJAG9OdKWGbzTZQhTcJxbgDwl/SB4rNbZ6W0Fjnmngm9Gv3K46rOi/0d8+niIGHM4CQgbZGO8LpXJWOHIYFC5Pu8M715k0YEHlh9mi7cm2qNO6FAluXs1UhZahl9e3rKftu3FAHb2UDmk/LnTEpyLxiweOv2K3rvj7/DpQKBgQDXQAEhUbEo7g0onvi2Y0n+/PZgsHQNUi9RvMcMkfbKUzf792n24Kpn7qtP46VL2MTd+LzRGzevi3d0Uu7ySJ/+I7Mgn96ETxo5afXVxDB+4oHCocA1wYfEAvCAPz1o8v/PdlQLMAq75QKfWnmIja7MklzBbyjfNSHkwfbXjcR7LwKBgBhiR3KLp5aWykLUTxhJo1RzebODkuH77vv8x5UOnAH7HyY+mTOD/g+spR/eQyV4qZDznL/jvoXSVKtLVOONjKZBmJeFNkTiSA4sZnGmywFZZHdDXHaBvTNG0zKSW6gtpFtOCOLk8vyFH/1300wAdotBRBTUGbXF0UzkSwBD8gHlAoGBAIQGyCqttuKKERwnVhpZEFwMwPpRgP1iuY+DIPw+04cKQD6WZTJ+X0dch0t7MDozfZ1BIl+IOEG+Y8i/zbxie3AgaZDLPmsdMdKhgZ5atfw95qWBHSpbyHCb9PRU1c/3rJyN0cMC9rKFJ3SixATUErF73NTw37r5Fg8AQe25ERkVAoGACTl64u9Yly/OaBSo8SfaAqiHlFX+R0nm2bUpeUAjVC6elYW1RLMB5kWEOfvU4WCX75YUKAJMpKl37kGKMJEbYMRpAX7P8HLHKifwyaeHgoOY/0o2KOxckrsPIUH+qa1qjOOcqfjfiOoT5WkSDh46z0r5U8USFpSDrgF6P6eshIQ=";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public static String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5uCxJYyk5nz2oJ3Jv4d9lS4azodz5EIe41OicPDmwtTAtGAZHfTttJe6/OijFiUWsVbuhXeokxPup3ww3nOMatFVtm9wEiti6AxFHJmRbz+uHxqj1oXVZMTEfh5wnG9/fKqlzoFGf7XpvS8EAp7ad8tJ6VM/vlrSfOoIZ1kF+CmZMGO2ldCC5xsYs1PIGxOwsuprqCXtBxUcwEydRt+9x7xig5DK8mBCCqR9GQnIXN/XL4YRDdkzJCEEyLAN20lnOOARDRBHD02SKq7vMI9zhohcoJSZ22a/pRZGc5AXWPMEC7PcqFohGTredvMkczD6laXmggdPjW4XM19O4CkHnwIDAQAB";

    // 服务器异步通知页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    public static String notify_url = "https://55933z8o60.zicp.fun/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    public static String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    public static String sign_type = "RSA2";

    // 字符编码格式
    public static String charset = "utf-8";

    // 支付宝网关
    public static String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    private String timeout = "30m";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+ timeout +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}

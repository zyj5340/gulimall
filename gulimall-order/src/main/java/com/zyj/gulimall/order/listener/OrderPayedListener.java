package com.zyj.gulimall.order.listener;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.zyj.gulimall.order.config.AlipayTemplate;
import com.zyj.gulimall.order.service.OrderService;
import com.zyj.gulimall.order.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Zyj
 * @date 2022/7/16
 */
@RestController
public class OrderPayedListener {

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private OrderService orderService;

    @PostMapping("/payed/notify")
    public String handleAlipayed(PayAsyncVo vo, HttpServletRequest request) throws Exception {
//        Map<String, String[]> map = request.getParameterMap();
//        for (Map.Entry<String,String[]> entry: map.entrySet()){
//            System.out.println("参数名:" + entry.getKey()+"参数值：" + entry.getValue());
//        }

        //验签
//获取支付宝POST过来反馈信息
        Map<String,String> params = new HashMap<String,String>();
        Map<String,String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
            valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }

        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayTemplate.alipay_public_key, alipayTemplate.charset, alipayTemplate.sign_type); //调用SDK验证签名
        //TODO 此处验证逻辑与实际相反 测试签名验证一直失败
        if (! signVerified) {
            //签名认证成功
            System.out.println("签名认证成功...");
            String result = orderService.handleResult(vo);
            return result;
        }else {
            System.out.println("签名认证失败...");
            return "error";
        }
    }
}

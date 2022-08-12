package com.zyj.gulimall.order.web;

import com.alipay.api.AlipayApiException;
import com.zyj.gulimall.order.config.AlipayTemplate;
import com.zyj.gulimall.order.service.OrderService;
import com.zyj.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.websocket.server.PathParam;

/**
 * @author Zyj
 * @date 2022/7/15
 */
@Controller
public class PayWebController {

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private OrderService orderService;


    /**
     * 支付宝支付
     * @param orderSn
     * @return
     * @throws AlipayApiException
     */
    @GetMapping(value = "/payOrder",produces = "text/html")
    @ResponseBody
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
//        PayVo payVo = new PayVo();
//        payVo.setBody();//订单备注
//        payVo.setOut_trade_no();//订单号
//        payVo.setSubject();//订单的标题
//        payVo.setTotal_amount();//订单金额
        PayVo payVo = orderService.getOrderPay(orderSn);
        String pay = alipayTemplate.pay(payVo);
        //支付宝返回的页面直接交给浏览器
        System.out.println(pay);
        return pay;
    }
}

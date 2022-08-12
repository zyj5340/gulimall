package com.zyj.gulimall.order.web;

import com.zyj.common.exception.NoStockException;
import com.zyj.gulimall.order.service.OrderService;
import com.zyj.gulimall.order.vo.OrderConfirmVo;
import com.zyj.gulimall.order.vo.OrderSubmitVo;
import com.zyj.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

/**
 * @author Zyj
 * @date 2022/7/3
 */
@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData",orderConfirmVo);
        //展示订单确认数据
        return "confirm";
    }


    /**
     * 提交订单
     * @param vo
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){

        try {
            SubmitOrderResponseVo resp = orderService.submitOrder(vo);
            if (resp.getCode() == 0){
                model.addAttribute("submitOrderResp",resp);
                return  "pay";
            }else {
                String msg = "下单失败:";
                switch (resp.getCode()){
                    case 1: msg += "令牌校验失败(订单信息过期，重新提交)"; break;
                    case 2: msg += "订单价格发生变化，确认后再次提交"; break;
                    case 3: msg += "库存锁定失败，商品库存不足"; break;
                }

                redirectAttributes.addAttribute("msg",msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
        } catch (Exception e) {
            if (e instanceof NoStockException){
                String message = ((NoStockException) e).getMessage();
                redirectAttributes.addAttribute("msg",message);
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }
}

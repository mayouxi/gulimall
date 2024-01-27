package com.sjy.gulimall.order.web;

import com.sjy.common.exception.NoStockException;
import com.sjy.gulimall.order.constant.OrderConstant;
import com.sjy.gulimall.order.intercept.LoginUserInterceptor;
import com.sjy.gulimall.order.service.OrderService;
import com.sjy.gulimall.order.vo.OrderConfirmVo;
import com.sjy.gulimall.order.vo.OrderSubmitVo;
import com.sjy.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * @Description:
 * @Created: with IntelliJ IDEA.
 * @author: 夏沫止水
 * @createTime: 2020-07-02 18:35
 **/

@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    /**
     * 去结算确认页
     * @param model
     * @param request
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping(value = "/toTrade")
    public String toTrade(Model model, HttpServletRequest request) throws ExecutionException, InterruptedException {

        OrderConfirmVo confirmVo = orderService.confirmOrder();

        model.addAttribute("orderConfirmData",confirmVo);
        //展示订单确认的数据

        return "confirm";
    }


    /**
     * 下单功能
     * @param vo
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){
        // 1、创建订单、验令牌、验价格、验库存
        try {
            SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
            if (responseVo.getCode() == 0) {
                // 下单成功来到支付选择页
                model.addAttribute("submitOrderResp",responseVo);
                return "pay";
            } else {
                // 下单失败回到订单确认页重新确认订单信息
                String msg = "下单失败: ";
                switch ( responseVo.getCode()){
                    case 1: msg+="订单信息过期，请刷新再次提交";break;
                    case 2: msg+="订单商品价格发生变化，请确认后再次提交";break;
                    case 3: msg+="商品库存不足";break;
                }
                redirectAttributes.addAttribute("msg",msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
        } catch (Exception e){
            if (e instanceof NoStockException) {
                String message = e.getMessage();
                redirectAttributes.addFlashAttribute("msg", message);
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }

    }

}

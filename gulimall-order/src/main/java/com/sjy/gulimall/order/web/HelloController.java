package com.sjy.gulimall.order.web;

import com.sjy.gulimall.order.entity.OrderEntity;
import com.sjy.gulimall.order.feign.CartFeignService;
import com.sjy.gulimall.order.vo.OrderItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.List;
import java.util.UUID;


@Controller
public class HelloController {

    @GetMapping(value = "/{page}.html")
    public String listPage(@PathVariable("page") String page) {

        return page;
    }

    @Autowired
    CartFeignService cartFeignService;

    @GetMapping("/hello")
    public void hello() {
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        System.out.println(currentUserCartItems);
    }

}

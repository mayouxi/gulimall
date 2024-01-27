package com.sjy.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.to.mq.SeckillOrderTo;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.order.entity.OrderEntity;
import com.sjy.gulimall.order.vo.OrderConfirmVo;
import com.sjy.gulimall.order.vo.OrderSubmitVo;
import com.sjy.gulimall.order.vo.SubmitOrderResponseVo;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * ????
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 15:14:47
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    OrderEntity getOrderByOrderSn(String orderSn);

    void closeOrder(OrderEntity entity);

    void createSeckillOrder(SeckillOrderTo orderTo);
}


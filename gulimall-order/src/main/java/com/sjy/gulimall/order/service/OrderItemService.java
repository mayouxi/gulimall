package com.sjy.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.order.entity.OrderItemEntity;

import java.util.Map;

/**
 * ????????Ϣ
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 15:14:46
 */
public interface OrderItemService extends IService<OrderItemEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


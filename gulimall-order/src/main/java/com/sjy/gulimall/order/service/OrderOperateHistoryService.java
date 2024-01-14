package com.sjy.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.order.entity.OrderOperateHistoryEntity;

import java.util.Map;

/**
 * ??????????ʷ??¼
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 15:14:45
 */
public interface OrderOperateHistoryService extends IService<OrderOperateHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


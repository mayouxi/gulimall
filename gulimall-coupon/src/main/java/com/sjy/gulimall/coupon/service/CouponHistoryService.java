package com.sjy.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.coupon.entity.CouponHistoryEntity;

import java.util.Map;

/**
 * ?Ż?ȯ??ȡ??ʷ??¼
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 14:59:44
 */
public interface CouponHistoryService extends IService<CouponHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


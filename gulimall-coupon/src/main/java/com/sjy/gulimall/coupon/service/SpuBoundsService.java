package com.sjy.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.coupon.entity.SpuBoundsEntity;

import java.util.Map;

/**
 * ??Ʒspu???????
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 14:59:40
 */
public interface SpuBoundsService extends IService<SpuBoundsEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


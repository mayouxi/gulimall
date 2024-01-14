package com.sjy.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.coupon.entity.HomeSubjectSpuEntity;

import java.util.Map;

/**
 * ר????Ʒ
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 14:59:49
 */
public interface HomeSubjectSpuService extends IService<HomeSubjectSpuEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


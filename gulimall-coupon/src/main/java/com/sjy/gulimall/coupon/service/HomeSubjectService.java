package com.sjy.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.coupon.entity.HomeSubjectEntity;

import java.util.Map;

/**
 * ??ҳר???�jd??ҳ?????ܶ?ר?⣬ÿ??ר???????µ?ҳ?棬չʾר????Ʒ??Ϣ??
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 14:59:50
 */
public interface HomeSubjectService extends IService<HomeSubjectEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


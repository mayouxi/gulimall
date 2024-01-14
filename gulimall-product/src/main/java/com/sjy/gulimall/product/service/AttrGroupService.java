package com.sjy.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.product.entity.AttrGroupEntity;

import java.util.Map;

/**
 * ???Է??
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 13:43:12
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


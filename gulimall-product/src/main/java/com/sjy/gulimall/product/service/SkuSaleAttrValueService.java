package com.sjy.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.product.entity.SkuSaleAttrValueEntity;

import java.util.Map;

/**
 * sku????????&ֵ
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 13:43:17
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


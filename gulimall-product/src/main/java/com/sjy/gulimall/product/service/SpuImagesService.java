package com.sjy.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.product.entity.SpuImagesEntity;

import java.util.Map;

/**
 * spuͼƬ
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 13:43:13
 */
public interface SpuImagesService extends IService<SpuImagesEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


package com.sjy.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.product.entity.SpuCommentEntity;

import java.util.Map;

/**
 * ??Ʒ???
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 13:43:15
 */
public interface SpuCommentService extends IService<SpuCommentEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


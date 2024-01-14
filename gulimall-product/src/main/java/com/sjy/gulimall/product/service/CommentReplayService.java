package com.sjy.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.product.entity.CommentReplayEntity;

import java.util.Map;

/**
 * ??Ʒ???ۻظ???ϵ
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 13:43:04
 */
public interface CommentReplayService extends IService<CommentReplayEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


package com.sjy.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.member.entity.IntegrationChangeHistoryEntity;

import java.util.Map;

/**
 * ???ֱ仯??ʷ??¼
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 15:07:53
 */
public interface IntegrationChangeHistoryService extends IService<IntegrationChangeHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


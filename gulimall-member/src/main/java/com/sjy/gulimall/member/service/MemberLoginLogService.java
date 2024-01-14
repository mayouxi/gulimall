package com.sjy.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.member.entity.MemberLoginLogEntity;

import java.util.Map;

/**
 * ??Ա??¼??¼
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 15:07:57
 */
public interface MemberLoginLogService extends IService<MemberLoginLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


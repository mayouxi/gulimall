package com.sjy.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.member.entity.MemberLevelEntity;

import java.util.Map;

/**
 * ??Ա?ȼ?
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 15:07:58
 */
public interface MemberLevelService extends IService<MemberLevelEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


package com.sjy.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.member.entity.MemberCollectSpuEntity;

import java.util.Map;

/**
 * ??Ա?ղص???Ʒ
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 15:07:51
 */
public interface MemberCollectSpuService extends IService<MemberCollectSpuEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


package com.sjy.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjy.common.utils.PageUtils;
import com.sjy.gulimall.ware.entity.UndoLogEntity;

import java.util.Map;

/**
 * 
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 15:18:33
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


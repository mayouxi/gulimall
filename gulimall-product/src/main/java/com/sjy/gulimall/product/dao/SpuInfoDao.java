package com.sjy.gulimall.product.dao;

import com.sjy.gulimall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * spu??Ϣ
 * 
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 13:43:11
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    void upSpuStatus(Long spuId, int code);
}

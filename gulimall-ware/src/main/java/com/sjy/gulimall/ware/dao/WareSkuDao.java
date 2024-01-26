package com.sjy.gulimall.ware.dao;

import com.sjy.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品库存
 * 
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 15:18:34
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    Long getStockBySkuId(Long skuId);

    List<Long> listWareIdHashSkuStock(Long skuId);

    boolean lockStock(Long wareId, Long skuId, Integer num);

    void unlockStock(Long skuId, Long wareId, Integer num);
}

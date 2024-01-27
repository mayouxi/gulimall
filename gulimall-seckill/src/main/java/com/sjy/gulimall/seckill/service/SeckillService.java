package com.sjy.gulimall.seckill.service;

import com.sjy.gulimall.seckill.to.SeckillSkuRedisTo;

import java.util.List;

/**
 * @author zr
 * @date 2022/1/6 18:14
 */
public interface SeckillService {
    /**
     * 上架三天需要秒杀的商品
     */
    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    SeckillSkuRedisTo getSkuSeckilInfo(Long skuId);

    String kill(String killId, String key, Integer num) throws InterruptedException;
}


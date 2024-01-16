package com.sjy.gulimall.ware.service.impl;

import com.mysql.cj.util.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjy.common.utils.PageUtils;
import com.sjy.common.utils.Query;

import com.sjy.gulimall.ware.dao.PurchaseDetailDao;
import com.sjy.gulimall.ware.entity.PurchaseDetailEntity;
import com.sjy.gulimall.ware.service.PurchaseDetailService;


@Service("purchaseDetailService")
public class PurchaseDetailServiceImpl extends ServiceImpl<PurchaseDetailDao, PurchaseDetailEntity> implements PurchaseDetailService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<PurchaseDetailEntity> wrapper = new QueryWrapper<>();

        String key  = (String) params.get("key");
        if (!StringUtils.isNullOrEmpty(key)){
            wrapper.and(w -> {
                w.eq("sku_id", key).or().eq("purchase_id", key);
            });
        }

        String status  = (String) params.get("status");
        if (!StringUtils.isNullOrEmpty(status)){
            wrapper.eq("status", status);
        }

        String wareId  = (String) params.get("wareId");
        if (!StringUtils.isNullOrEmpty(wareId)){
            wrapper.eq("ware_id", wareId);
        }

        IPage<PurchaseDetailEntity> page = this.page(
                new Query<PurchaseDetailEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }


}
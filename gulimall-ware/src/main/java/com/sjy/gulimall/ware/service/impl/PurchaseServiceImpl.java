package com.sjy.gulimall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sjy.common.constant.WareConstant;
import com.sjy.gulimall.ware.entity.PurchaseDetailEntity;
import com.sjy.gulimall.ware.service.PurchaseDetailService;
import com.sjy.gulimall.ware.vo.MergeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjy.common.utils.PageUtils;
import com.sjy.common.utils.Query;

import com.sjy.gulimall.ware.dao.PurchaseDao;
import com.sjy.gulimall.ware.entity.PurchaseEntity;
import com.sjy.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", WareConstant.PurchaseStatusEnum.CREATED.getCode()).or().eq("status", WareConstant.PurchaseStatusEnum.ASSIGNED.getCode());

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Autowired
    PurchaseDetailService detailService;

    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        if (purchaseId == null) {
            //新建采购单
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        //合并采购需求
        List<Long> items = mergeVo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> list = detailService.getBaseMapper().selectBatchIds(items).stream().filter(entity -> {
            //如果还没去采购，或者采购失败，就可以修改
            return entity.getStatus() < WareConstant.PurchaseDetailStatusEnum.BUYING.getCode()
                    || entity.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode();
        }).map(entity -> {
            //修改状态，以及采购单id
            entity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            entity.setPurchaseId(finalPurchaseId);
            return entity;
        }).collect(Collectors.toList());
        detailService.updateBatchById(list);
    }

    @Transactional
    @Override
    public void received(List<Long> ids) {
        // 没有采购需求直接返回，否则会破坏采购单
        if (ids == null || ids.size() == 0) {
            return;
        }

        List<PurchaseEntity> list = this.getBaseMapper().selectBatchIds(ids).stream().filter(entity -> {
            //确保采购单的状态是新建或者已分配
            return entity.getStatus() <= WareConstant.PurchaseStatusEnum.ASSIGNED.getCode();
        }).map(entity -> {
            //修改采购单的状态为已领取
            entity.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            return entity;
        }).collect(Collectors.toList());
        this.updateBatchById(list);

        //修改该采购单下的所有采购需求的状态为正在采购
        UpdateWrapper<PurchaseDetailEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("purchase_id", ids);
        PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
        purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
        detailService.update(purchaseDetailEntity, updateWrapper);
    }



}
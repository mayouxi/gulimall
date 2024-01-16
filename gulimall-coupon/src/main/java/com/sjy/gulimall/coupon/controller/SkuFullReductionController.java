package com.sjy.gulimall.coupon.controller;

import java.util.Arrays;
import java.util.Map;

import com.sjy.common.to.SkuReductionTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.sjy.gulimall.coupon.entity.SkuFullReductionEntity;
import com.sjy.gulimall.coupon.service.SkuFullReductionService;
import com.sjy.common.utils.PageUtils;
import com.sjy.common.utils.R;



/**
 * ??Ʒ??????Ϣ
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 14:59:37
 */
@RestController
@RequestMapping("coupon/skufullreduction")
public class SkuFullReductionController {
    @Autowired
    private SkuFullReductionService skuFullReductionService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = skuFullReductionService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		SkuFullReductionEntity skuFullReduction = skuFullReductionService.getById(id);

        return R.ok().put("skuFullReduction", skuFullReduction);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody SkuFullReductionEntity skuFullReduction){
		skuFullReductionService.save(skuFullReduction);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody SkuFullReductionEntity skuFullReduction){
		skuFullReductionService.updateById(skuFullReduction);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		skuFullReductionService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    @PostMapping("/saveInfo")
    public R saveInfo(@RequestBody SkuReductionTo skuReductionTo){
        skuFullReductionService.saveSkuReduction(skuReductionTo);
        return R.ok();
    }

}

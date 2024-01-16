package com.sjy.gulimall.product.service.impl;

import com.sjy.gulimall.product.entity.AttrEntity;
import com.sjy.gulimall.product.service.AttrService;
import com.sjy.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjy.common.utils.PageUtils;
import com.sjy.common.utils.Query;

import com.sjy.gulimall.product.dao.AttrGroupDao;
import com.sjy.gulimall.product.entity.AttrGroupEntity;
import com.sjy.gulimall.product.service.AttrGroupService;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), new QueryWrapper<AttrGroupEntity>());

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((item) -> {
                item.like("attr_group_name", key).or().eq("attr_group_id", key);
            });
        }
        if (catelogId != 0) {
            wrapper.eq("catelog_id", catelogId);
        }
        wrapper.eq("catelog_id", catelogId);
        IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Autowired
    AttrService attrService;
    /*
     *根据分类id查出所有的分组以及这些分组里面的属性
     *@param:[catelogId]
     *@return:java.util.List<com.xmh.gulimall.product.vo.AttrGroupWithAttrsVo>
     *@date: 2021/8/16 21:13
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {
        List<AttrGroupEntity> attrGroupEntities = this.baseMapper.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<AttrGroupWithAttrsVo> collect = attrGroupEntities.stream().map(item -> {
            AttrGroupWithAttrsVo attrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(item, attrsVo);
            List<AttrEntity> relationAttr = attrService.getRelationAttr(item.getAttrGroupId());
            attrsVo.setAttrs(relationAttr);
            return attrsVo;
        }).collect(Collectors.toList());
        return collect;
    }


}
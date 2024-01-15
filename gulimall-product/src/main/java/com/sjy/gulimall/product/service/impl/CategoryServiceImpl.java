package com.sjy.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjy.common.utils.PageUtils;
import com.sjy.common.utils.Query;

import com.sjy.gulimall.product.dao.CategoryDao;
import com.sjy.gulimall.product.entity.CategoryEntity;
import com.sjy.gulimall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        List<CategoryEntity> all = baseMapper.selectList(null);
        List<CategoryEntity> level1Menus = all.stream().filter(entity -> {
            return entity.getParentCid() == 0;
        }).map(entity -> {
            entity.setChildren(getChildren(entity, all));
            return entity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> catIds) {
        //TODO 1、检查当前删除的菜单，是否被别的地方引用
        baseMapper.deleteBatchIds(catIds);
    }

    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(entity -> {
            return entity.getParentCid().equals(root.getCatId());
        }).map(child -> {
            child.setChildren(getChildren(child, all));
            return child;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

}
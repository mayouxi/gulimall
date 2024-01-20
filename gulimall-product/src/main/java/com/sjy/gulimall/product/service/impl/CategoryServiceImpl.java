package com.sjy.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.sjy.gulimall.product.service.CategoryBrandRelationService;
import com.sjy.gulimall.product.vo.Catelog2Vo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjy.common.utils.PageUtils;
import com.sjy.common.utils.Query;

import com.sjy.gulimall.product.dao.CategoryDao;
import com.sjy.gulimall.product.entity.CategoryEntity;
import com.sjy.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


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

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        //1. 加入缓存逻辑
        String catelogJSON = redisTemplate.opsForValue().get("catelogJSON");
        if (StringUtils.isEmpty(catelogJSON)){
            //2.缓存中没有,查询数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDb();
            //3.查到的数据再放入缓存,将对象转为json放在缓存中
            catelogJSON = JSON.toJSONString(catalogJsonFromDb);
            redisTemplate.opsForValue().set("catelogJSON",catelogJSON);
        }
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
        return result;
    }

    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDb() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        List<CategoryEntity> level1Categories = getListByParentId(categoryEntities, 0L);
        Map<String, List<Catelog2Vo>> collect = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), level1 -> {
            List<CategoryEntity> category2level = getListByParentId(categoryEntities, level1.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (category2level != null) {
                catelog2Vos = category2level.stream().map(level2 -> {
                    List<CategoryEntity> category3level = getListByParentId(categoryEntities, level2.getCatId());
                    List<Catelog2Vo.Catelog3Vo> catelog3Vos = null;
                    if (category3level != null) {
                        catelog3Vos = category3level.stream().map(level3 -> new Catelog2Vo.Catelog3Vo(level2.getCatId().toString(), level3.getCatId().toString(), level3.getName())).collect(Collectors.toList());
                    }
                    return new Catelog2Vo(level1.getCatId().toString(), catelog3Vos, level1.getCatId().toString(), level1.getName());
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        return collect;
    }

    public List<CategoryEntity> getListByParentId(List<CategoryEntity> list, Long parent_cid) {
        return list.stream().filter((item) -> Objects.equals(item.getParentCid(), parent_cid)).collect(Collectors.toList());
    }


    //递归查找父节点id
    public List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //1、收集当前节点id
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        paths.add(catelogId);
        return paths;
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

    @Autowired
    StringRedisTemplate redisTemplate;

}
package com.sjy.gulimall.product.web;

import com.sjy.gulimall.product.entity.CategoryEntity;
import com.sjy.gulimall.product.service.CategoryService;
import com.sjy.gulimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;


@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @GetMapping({"/", "index.html"})
    public String getLevel1(Model model) {
        List<CategoryEntity> data = categoryService.getLevel1Categorys();
        model.addAttribute("categories", data);
        return "index";
    }

    /**
     * 查出三级分类
     * 1级分类作为key，2级引用List
     */
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        Map<String, List<Catelog2Vo>> map = categoryService.getCatalogJson();
        return map;
    }
}



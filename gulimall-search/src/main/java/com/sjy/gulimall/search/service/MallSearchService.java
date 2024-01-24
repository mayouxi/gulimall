package com.sjy.gulimall.search.service;

import com.sjy.gulimall.search.vo.SearchParam;
import com.sjy.gulimall.search.vo.SearchResult;


public interface MallSearchService {

    SearchResult search(SearchParam param);
}

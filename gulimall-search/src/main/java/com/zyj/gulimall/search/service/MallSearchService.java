package com.zyj.gulimall.search.service;

import com.zyj.gulimall.search.vo.SearchParam;
import com.zyj.gulimall.search.vo.SearchResult;

/**
 * @author Zyj
 * @date 2022/5/20
 */
public interface MallSearchService {

    SearchResult search(SearchParam param);

}

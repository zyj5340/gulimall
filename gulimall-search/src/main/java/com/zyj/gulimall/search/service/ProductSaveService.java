package com.zyj.gulimall.search.service;

import com.zyj.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

/**
 * @author Stone
 * @date 2022/5/13
 */

public interface ProductSaveService {
    boolean productSatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}

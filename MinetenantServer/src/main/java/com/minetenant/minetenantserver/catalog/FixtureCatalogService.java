package com.minetenant.minetenantserver.catalog;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.minetenant.minetenantserver.network.CatalogResultCode;

/**
 * 画面確認用の仮の取得元。通信しない。
 * 登録済みのstoreIdはその店舗の商品だけを返し(空なら0件で成功)、未登録は STORE_NOT_FOUND にする。
 */
public class FixtureCatalogService implements CatalogService {

    private final Map<String, List<ProductSummary>> productsByStore;

    public FixtureCatalogService(Map<String, List<ProductSummary>> productsByStore) {
        this.productsByStore = Map.copyOf(productsByStore);
    }

    @Override
    public CompletableFuture<CatalogResult> fetchProducts(String storeId) {
        List<ProductSummary> products = this.productsByStore.get(storeId);
        if (products == null) {
            return CompletableFuture.completedFuture(CatalogResult.failure(CatalogResultCode.STORE_NOT_FOUND));
        }

        return CompletableFuture.completedFuture(CatalogResult.success(products));
    }
}

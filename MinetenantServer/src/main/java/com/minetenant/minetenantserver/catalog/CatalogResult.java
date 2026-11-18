package com.minetenant.minetenantserver.catalog;

import java.util.List;

import com.minetenant.minetenantserver.network.CatalogResultCode;

/**
 * 商品一覧取得の結果。成功時は商品(0件なら空)、失敗時は理由だけを持ち、商品は常に空。
 */
public record CatalogResult(CatalogResultCode code, List<ProductSummary> products) {

    public CatalogResult {
        products = code == CatalogResultCode.SUCCESS ? List.copyOf(products) : List.of();
    }

    public static CatalogResult success(List<ProductSummary> products) {
        return new CatalogResult(CatalogResultCode.SUCCESS, products);
    }

    public static CatalogResult failure(CatalogResultCode code) {
        return new CatalogResult(code, List.of());
    }

    public boolean isSuccess() {
        return this.code == CatalogResultCode.SUCCESS;
    }
}

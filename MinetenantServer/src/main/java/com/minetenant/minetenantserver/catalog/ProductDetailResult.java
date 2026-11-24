package com.minetenant.minetenantserver.catalog;

import com.minetenant.minetenantserver.network.ProductDetailResultCode;

/**
 * 商品詳細取得の結果。成功時だけ商品を持ち、失敗時は理由だけを持つ(商品はnull)。
 */
public record ProductDetailResult(ProductDetailResultCode code, ProductDetail detail) {

    public ProductDetailResult {
        if (code == ProductDetailResultCode.SUCCESS) {
            if (detail == null) {
                throw new IllegalArgumentException("detail is required for success");
            }
        } else {
            detail = null;
        }
    }

    public static ProductDetailResult success(ProductDetail detail) {
        return new ProductDetailResult(ProductDetailResultCode.SUCCESS, detail);
    }

    public static ProductDetailResult failure(ProductDetailResultCode code) {
        return new ProductDetailResult(code, null);
    }

    public boolean isSuccess() {
        return this.code == ProductDetailResultCode.SUCCESS;
    }
}

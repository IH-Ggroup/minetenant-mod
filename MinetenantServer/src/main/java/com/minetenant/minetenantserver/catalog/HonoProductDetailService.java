package com.minetenant.minetenantserver.catalog;

import java.util.concurrent.CompletableFuture;

import com.minetenant.minetenantserver.http.HonoApiClient;
import com.minetenant.minetenantserver.network.ProductDetailResultCode;

/**
 * Honoの `GET /api/v1/products/{productId}` で商品詳細を取得する。毎回取得し、キャッシュしない。
 * このGETは認証不要の公開APIで、soldの商品も200で返る。存在しない・削除済みは404。
 */
public class HonoProductDetailService implements ProductDetailService {

    static final String PRODUCTS_PATH = "/api/v1/products/";

    private final HonoApiClient client;

    public HonoProductDetailService(HonoApiClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<ProductDetailResult> fetchProduct(String productId) {
        // pathに入る値なので、呼び出し元が検証済みでもここで再確認する
        if (!ProductDetailRequestProcessor.isValidProductId(productId)) {
            return CompletableFuture.completedFuture(
                    ProductDetailResult.failure(ProductDetailResultCode.INVALID_REQUEST));
        }

        return this.client.get(PRODUCTS_PATH + productId, HonoProduct.class)
                .thenApply(ProductDetailResultMapper::from);
    }
}

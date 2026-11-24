package com.minetenant.minetenantserver.catalog;

import java.util.concurrent.CompletableFuture;

/**
 * 商品詳細の取得元。常に最新の状態を取得し、キャッシュしない。
 * 失敗は例外ではなく ProductDetailResult の失敗で返す。
 */
public interface ProductDetailService {

    CompletableFuture<ProductDetailResult> fetchProduct(String productId);
}

package com.minetenant.minetenantserver.catalog;

import java.util.concurrent.CompletableFuture;

/**
 * 店舗の商品一覧の取得元。今はfixture実装だけ。Backend契約の確定後にHonoへのGETに差し替える。
 * 失敗は例外ではなく CatalogResult の失敗で返す。
 */
public interface CatalogService {

    CompletableFuture<CatalogResult> fetchProducts(String storeId);
}

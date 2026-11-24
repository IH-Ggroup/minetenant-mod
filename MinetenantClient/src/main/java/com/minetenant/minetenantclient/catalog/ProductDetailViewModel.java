package com.minetenant.minetenantclient.catalog;

import java.util.Objects;

/**
 * 詳細画面の表示用データ。ProductDetail から表示に必要な項目だけを整える。
 * 購入確認へ進めるのは販売中(available)の商品だけ。
 */
public record ProductDetailViewModel(String id, String name, String description, String priceText, String statusKey,
        boolean purchasable) {

    public ProductDetailViewModel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(priceText, "priceText");
        Objects.requireNonNull(statusKey, "statusKey");
    }

    public static ProductDetailViewModel from(ProductDetail detail) {
        boolean available = detail.status() == ProductStatus.AVAILABLE;

        return new ProductDetailViewModel(
                detail.id(),
                detail.name(),
                detail.description(),
                ProductSummaryViewModel.formatPrice(detail.price()),
                available ? ProductSummaryViewModel.STATUS_AVAILABLE_KEY : ProductSummaryViewModel.STATUS_SOLD_KEY,
                available);
    }
}

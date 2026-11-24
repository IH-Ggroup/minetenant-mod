package com.minetenant.minetenantserver.catalog;

import java.util.Objects;

/**
 * 商品詳細。Backend契約のProductから、Clientへ見せてよい項目だけを持つ。
 * sellerId・createdAtなど内部用の項目やHonoのtokenは含めない。
 * category・theme・emojiはBackendの値をそのまま保持し、Server側では解釈しない。
 */
public record ProductDetail(String id, String storeId, String name, String description, long price,
        ProductStatus status, String category, String theme, String emoji) {

    public ProductDetail {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(storeId, "storeId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(emoji, "emoji");
        if (price < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
    }
}

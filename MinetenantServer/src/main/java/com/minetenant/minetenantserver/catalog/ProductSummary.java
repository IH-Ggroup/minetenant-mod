package com.minetenant.minetenantserver.catalog;

import java.util.Objects;

/**
 * 一覧表示に必要な最小項目だけを持つ商品summary。
 * sellerの情報やHonoのtokenは含めない。項目はBackend契約の確定後に見直す。
 */
public record ProductSummary(String id, String storeId, String name, long price, ProductStatus status) {

    public ProductSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(storeId, "storeId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        if (price < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
    }
}

package com.minetenant.minetenantclient.catalog;

import java.util.List;

/** GUI確認用の固定データ。実APIがつながるまで、5つの画面状態をこれで表示する。 */
public final class ProductListFixtures {

    private static final String STORE = "store-fixture";

    private ProductListFixtures() {
    }

    /** 販売中だけの一覧。 */
    public static List<ProductSummary> available() {
        return List.of(
                new ProductSummary("p-1", STORE, "木彫りの熊", 3200, ProductStatus.AVAILABLE),
                new ProductSummary("p-2", STORE, "手織りのマフラー", 5800, ProductStatus.AVAILABLE),
                new ProductSummary("p-3", STORE, "ガラスの小皿", 1500, ProductStatus.AVAILABLE));
    }

    /** soldが混ざった一覧。長い商品名も含める。 */
    public static List<ProductSummary> withSold() {
        return List.of(
                new ProductSummary("p-1", STORE, "木彫りの熊", 3200, ProductStatus.AVAILABLE),
                new ProductSummary("p-2", STORE, "手織りのマフラー", 5800, ProductStatus.SOLD),
                new ProductSummary("p-3", STORE,
                        "とても長い商品名のテスト用データ 一点物の手作りアンティーク風ランプシェード 大 ブロンズ色",
                        1234567, ProductStatus.AVAILABLE),
                new ProductSummary("p-4", STORE, "ガラスの小皿", 1500, ProductStatus.SOLD));
    }

    /** スクロール確認用の多数件。 */
    public static List<ProductSummary> many() {
        return java.util.stream.IntStream.rangeClosed(1, 40)
                .mapToObj(i -> new ProductSummary("m-" + i, STORE, "商品 " + i, 100L * i,
                        i % 3 == 0 ? ProductStatus.SOLD : ProductStatus.AVAILABLE))
                .toList();
    }
}

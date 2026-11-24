package com.minetenant.minetenantclient.catalog;

import java.util.Optional;

/** GUI確認用の固定データ。実APIがつながるまで、詳細画面の状態をこれで表示する。 */
public final class ProductDetailFixtures {

    private static final String STORE = "store-fixture";

    private ProductDetailFixtures() {
    }

    public static ProductDetail available() {
        return new ProductDetail("p-1", STORE, "木彫りの熊",
                "職人が一本の木から彫り出した一点物です。\n手のひらに収まるサイズで、飾りとしても使えます。",
                3200, ProductStatus.AVAILABLE, "craft", "nature", "🐻");
    }

    public static ProductDetail sold() {
        return new ProductDetail("p-2", STORE, "手織りのマフラー",
                "やわらかなウール100%の手織りマフラーです。", 5800, ProductStatus.SOLD, "fashion", "winter", "🧣");
    }

    /** 長い商品名・長い説明・大きい金額。画面が崩れないかの確認用。 */
    public static ProductDetail longText() {
        String description = "とても長い説明文のテスト用データです。一点物の手作りアンティーク風ランプシェードで、"
                + "ブロンズ色の金具と手吹きガラスを組み合わせています。".repeat(6) + "\n\n"
                + "お届け後の返品はできません。";

        return new ProductDetail("p-3", STORE,
                "とても長い商品名のテスト用データ 一点物の手作りアンティーク風ランプシェード 大 ブロンズ色",
                description, 1234567, ProductStatus.AVAILABLE, "interior", "antique", "💡");
    }

    /** 一覧のfixtureで選んだ商品に対応する詳細。fixtureにない商品は empty。 */
    public static Optional<ProductDetail> forId(String id) {
        return switch (id) {
            case "p-1" -> Optional.of(available());
            case "p-2" -> Optional.of(sold());
            case "p-3" -> Optional.of(longText());
            default -> Optional.empty();
        };
    }
}

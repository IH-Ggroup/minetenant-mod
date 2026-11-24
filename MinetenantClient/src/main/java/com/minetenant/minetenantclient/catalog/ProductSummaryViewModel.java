package com.minetenant.minetenantclient.catalog;

import java.util.Locale;
import java.util.Objects;

/**
 * 一覧の1行ぶんの表示用データ。ProductSummary から表示に必要な形へ整えるだけで、通信や状態は持たない。
 * 在庫数は表示しない(一点物なので状態だけ)。
 */
public record ProductSummaryViewModel(String id, String name, String priceText, String statusKey, boolean selectable) {

    public static final String STATUS_AVAILABLE_KEY = "minetenantclient.catalog.status.available";
    public static final String STATUS_SOLD_KEY = "minetenantclient.catalog.status.sold";

    public ProductSummaryViewModel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(priceText, "priceText");
        Objects.requireNonNull(statusKey, "statusKey");
    }

    /** availableだけ選択できる。 */
    public static ProductSummaryViewModel from(ProductSummary summary) {
        boolean available = summary.status() == ProductStatus.AVAILABLE;

        return new ProductSummaryViewModel(
                summary.id(),
                summary.name(),
                formatPrice(summary.price()),
                available ? STATUS_AVAILABLE_KEY : STATUS_SOLD_KEY,
                available);
    }

    /** 円価格を3桁区切りで表示する。表示はロケールに依存させない。 */
    public static String formatPrice(long price) {
        return String.format(Locale.ROOT, "¥%,d", price);
    }
}

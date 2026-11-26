package com.minetenant.minetenantserver.catalog;

/**
 * 商品詳細をpacketへ載せるときの文字数の上限と、上限に収める処理。Minecraftのクラスには依存しない。
 * packetの符号化は上限を超える文字列で例外になるため、送信前にここで収める。
 * ClientのProductDetailResponseの復号側も同じ上限を使う(定数を揃えること)。
 */
public final class ProductDetailPacketLimits {

    public static final int MAX_ID_LENGTH = 256;
    public static final int MAX_NAME_LENGTH = 256;
    public static final int MAX_DESCRIPTION_LENGTH = 1024;
    public static final int MAX_CATEGORY_LENGTH = 64;
    public static final int MAX_THEME_LENGTH = 64;
    public static final int MAX_EMOJI_LENGTH = 32;

    private ProductDetailPacketLimits() {
    }

    /**
     * 本文系の項目は上限まで切り詰める。id・storeIdは切ると別の識別子になるため、
     * 超える場合は null を返す(送らない)。
     */
    public static ProductDetail fit(ProductDetail detail) {
        if (detail.id().length() > MAX_ID_LENGTH || detail.storeId().length() > MAX_ID_LENGTH) {
            return null;
        }

        return new ProductDetail(detail.id(), detail.storeId(),
                truncate(detail.name(), MAX_NAME_LENGTH),
                truncate(detail.description(), MAX_DESCRIPTION_LENGTH),
                detail.price(), detail.status(),
                truncate(detail.category(), MAX_CATEGORY_LENGTH),
                truncate(detail.theme(), MAX_THEME_LENGTH),
                truncate(detail.emoji(), MAX_EMOJI_LENGTH));
    }

    /** サロゲートペアを途中で切らないように、UTF-16の文字数で切り詰める。 */
    static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        int end = maxLength;
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }
}

package com.minetenant.minetenantserver.network;

/**
 * 商品詳細取得の結果の種類。通信上は数値で送る。ClientとServerで同じ定義を持つ。
 * 一度付けた数値は変更しない。
 */
public enum ProductDetailResultCode {
    SUCCESS(0),
    /** 入力(player・productId)が不正でHonoへ問い合わせなかった */
    INVALID_REQUEST(1),
    /** Honoの認証失敗(401)。Serverの設定問題 */
    UNAUTHORIZED(2),
    /** 商品が存在しない、または削除済み(404) */
    PRODUCT_NOT_FOUND(3),
    /** Hono側のエラー(5xx) */
    SERVER_ERROR(4),
    /** Honoへの要求がタイムアウトした */
    TIMEOUT(5),
    /** 上記以外の失敗(応答の形式不正を含む) */
    UNKNOWN(6);

    private final int id;

    ProductDetailResultCode(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }

    /** 未知の数値は UNKNOWN として扱う(落とさない)。 */
    public static ProductDetailResultCode fromId(int id) {
        for (ProductDetailResultCode value : values()) {
            if (value.id == id) {
                return value;
            }
        }

        return UNKNOWN;
    }
}

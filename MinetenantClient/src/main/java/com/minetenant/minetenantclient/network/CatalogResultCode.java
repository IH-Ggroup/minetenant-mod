package com.minetenant.minetenantclient.network;

/**
 * 商品一覧取得の結果の種類。通信上は数値で送る。ClientとServerで同じ定義を持つ。
 * 一度付けた数値は変更しない。
 */
public enum CatalogResultCode {
    SUCCESS(0),
    /** 入力(player・storeId)が不正でHonoへ問い合わせなかった */
    INVALID_REQUEST(1),
    /** Honoの認証失敗(401)。Serverの設定問題 */
    UNAUTHORIZED(2),
    /** 店舗が存在しない(404) */
    STORE_NOT_FOUND(3),
    /** Hono側のエラー(5xx) */
    SERVER_ERROR(4),
    /** Honoへの要求がタイムアウトした */
    TIMEOUT(5),
    /** 上記以外の失敗 */
    UNKNOWN(6);

    private final int id;

    CatalogResultCode(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }

    /** 未知の数値は UNKNOWN として扱う(落とさない)。 */
    public static CatalogResultCode fromId(int id) {
        for (CatalogResultCode value : values()) {
            if (value.id == id) {
                return value;
            }
        }

        return UNKNOWN;
    }
}

package com.minetenant.minetenantserver.network;

/**
 * 連携結果の種類。通信上は数値で送る。ClientとServerで同じ定義を持つ。
 * 一度付けた数値は変更しない。
 */
public enum LinkResultCode {
    SUCCESS(0),
    /** コードが無効・期限切れ・使用済みなどで受け付けられなかった */
    CODE_REJECTED(1),
    /** 上記以外の失敗 */
    UNKNOWN(2);

    private final int id;

    LinkResultCode(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }

    /** 未知の数値は UNKNOWN として扱う(落とさない)。 */
    public static LinkResultCode fromId(int id) {
        for (LinkResultCode value : values()) {
            if (value.id == id) {
                return value;
            }
        }

        return UNKNOWN;
    }
}

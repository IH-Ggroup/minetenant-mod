package com.minetenant.minetenantclient.link;

/**
 * 連携コード入力で表示するエラーの種類。
 * 文言は翻訳キーで持ち、表示側で翻訳する。番号は問い合わせやログ検索用。
 * Client内の仮の番号。Web/Hono側の体系が決まったら見直す。
 * 一度付けた番号は変更しない。削除した番号も再利用しない。
 */
public enum LinkError {
    /** 未入力 */
    EMPTY("LINK-001", "minetenantclient.link.error.empty"),
    /** 形式が不正 */
    INVALID_FORMAT("LINK-002", "minetenantclient.link.error.invalid_format"),
    /** コードが無効・期限切れ・使用済みなどで受け付けられなかった */
    CODE_REJECTED("LINK-003", "minetenantclient.link.error.code_rejected"),
    /** 上記以外の失敗。今後エラーが増えても番号は999のまま */
    UNKNOWN("LINK-999", "minetenantclient.link.error.unknown");

    private final String code;
    private final String translationKey;

    LinkError(String code, String translationKey) {
        this.code = code;
        this.translationKey = translationKey;
    }

    /** エラー番号(例: LINK-001) */
    public String code() {
        return this.code;
    }

    public String translationKey() {
        return this.translationKey;
    }
}

package com.minetenant.minetenantclient.link;

/**
 * 仮の形式検証。桁数・文字種は未確定のため固定しない。
 * 前後の空白を除き、空・空白や制御文字を含む・長すぎるものだけ弾く。
 * 本番の形式が決まったら差し替える。
 */
public class DefaultLinkCodeValidator implements LinkCodeValidator {

    /** 仮の上限。入力欄の暴走を防ぐためだけの値で、本番仕様ではない。 */
    public static final int MAX_LENGTH = 64;

    @Override
    public Result validate(String raw) {
        String code = raw == null ? "" : raw.strip();

        if (code.isEmpty()) {
            return Result.invalid(LinkError.EMPTY);
        }

        if (code.length() > MAX_LENGTH || code.chars().anyMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c))) {
            return Result.invalid(LinkError.INVALID_FORMAT);
        }

        return Result.ok(code);
    }
}

package com.minetenant.minetenantserver.http;

import java.util.Map;

import com.minetenant.minetenantserver.config.HonoConfig;

/**
 * Server認証headerとrequest IDのheader名を1か所にまとめる。
 * 値はbackendのapi.mdで未確定(B-CONTRACT-05以降)のため仮置き。確定したらここだけ直す。
 */
public final class ServerAuthHeaderProvider {

    static final String REQUEST_ID_HEADER = "X-Request-Id";

    private ServerAuthHeaderProvider() {
    }

    /** token未設定なら空を返す。tokenはここ以外に渡さない。 */
    public static Map<String, String> headers(HonoConfig config) {
        if (!config.hasToken()) {
            return Map.of();
        }
        return Map.of("Authorization", "Bearer " + config.serverToken());
    }
}

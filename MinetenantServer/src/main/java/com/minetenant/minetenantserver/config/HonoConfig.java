package com.minetenant.minetenantserver.config;

import java.net.URI;
import java.time.Duration;

/** Honoへの接続設定。tokenはtoString等に出さない。 */
public record HonoConfig(
        URI baseUrl,
        String serverToken,
        Duration connectTimeout,
        Duration readTimeout,
        Duration pollInterval) {

    public boolean hasToken() {
        return serverToken != null && !serverToken.isBlank();
    }

    @Override
    public String toString() {
        return "HonoConfig[baseUrl=" + baseUrl
                + ", serverToken=" + (hasToken() ? "****" : "(not set)")
                + ", connectTimeout=" + connectTimeout
                + ", readTimeout=" + readTimeout
                + ", pollInterval=" + pollInterval + "]";
    }
}

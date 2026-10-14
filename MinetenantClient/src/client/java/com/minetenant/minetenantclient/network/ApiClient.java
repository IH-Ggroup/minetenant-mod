package com.minetenant.minetenantclient.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletionException;

import net.minecraft.client.MinecraftClient;

import fi.dy.masa.malilib.util.StringUtils;

import com.minetenant.minetenantclient.config.Configs;
import com.minetenant.minetenantclient.debug.DebugLog;
import com.minetenant.minetenantclient.debug.DebugLog.Level;

/**
 * WebAPIバックエンドとの疎通確認(GETして返ってきたJSONをログに出す)を担当する。
 */
public final class ApiClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private ApiClient() {
    }

    public static void sendRequest() {
        String url = Configs.Generic.API_URL.getStringValue().trim();

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            DebugLog.add(Level.ERROR, StringUtils.translate("minetenantclient.debug.log.api_invalid_url", url));
            return;
        }

        DebugLog.add(Level.INFO, StringUtils.translate("minetenantclient.debug.log.api_sent", url));
        long start = System.nanoTime();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    long millis = (System.nanoTime() - start) / 1_000_000L;
                    // ログへの追加はメインスレッドで行う
                    MinecraftClient.getInstance().execute(() -> onComplete(response, error, millis));
                });
    }

    private static void onComplete(HttpResponse<String> response, Throwable error, long millis) {
        if (error != null) {
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            DebugLog.add(Level.ERROR, StringUtils.translate("minetenantclient.debug.log.api_failed", cause.toString()));
            return;
        }

        if (response.statusCode() != 200) {
            DebugLog.add(Level.ERROR, StringUtils.translate("minetenantclient.debug.log.api_status", response.statusCode()));
            return;
        }

        DebugLog.add(Level.SUCCESS, StringUtils.translate("minetenantclient.debug.log.api_success", millis));
        DebugLog.add(Level.INFO, response.body().replaceAll("\\s+", " ").trim());
    }
}

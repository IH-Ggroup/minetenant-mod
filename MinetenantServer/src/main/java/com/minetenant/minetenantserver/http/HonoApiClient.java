package com.minetenant.minetenantserver.http;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minetenant.minetenantserver.config.HonoConfig;

/**
 * HonoへのGETを非同期で行う共通クライアント。
 * Futureは例外完了せず、必ずApiResultで完了する。
 * 結果の後処理でworldを触る場合は、呼び出し側でServer threadへ戻すこと(例: thenAcceptAsync(handler, server))。
 */
public final class HonoApiClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger("minetenantserver");
    private static final int DEFAULT_MAX_IN_FLIGHT = 8;
    private static final int MAX_THREADS = 4;

    private final String baseUrl;
    private final HonoConfig config;
    private final Map<String, String> headers;
    private final Semaphore permits;
    private final ExecutorService executor;
    private final HttpClient httpClient;
    private volatile boolean closed;

    public HonoApiClient(HonoConfig config, Map<String, String> headers) {
        this(config, headers, DEFAULT_MAX_IN_FLIGHT);
    }

    /** maxInFlight: 同時に処理するリクエスト数の上限。超えた分はREJECTEDにする。 */
    public HonoApiClient(HonoConfig config, Map<String, String> headers, int maxInFlight) {
        String url = config.baseUrl().toString();
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.config = config;
        this.headers = Map.copyOf(headers);
        this.permits = new Semaphore(maxInFlight);
        this.executor = Executors.newFixedThreadPool(Math.max(2, Math.min(maxInFlight, MAX_THREADS)), daemonThreads());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .executor(executor)
                .build();
    }

    public <T> CompletableFuture<ApiResult<T>> get(String path, Class<T> type) {
        return get(path, (Type) type);
    }

    public <T> CompletableFuture<ApiResult<T>> get(String path, Type type) {
        if (closed) {
            return failed(path, ApiError.Kind.REJECTED);
        }
        if (!permits.tryAcquire()) {
            return failed(path, ApiError.Kind.REJECTED);
        }

        CompletableFuture<HttpResponse<String>> sent;
        try {
            sent = httpClient.sendAsync(buildRequest(path), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            permits.release();
            return failed(path, ApiError.Kind.INVALID_REQUEST);
        } catch (RuntimeException e) {
            permits.release();
            return failed(path, ApiError.Kind.REJECTED);
        }

        return sent.handle((response, throwable) -> {
            permits.release();
            ApiResult<T> result = throwable == null
                    ? toResult(response, type)
                    : new ApiResult.Failure<>(ApiError.of(kindOf(throwable)));
            logIfFailed(path, result);
            return result;
        });
    }

    /** 以降の呼び出しを拒否し、実行中の通信も打ち切る。複数回呼んでも安全。 */
    @Override
    public void close() {
        closed = true;
        httpClient.shutdownNow();
        executor.shutdownNow();
    }

    private HttpRequest buildRequest(String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + normalized))
                .timeout(config.readTimeout())
                .header("Accept", "application/json")
                .GET();
        headers.forEach(builder::header);
        return builder.build();
    }

    private static <T> ApiResult<T> toResult(HttpResponse<String> response, Type type) {
        int status = response.statusCode();
        if (status == 200) {
            return JsonCodec.decode(response.body(), type);
        }
        ApiError.Kind kind = status == 404 ? ApiError.Kind.NOT_FOUND
                : status >= 500 ? ApiError.Kind.SERVER_ERROR
                : ApiError.Kind.HTTP_ERROR;
        return new ApiResult.Failure<>(new ApiError(kind, status));
    }

    private static ApiError.Kind kindOf(Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof HttpTimeoutException) {
            return ApiError.Kind.TIMEOUT;
        }
        if (cause instanceof RejectedExecutionException) {
            return ApiError.Kind.REJECTED;
        }
        return ApiError.Kind.NETWORK;
    }

    private static <T> CompletableFuture<ApiResult<T>> failed(String path, ApiError.Kind kind) {
        ApiResult<T> result = new ApiResult.Failure<>(ApiError.of(kind));
        logIfFailed(path, result);
        return CompletableFuture.completedFuture(result);
    }

    /** ログにはkindとpath(クエリ除く)だけを出し、URL全体・本文・tokenは出さない。 */
    private static void logIfFailed(String path, ApiResult<?> result) {
        if (result instanceof ApiResult.Failure<?> failure) {
            int query = path.indexOf('?');
            LOGGER.warn("Hono GET failed: kind={} path={}", failure.error().kind(),
                    query >= 0 ? path.substring(0, query) : path);
        }
    }

    private static java.util.concurrent.ThreadFactory daemonThreads() {
        AtomicInteger count = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "minetenant-hono-http-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}

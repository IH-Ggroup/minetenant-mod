package com.minetenant.minetenantserver.http;

import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minetenant.minetenantserver.config.HonoConfig;

/**
 * HonoへのGET/POSTを非同期で行う共通クライアント。
 * Futureは例外完了せず、必ずApiResult(GET)またはPostOutcome(POST)で完了する。
 * 認証エラー(UNAUTHORIZED / FORBIDDEN / AUTH_UNAVAILABLE)は自動再送しない。呼び出し側も設定が直るまで再送せず、
 * AUTH_UNAVAILABLEは業務処理前の応答なので未送信として扱い、間隔を空けてから送り直すこと。
 * 結果の後処理でworldを触る場合は、呼び出し側でServer threadへ戻すこと(例: thenAcceptAsync(handler, server))。
 */
public final class HonoApiClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger("minetenantserver");
    private static final int DEFAULT_MAX_IN_FLIGHT = 8;
    private static final int MAX_THREADS = 4;
    // 仮置き。backendのapi.mdで確定したら直す
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    /** Fabric Server認証(Bearer)を付ける対象。api.mdの「保護するpath」に合わせ、このprefix配下だけに付ける。 */
    private static final String MINECRAFT_PATH_PREFIX = "/api/v1/minecraft/";
    // api.mdの「認証結果とerror」で固定されたcode
    private static final String CODE_FORBIDDEN = "FABRIC_FORBIDDEN";
    private static final String CODE_AUTH_UNAVAILABLE = "FABRIC_AUTHENTICATION_UNAVAILABLE";

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
        if (lacksRequiredToken(path)) {
            return CompletableFuture.completedFuture(
                    loggedGet(path, new ApiResult.Failure<>(ApiError.of(ApiError.Kind.UNAUTHORIZED))));
        }
        return this.<ApiResult<T>>execute(
                () -> newRequest(path).GET().build(),
                kind -> loggedGet(path, new ApiResult.Failure<>(ApiError.of(kind))),
                (response, throwable) -> loggedGet(path, throwable == null
                        ? toResult(response, type)
                        : new ApiResult.Failure<>(ApiError.of(kindOf(throwable)))));
    }

    public <T> CompletableFuture<PostOutcome<T>> post(String path, String requestId, Object body, Class<T> type) {
        return post(path, requestId, body, (Type) type);
    }

    /**
     * JSON本文をPOSTする。requestIdを付ける。Minecraft routeにはFabric Server認証(Bearer)を付ける。
     * requestIdは呼び出し側が持ち、結果不明(Unknown)からの再送では同じ値を渡すこと。
     */
    public <T> CompletableFuture<PostOutcome<T>> post(String path, String requestId, Object body, Type type) {
        if (lacksRequiredToken(path)) {
            return CompletableFuture.completedFuture(
                    loggedPost(path, new PostOutcome.NotSent<>(ApiError.Kind.UNAUTHORIZED)));
        }
        String json;
        try {
            json = JsonCodec.encode(body);
        } catch (RuntimeException e) {
            return CompletableFuture.completedFuture(
                    loggedPost(path, new PostOutcome.NotSent<>(ApiError.Kind.INVALID_REQUEST)));
        }
        return this.<PostOutcome<T>>execute(
                () -> {
                    return newRequest(path)
                            .header("Content-Type", "application/json; charset=utf-8")
                            .header(REQUEST_ID_HEADER, requestId)
                            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                            .build();
                },
                kind -> loggedPost(path, new PostOutcome.NotSent<>(kind)),
                (response, throwable) -> loggedPost(path, throwable == null
                        ? toOutcome(response, type)
                        : outcomeOf(throwable)));
    }

    /** 以降の呼び出しを拒否し、実行中の通信も打ち切る。複数回呼んでも安全。 */
    @Override
    public void close() {
        closed = true;
        httpClient.shutdownNow();
        executor.shutdownNow();
    }

    /** 終了状態と同時実行数を見てから送る。送れなかった理由はnotSentで結果型へ変換する。 */
    private <R> CompletableFuture<R> execute(Supplier<HttpRequest> request,
            Function<ApiError.Kind, R> notSent, BiFunction<HttpResponse<String>, Throwable, R> done) {
        if (closed || !permits.tryAcquire()) {
            return CompletableFuture.completedFuture(notSent.apply(ApiError.Kind.REJECTED));
        }

        CompletableFuture<HttpResponse<String>> sent;
        try {
            sent = httpClient.sendAsync(request.get(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            permits.release();
            return CompletableFuture.completedFuture(notSent.apply(ApiError.Kind.INVALID_REQUEST));
        } catch (RuntimeException e) {
            permits.release();
            return CompletableFuture.completedFuture(notSent.apply(ApiError.Kind.REJECTED));
        }

        return sent.handle((response, throwable) -> {
            permits.release();
            return done.apply(response, throwable);
        });
    }

    /** Minecraft routeなのにtokenが無い場合は、認証なしで送らず未送信にする。 */
    private boolean lacksRequiredToken(String path) {
        return isMinecraftPath(path) && !config.hasToken();
    }

    private static boolean isMinecraftPath(String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        return stripQuery(normalized).startsWith(MINECRAFT_PATH_PREFIX);
    }

    private HttpRequest.Builder newRequest(String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + normalized))
                .timeout(config.readTimeout())
                .header("Accept", "application/json");
        headers.forEach(builder::header);
        if (isMinecraftPath(path)) {
            // 共通headerより後に付け、上書きされないようにする。値はログに出さない
            builder.setHeader("Authorization", "Bearer " + config.serverToken());
        }
        return builder;
    }

    private static <T> ApiResult<T> toResult(HttpResponse<String> response, Type type) {
        int status = response.statusCode();
        if (status == 200) {
            return JsonCodec.decode(response.body(), type);
        }
        String code = JsonCodec.decodeError(response.body()).code();
        ApiError.Kind kind = status == 401 ? ApiError.Kind.UNAUTHORIZED
                : status == 403 && CODE_FORBIDDEN.equals(code) ? ApiError.Kind.FORBIDDEN
                : status == 503 && CODE_AUTH_UNAVAILABLE.equals(code) ? ApiError.Kind.AUTH_UNAVAILABLE
                : status == 404 ? ApiError.Kind.NOT_FOUND
                : status >= 500 ? ApiError.Kind.SERVER_ERROR
                : ApiError.Kind.HTTP_ERROR;
        return new ApiResult.Failure<>(new ApiError(kind, status));
    }

    private static <T> PostOutcome<T> toOutcome(HttpResponse<String> response, Type type) {
        int status = response.statusCode();
        if (status == 200 || status == 201) {
            // 成功statusで本文が読めない場合、処理は済んでいる可能性があるため失敗にせず結果不明にする
            ApiResult<T> decoded = JsonCodec.decode(response.body(), type);
            if (decoded instanceof ApiResult.Success<T> success) {
                return new PostOutcome.Success<>(success.data());
            }
            return new PostOutcome.Unknown<>(((ApiResult.Failure<T>) decoded).error().kind());
        }
        JsonCodec.ErrorBody error = JsonCodec.decodeError(response.body());
        // 認証層の503は業務処理の前に返るため、結果不明ではなく未送信として扱う
        if (status == 503 && CODE_AUTH_UNAVAILABLE.equals(error.code())) {
            return new PostOutcome.NotSent<>(ApiError.Kind.AUTH_UNAVAILABLE);
        }
        // 5xxは処理が完了した後に失敗した可能性があるため、失敗と決めつけず結果不明にする
        if (status >= 500) {
            return new PostOutcome.Unknown<>(ApiError.Kind.SERVER_ERROR);
        }
        return new PostOutcome.BusinessError<>(status, error.code(), error.message());
    }

    /** 接続できなかった場合は未送信、送信後のtimeoutや切断は結果不明として扱う。 */
    private static <T> PostOutcome<T> outcomeOf(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof HttpConnectTimeoutException || cause instanceof ConnectException) {
            return new PostOutcome.NotSent<>(ApiError.Kind.NETWORK);
        }
        if (cause instanceof RejectedExecutionException) {
            return new PostOutcome.NotSent<>(ApiError.Kind.REJECTED);
        }
        return new PostOutcome.Unknown<>(kindOf(throwable));
    }

    private static ApiError.Kind kindOf(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof HttpTimeoutException) {
            return ApiError.Kind.TIMEOUT;
        }
        if (cause instanceof RejectedExecutionException) {
            return ApiError.Kind.REJECTED;
        }
        return ApiError.Kind.NETWORK;
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    /** ログにはkindとpath(クエリ除く)だけを出し、URL全体・本文・tokenは出さない。 */
    private static <T> ApiResult<T> loggedGet(String path, ApiResult<T> result) {
        if (result instanceof ApiResult.Failure<T> failure) {
            if (isAuthKind(failure.error().kind())) {
                // 設定や運用の対応が要るため、通常の失敗よりerrorで目立たせる
                LOGGER.error("Hono GET authentication failed: kind={} path={}", failure.error().kind(), stripQuery(path));
            } else {
                LOGGER.warn("Hono GET failed: kind={} path={}", failure.error().kind(), stripQuery(path));
            }
        }
        return result;
    }

    /** ログにはoutcomeの種類・status・codeとpath(クエリ除く)だけを出し、URL全体・本文・messageは出さない。 */
    private static <T> PostOutcome<T> loggedPost(String path, PostOutcome<T> outcome) {
        if (outcome instanceof PostOutcome.BusinessError<T> e && isAuthCode(e.code())) {
            LOGGER.error("Hono POST authentication rejected: status={} code={} path={}", e.status(), e.code(), stripQuery(path));
        } else if (outcome instanceof PostOutcome.NotSent<T> e && isAuthKind(e.reason())) {
            LOGGER.error("Hono POST authentication unavailable: reason={} path={}", e.reason(), stripQuery(path));
        } else if (outcome instanceof PostOutcome.BusinessError<T> e) {
            LOGGER.warn("Hono POST rejected: status={} code={} path={}", e.status(), e.code(), stripQuery(path));
        } else if (outcome instanceof PostOutcome.Unknown<T> e) {
            LOGGER.warn("Hono POST result unknown: reason={} path={}", e.reason(), stripQuery(path));
        } else if (outcome instanceof PostOutcome.NotSent<T> e) {
            LOGGER.warn("Hono POST not sent: reason={} path={}", e.reason(), stripQuery(path));
        }
        return outcome;
    }

    private static boolean isAuthKind(ApiError.Kind kind) {
        return kind == ApiError.Kind.UNAUTHORIZED || kind == ApiError.Kind.FORBIDDEN
                || kind == ApiError.Kind.AUTH_UNAVAILABLE;
    }

    private static boolean isAuthCode(String code) {
        return code != null && code.startsWith("FABRIC_");
    }

    private static String stripQuery(String path) {
        int query = path.indexOf('?');
        return query >= 0 ? path.substring(0, query) : path;
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

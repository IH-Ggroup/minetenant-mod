package com.minetenant.minetenantserver.http;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minetenant.minetenantserver.config.HonoConfig;
import com.sun.net.httpserver.HttpServer;

class HonoApiClientTest {

    record Item(String id) {
    }

    private HttpServer server;
    private HonoApiClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        server.stop(0);
    }

    private void respond(String path, int status, String body) {
        server.createContext(path, ex -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                ex.getResponseBody().write(bytes);
            }
            ex.close();
        });
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private HonoConfig config(String baseUrl, Duration readTimeout) {
        return new HonoConfig(URI.create(baseUrl), "secret-token",
                Duration.ofSeconds(2), readTimeout, Duration.ofSeconds(3));
    }

    private HonoApiClient newClient(Duration readTimeout) {
        client = new HonoApiClient(config(baseUrl(), readTimeout), Map.of(), 4);
        return client;
    }

    private ApiResult<Item> get(String path) {
        return client.get(path, Item.class).join();
    }

    private static ApiError.Kind kindOf(ApiResult<?> r) {
        return assertInstanceOf(ApiResult.Failure.class, r).error().kind();
    }

    @Test
    void 状態200のdataを返す() {
        respond("/items/1", 200, "{\"data\":{\"id\":\"a\"}}");
        newClient(Duration.ofSeconds(2));
        assertEquals(new Item("a"), assertInstanceOf(ApiResult.Success.class, get("/items/1")).data());
    }

    @Test
    void 状態404はNOT_FOUND() {
        respond("/items/x", 404, "{\"message\":\"not found\"}");
        newClient(Duration.ofSeconds(2));
        assertEquals(ApiError.Kind.NOT_FOUND, kindOf(get("/items/x")));
    }

    @Test
    void 状態500はSERVER_ERROR() {
        respond("/items/err", 500, "{\"message\":\"boom\"}");
        newClient(Duration.ofSeconds(2));
        assertEquals(ApiError.Kind.SERVER_ERROR, kindOf(get("/items/err")));
    }

    @Test
    void その他の状態はHTTP_ERRORで状態コードを持つ() {
        respond("/items/teapot", 418, "");
        newClient(Duration.ofSeconds(2));
        ApiError error = assertInstanceOf(ApiResult.Failure.class, get("/items/teapot")).error();
        assertEquals(ApiError.Kind.HTTP_ERROR, error.kind());
        assertEquals(418, error.status());
    }

    @Test
    void 不正JSONでも例外にならずINVALID_JSON() {
        respond("/items/bad", 200, "{not json");
        newClient(Duration.ofSeconds(2));
        assertEquals(ApiError.Kind.INVALID_JSON, kindOf(get("/items/bad")));
    }

    @Test
    void 応答が遅いとTIMEOUT() {
        server.createContext("/slow", ex -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
                // テスト終了時の割り込みは無視する
            }
            ex.close();
        });
        newClient(Duration.ofMillis(300));
        assertEquals(ApiError.Kind.TIMEOUT, kindOf(get("/slow")));
    }

    @Test
    void 接続できないときはNETWORK() {
        int closedPort = server.getAddress().getPort();
        server.stop(0);
        client = new HonoApiClient(config("http://127.0.0.1:" + closedPort, Duration.ofSeconds(2)), Map.of(), 4);
        assertEquals(ApiError.Kind.NETWORK, kindOf(get("/items/1")));
    }

    @Test
    void 指定したヘッダを送る() {
        AtomicReference<String> received = new AtomicReference<>();
        server.createContext("/h", ex -> {
            received.set(ex.getRequestHeaders().getFirst("X-Test"));
            byte[] bytes = "{\"data\":{\"id\":\"a\"}}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        client = new HonoApiClient(config(baseUrl(), Duration.ofSeconds(2)), Map.of("X-Test", "v"), 4);
        get("/h");
        assertEquals("v", received.get());
    }

    @Test
    void baseUrl末尾のスラッシュとpath先頭のスラッシュは重ならない() {
        respond("/items/1", 200, "{\"data\":{\"id\":\"a\"}}");
        client = new HonoApiClient(config(baseUrl() + "/", Duration.ofSeconds(2)), Map.of(), 4);
        assertInstanceOf(ApiResult.Success.class, get("/items/1"));
    }

    @Test
    void 同時実行数の上限を超えるとREJECTED() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        server.createContext("/block", ex -> {
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                // テスト終了時の割り込みは無視する
            }
            ex.close();
        });
        client = new HonoApiClient(config(baseUrl(), Duration.ofSeconds(5)), Map.of(), 1);

        CompletableFuture<ApiResult<Item>> first = client.get("/block", Item.class);
        assertTrue(entered.await(3, TimeUnit.SECONDS));

        assertEquals(ApiError.Kind.REJECTED, kindOf(client.get("/block", Item.class).join()));
        release.countDown();
        first.join();
    }

    @Test
    void close後の呼び出しはREJECTEDで落ちない() {
        respond("/items/1", 200, "{\"data\":{\"id\":\"a\"}}");
        newClient(Duration.ofSeconds(2));
        client.close();
        assertEquals(ApiError.Kind.REJECTED, kindOf(get("/items/1")));
    }

    @Test
    void 失敗メッセージにURLとtokenを含めない() {
        respond("/items/err", 500, "{\"message\":\"secret-body\"}");
        newClient(Duration.ofSeconds(2));
        String message = assertInstanceOf(ApiResult.Failure.class, get("/items/err")).error().message();
        assertFalse(message.contains("127.0.0.1"));
        assertFalse(message.contains("secret-token"));
        assertFalse(message.contains("secret-body"));
    }
}

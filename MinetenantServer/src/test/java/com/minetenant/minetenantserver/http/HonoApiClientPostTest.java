package com.minetenant.minetenantserver.http;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minetenant.minetenantserver.config.HonoConfig;
import com.sun.net.httpserver.HttpServer;

class HonoApiClientPostTest {

    record Item(String id) {
    }

    record Body(String name) {
    }

    private static final String TOKEN = "secret-token";

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
            ex.getRequestBody().readAllBytes();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                ex.getResponseBody().write(bytes);
            }
            ex.close();
        });
    }

    private HonoConfig config(String token, Duration readTimeout) {
        return new HonoConfig(URI.create("http://127.0.0.1:" + server.getAddress().getPort()), token,
                Duration.ofSeconds(2), readTimeout, Duration.ofSeconds(3));
    }

    private HonoApiClient newClient(String token, Duration readTimeout) {
        client = new HonoApiClient(config(token, readTimeout), Map.of(), 4);
        return client;
    }

    private PostOutcome<Item> post(String path) {
        return client.post(path, "req-1", new Body("n"), Item.class).join();
    }

    @Test
    void 状態200と201のdataを返す() {
        respond("/ok200", 200, "{\"data\":{\"id\":\"a\"}}");
        respond("/ok201", 201, "{\"data\":{\"id\":\"b\"}}");
        newClient(TOKEN, Duration.ofSeconds(2));
        assertEquals(new Item("a"), assertInstanceOf(PostOutcome.Success.class, post("/ok200")).data());
        assertEquals(new Item("b"), assertInstanceOf(PostOutcome.Success.class, post("/ok201")).data());
    }

    @Test
    void requestIdとJSON本文を送る() {
        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server.createContext("/send", ex -> {
            requestId.set(ex.getRequestHeaders().getFirst("X-Request-Id"));
            contentType.set(ex.getRequestHeaders().getFirst("Content-Type"));
            body.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = "{\"data\":{\"id\":\"a\"}}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        newClient(TOKEN, Duration.ofSeconds(2));
        post("/send");
        assertEquals("req-1", requestId.get());
        assertTrue(contentType.get().startsWith("application/json"));
        assertEquals("{\"name\":\"n\"}", body.get());
    }

    @Test
    void 業務エラーはstatusとcodeを持つ() {
        respond("/conflict", 409, "{\"message\":\"m\",\"code\":\"REQUEST_ID_CONFLICT\"}");
        respond("/invalid", 422, "{\"message\":\"m\",\"code\":\"VALIDATION\",\"errors\":{}}");
        newClient(TOKEN, Duration.ofSeconds(2));
        var conflict = assertInstanceOf(PostOutcome.BusinessError.class, post("/conflict"));
        assertEquals(409, conflict.status());
        assertEquals("REQUEST_ID_CONFLICT", conflict.code());
        assertEquals(422, assertInstanceOf(PostOutcome.BusinessError.class, post("/invalid")).status());
    }

    @Test
    void 状態401は本文にcodeがなくても業務エラー() {
        respond("/unauth", 401, "");
        newClient(TOKEN, Duration.ofSeconds(2));
        var unauth = assertInstanceOf(PostOutcome.BusinessError.class, post("/unauth"));
        assertEquals(401, unauth.status());
        assertNull(unauth.code());
    }

    @Test
    void 状態5xxは結果不明() {
        respond("/boom", 500, "<html>error</html>");
        respond("/unavailable", 503, "");
        newClient(TOKEN, Duration.ofSeconds(2));
        assertEquals(ApiError.Kind.SERVER_ERROR, assertInstanceOf(PostOutcome.Unknown.class, post("/boom")).reason());
        assertEquals(ApiError.Kind.SERVER_ERROR, assertInstanceOf(PostOutcome.Unknown.class, post("/unavailable")).reason());
    }

    @Test
    void timeoutは失敗でなく結果不明() {
        server.createContext("/slow", ex -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
                // テスト終了時の割り込みは無視する
            }
            ex.close();
        });
        newClient(TOKEN, Duration.ofMillis(300));
        var unknown = assertInstanceOf(PostOutcome.Unknown.class, post("/slow"));
        assertEquals(ApiError.Kind.TIMEOUT, unknown.reason());
    }

    @Test
    void 成功statusでも本文が不正なら結果不明() {
        respond("/bad", 201, "{not json");
        newClient(TOKEN, Duration.ofSeconds(2));
        var unknown = assertInstanceOf(PostOutcome.Unknown.class, post("/bad"));
        assertEquals(ApiError.Kind.INVALID_JSON, unknown.reason());
    }

    @Test
    void 接続できないときは未送信() {
        int closedPort = server.getAddress().getPort();
        server.stop(0);
        client = new HonoApiClient(new HonoConfig(URI.create("http://127.0.0.1:" + closedPort), TOKEN,
                Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofSeconds(3)), Map.of(), 4);
        assertInstanceOf(PostOutcome.NotSent.class, post("/x"));
    }

    @Test
    void close後はREJECTEDの未送信() {
        respond("/ok", 200, "{\"data\":{\"id\":\"a\"}}");
        newClient(TOKEN, Duration.ofSeconds(2));
        client.close();
        var notSent = assertInstanceOf(PostOutcome.NotSent.class, post("/ok"));
        assertEquals(ApiError.Kind.REJECTED, notSent.reason());
    }

    @Test
    void 結果のtoStringにtokenと本文のmessageを含めない() {
        respond("/err", 422, "{\"message\":\"secret-body\",\"code\":\"X\"}");
        newClient(TOKEN, Duration.ofSeconds(2));
        String text = post("/err").toString();
        assertFalse(text.contains(TOKEN));
        assertFalse(text.contains("secret-body"));
    }
}

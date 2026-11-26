package com.minetenant.minetenantserver.http;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minetenant.minetenantserver.config.HonoConfig;
import com.sun.net.httpserver.HttpServer;

/** Fabric認証のerror(401・403・503)の分類を確認する。 */
class HonoApiClientAuthErrorTest {

    record Item(String id) {
    }

    record Body(String name) {
    }

    private static final String TOKEN = "a".repeat(64);
    private static final String GET_PATH = "/api/v1/minecraft/catalog";
    private static final String POST_PATH = "/api/v1/minecraft/purchases";

    private HttpServer server;
    private HonoApiClient client;
    private volatile int status;
    private volatile String body = "";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            ex.getRequestBody().readAllBytes();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                ex.getResponseBody().write(bytes);
            }
            ex.close();
        });
        server.start();
        HonoConfig config = new HonoConfig(URI.create("http://127.0.0.1:" + server.getAddress().getPort()), TOKEN,
                Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofSeconds(3));
        client = new HonoApiClient(config, Map.of(), 4);
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.stop(0);
    }

    private void reply(int status, String code) {
        this.status = status;
        this.body = "{\"message\":\"m\",\"code\":\"" + code + "\"}";
    }

    private ApiError.Kind getKind() {
        return assertInstanceOf(ApiResult.Failure.class, client.get(GET_PATH, Item.class).join()).error().kind();
    }

    @Test
    void GETの401_403_503を区別する() {
        reply(401, "FABRIC_AUTHENTICATION_FAILED");
        assertEquals(ApiError.Kind.UNAUTHORIZED, getKind());
        reply(403, "FABRIC_FORBIDDEN");
        assertEquals(ApiError.Kind.FORBIDDEN, getKind());
        reply(503, "FABRIC_AUTHENTICATION_UNAVAILABLE");
        assertEquals(ApiError.Kind.AUTH_UNAVAILABLE, getKind());
    }

    @Test
    void codeが違う403と503は認証errorにしない() {
        reply(403, "OTHER");
        assertEquals(ApiError.Kind.HTTP_ERROR, getKind());
        status = 503;
        body = "";
        assertEquals(ApiError.Kind.SERVER_ERROR, getKind());
    }

    @Test
    void POSTの401と403は業務errorとして返す() {
        reply(401, "FABRIC_AUTHENTICATION_FAILED");
        var unauthorized = assertInstanceOf(PostOutcome.BusinessError.class,
                client.post(POST_PATH, "req-1", new Body("n"), Item.class).join());
        assertEquals(401, unauthorized.status());
        assertEquals("FABRIC_AUTHENTICATION_FAILED", unauthorized.code());

        reply(403, "FABRIC_FORBIDDEN");
        var forbidden = assertInstanceOf(PostOutcome.BusinessError.class,
                client.post(POST_PATH, "req-1", new Body("n"), Item.class).join());
        assertEquals("FABRIC_FORBIDDEN", forbidden.code());
    }

    @Test
    void POSTの認証503は未送信で他の503は結果不明() {
        reply(503, "FABRIC_AUTHENTICATION_UNAVAILABLE");
        var notSent = assertInstanceOf(PostOutcome.NotSent.class,
                client.post(POST_PATH, "req-1", new Body("n"), Item.class).join());
        assertEquals(ApiError.Kind.AUTH_UNAVAILABLE, notSent.reason());

        reply(503, "OTHER");
        var unknown = assertInstanceOf(PostOutcome.Unknown.class,
                client.post(POST_PATH, "req-1", new Body("n"), Item.class).join());
        assertEquals(ApiError.Kind.SERVER_ERROR, unknown.reason());
    }
}

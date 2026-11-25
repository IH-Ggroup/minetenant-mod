package com.minetenant.minetenantserver.http;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minetenant.minetenantserver.config.HonoConfig;
import com.sun.net.httpserver.HttpServer;

/** Minecraft routeへのFabric Server認証(Bearer)の付与を確認する。 */
class HonoApiClientAuthTest {

    record Item(String id) {
    }

    record Body(String name) {
    }

    private static final String TOKEN = "a".repeat(64);

    private HttpServer server;
    private HonoApiClient client;
    private final List<List<String>> authorizations = new CopyOnWriteArrayList<>();
    private final AtomicInteger hits = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            hits.incrementAndGet();
            List<String> values = ex.getRequestHeaders().get("Authorization");
            authorizations.add(values == null ? List.of() : List.copyOf(values));
            ex.getRequestBody().readAllBytes();
            byte[] bytes = "{\"data\":{\"id\":\"a\"}}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        server.stop(0);
    }

    private HonoApiClient newClient(String token, Map<String, String> headers) {
        HonoConfig config = new HonoConfig(URI.create("http://127.0.0.1:" + server.getAddress().getPort()), token,
                Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofSeconds(3));
        client = new HonoApiClient(config, headers, 4);
        return client;
    }

    @Test
    void minecraftのGETにBearerを付ける() {
        newClient(TOKEN, Map.of());
        assertInstanceOf(ApiResult.Success.class, client.get("/api/v1/minecraft/catalog?storeId=s", Item.class).join());
        assertEquals(List.of(List.of("Bearer " + TOKEN)), authorizations);
    }

    @Test
    void minecraftのPOSTにBearerを付ける() {
        newClient(TOKEN, Map.of());
        assertInstanceOf(PostOutcome.Success.class,
                client.post("/api/v1/minecraft/purchases", "req-1", new Body("n"), Item.class).join());
        assertEquals(List.of(List.of("Bearer " + TOKEN)), authorizations);
    }

    @Test
    void minecraft以外のpathにはtokenを送らない() {
        newClient(TOKEN, Map.of());
        client.get("/api/v1/products/p1", Item.class).join();
        client.get("/api/v1/minecraftx/catalog", Item.class).join();
        assertEquals(List.of(List.of(), List.of()), authorizations);
    }

    @Test
    void 共通headerのAuthorizationで上書きされない() {
        newClient(TOKEN, Map.of("Authorization", "Bearer other"));
        client.get("/api/v1/minecraft/catalog", Item.class).join();
        assertEquals(List.of(List.of("Bearer " + TOKEN)), authorizations);
    }

    @Test
    void token未設定ならminecraftは認証なしで送らない() {
        newClient("", Map.of());
        var get = assertInstanceOf(ApiResult.Failure.class, client.get("/api/v1/minecraft/catalog", Item.class).join());
        assertEquals(ApiError.Kind.UNAUTHORIZED, get.error().kind());
        var post = assertInstanceOf(PostOutcome.NotSent.class,
                client.post("/api/v1/minecraft/purchases", "req-1", new Body("n"), Item.class).join());
        assertEquals(ApiError.Kind.UNAUTHORIZED, post.reason());
        assertEquals(0, hits.get());
    }

    @Test
    void token未設定でも公開pathは呼べる() {
        newClient("", Map.of());
        assertInstanceOf(ApiResult.Success.class, client.get("/api/v1/products/p1", Item.class).join());
        assertEquals(1, hits.get());
    }
}

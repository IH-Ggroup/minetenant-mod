package com.minetenant.minetenantserver.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minetenant.minetenantserver.config.HonoConfig;
import com.minetenant.minetenantserver.http.HonoApiClient;
import com.minetenant.minetenantserver.network.ProductDetailResultCode;
import com.sun.net.httpserver.HttpServer;

/** JDK標準のHttpServerをHonoの代わりにして、status別の変換を確認する。 */
class HonoProductDetailServiceTest {

    private static final String PRODUCT_JSON = """
            {"data":{"id":"product-stool","storeId":"store-mine","sellerId":"user-seller",
            "name":"Stool","description":"A small stool","price":4200,"status":"%s",
            "category":"interior","theme":"forest","emoji":"wood","createdAt":"2026-07-17T04:30:00.000000Z"}}
            """;

    private HttpServer server;
    private HonoApiClient client;
    private HonoProductDetailService service;
    private final List<String> requestedPaths = new CopyOnWriteArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        HonoConfig config = new HonoConfig(URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                "secret-token", Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofSeconds(3));
        client = new HonoApiClient(config, Map.of(), 4);
        service = new HonoProductDetailService(client);
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.stop(0);
    }

    private void respond(String path, int status, String body) {
        server.createContext(path, ex -> {
            requestedPaths.add(ex.getRequestURI().getPath());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                ex.getResponseBody().write(bytes);
            }
            ex.close();
        });
    }

    private ProductDetailResult fetch(String productId) {
        return service.fetchProduct(productId).join();
    }

    @Test
    void existingProductIsReturned() {
        respond("/api/v1/products/product-stool", 200, PRODUCT_JSON.formatted("available"));

        ProductDetailResult result = fetch("product-stool");

        assertTrue(result.isSuccess());
        assertEquals(new ProductDetail("product-stool", "store-mine", "Stool", "A small stool", 4200,
                ProductStatus.AVAILABLE, "interior", "forest", "wood"), result.detail());
        assertEquals(List.of("/api/v1/products/product-stool"), requestedPaths);
    }

    @Test
    void soldStatusIsReturnedAsSuccess() {
        respond("/api/v1/products/product-stool", 200, PRODUCT_JSON.formatted("sold"));

        ProductDetailResult result = fetch("product-stool");

        assertTrue(result.isSuccess());
        assertEquals(ProductStatus.SOLD, result.detail().status());
    }

    @Test
    void statusChangeIsReflectedOnNextFetch() {
        AtomicReference<String> status = new AtomicReference<>("available");
        server.createContext("/api/v1/products/product-stool", ex -> {
            byte[] bytes = PRODUCT_JSON.formatted(status.get()).getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });

        assertEquals(ProductStatus.AVAILABLE, fetch("product-stool").detail().status());
        status.set("sold");
        assertEquals(ProductStatus.SOLD, fetch("product-stool").detail().status());
    }

    @Test
    void notFoundIsDistinguished() {
        respond("/api/v1/products/gone", 404, "{\"message\":\"not found\"}");

        ProductDetailResult result = fetch("gone");

        assertEquals(ProductDetailResultCode.PRODUCT_NOT_FOUND, result.code());
        assertNull(result.detail());
    }

    @Test
    void otherFailuresAreDistinguished() {
        respond("/api/v1/products/unauth", 401, "{}");
        respond("/api/v1/products/broken", 500, "{}");
        respond("/api/v1/products/invalid", 422, "{}");

        assertEquals(ProductDetailResultCode.UNAUTHORIZED, fetch("unauth").code());
        assertEquals(ProductDetailResultCode.SERVER_ERROR, fetch("broken").code());
        assertEquals(ProductDetailResultCode.UNKNOWN, fetch("invalid").code());
    }

    @Test
    void timeoutIsDistinguished() throws IOException {
        server.createContext("/api/v1/products/slow", ex -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            ex.close();
        });
        HonoConfig config = new HonoConfig(URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                "secret-token", Duration.ofSeconds(2), Duration.ofMillis(200), Duration.ofSeconds(3));
        try (HonoApiClient shortTimeout = new HonoApiClient(config, Map.of(), 4)) {
            assertEquals(ProductDetailResultCode.TIMEOUT,
                    new HonoProductDetailService(shortTimeout).fetchProduct("slow").join().code());
        }
    }

    @Test
    void contractViolationsBecomeUnknown() {
        respond("/api/v1/products/unknown-status", 200, PRODUCT_JSON.formatted("reserved"));
        respond("/api/v1/products/no-data", 200, "{\"data\":null}");
        respond("/api/v1/products/not-json", 200, "oops");
        respond("/api/v1/products/no-price", 200,
                "{\"data\":{\"id\":\"no-price\",\"storeId\":\"s\",\"name\":\"n\",\"description\":\"d\","
                        + "\"status\":\"available\",\"category\":\"c\",\"theme\":\"t\",\"emoji\":\"e\"}}");
        respond("/api/v1/products/negative", 200,
                "{\"data\":{\"id\":\"negative\",\"storeId\":\"s\",\"name\":\"n\",\"description\":\"d\",\"price\":-1,"
                        + "\"status\":\"available\",\"category\":\"c\",\"theme\":\"t\",\"emoji\":\"e\"}}");

        for (String id : new String[] {"unknown-status", "no-data", "not-json", "no-price", "negative"}) {
            assertEquals(ProductDetailResultCode.UNKNOWN, fetch(id).code(), id);
        }
    }

    @Test
    void invalidIdIsNotSentToHono() {
        respond("/", 200, PRODUCT_JSON.formatted("available"));

        for (String bad : new String[] {null, "", "../admin", "a/b", "a?x=1", "a b"}) {
            assertEquals(ProductDetailResultCode.INVALID_REQUEST, fetch(bad).code(), "productId=" + bad);
        }
        assertTrue(requestedPaths.isEmpty());
    }
}

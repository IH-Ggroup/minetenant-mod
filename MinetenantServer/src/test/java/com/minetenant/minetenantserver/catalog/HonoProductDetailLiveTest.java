package com.minetenant.minetenantserver.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.minetenant.minetenantserver.config.HonoConfig;
import com.minetenant.minetenantserver.http.HonoApiClient;
import com.minetenant.minetenantserver.network.ProductDetailResultCode;

/**
 * 実際に起動しているBackendへ接続する手動確認用のテスト。通常は実行しない。
 * 実行例(PowerShell): $env:MINETENANT_LIVE_HONO_URL="http://localhost:8787"; ./gradlew test --tests "*HonoProductDetailLiveTest"
 * Backendのseed商品(product-hoodie=available、product-stool=sold)があることを前提にする。
 */
@EnabledIfEnvironmentVariable(named = "MINETENANT_LIVE_HONO_URL", matches = ".+")
class HonoProductDetailLiveTest {

    private HonoApiClient client;
    private HonoProductDetailService service;

    @BeforeEach
    void connect() {
        HonoConfig config = new HonoConfig(URI.create(System.getenv("MINETENANT_LIVE_HONO_URL")), "",
                Duration.ofSeconds(3), Duration.ofSeconds(5), Duration.ofSeconds(3));
        client = new HonoApiClient(config, Map.of(), 4);
        service = new HonoProductDetailService(client);
    }

    @AfterEach
    void close() {
        client.close();
    }

    @Test
    void availableProductIsReturnedWithJapaneseTextIntact() {
        ProductDetailResult result = service.fetchProduct("product-hoodie").join();

        assertEquals(ProductDetailResultCode.SUCCESS, result.code());
        ProductDetail detail = result.detail();
        assertEquals("product-hoodie", detail.id());
        assertEquals("store-mine", detail.storeId());
        assertEquals("コバルトブルーのパーカー", detail.name());
        assertEquals(6800, detail.price());
        assertEquals(ProductStatus.AVAILABLE, detail.status());
        assertEquals("fashion", detail.category());
        assertEquals("ocean", detail.theme());
        assertEquals("🧥", detail.emoji());
        assertTrue(detail.description().startsWith("深い青色"));
        // packetに載せる処理を通っても欠けない
        assertEquals(detail, ProductDetailPacketLimits.fit(detail));
    }

    @Test
    void soldProductIsReturnedAsSuccess() {
        ProductDetailResult result = service.fetchProduct("product-stool").join();

        assertEquals(ProductDetailResultCode.SUCCESS, result.code());
        assertEquals(ProductStatus.SOLD, result.detail().status());
    }

    @Test
    void missingProductIsNotFound() {
        ProductDetailResult result = service.fetchProduct("no-such-product").join();

        assertEquals(ProductDetailResultCode.PRODUCT_NOT_FOUND, result.code());
        assertNull(result.detail());
    }

    @Test
    void everySeedProductMatchesTheContract() {
        for (String id : new String[] {"product-hoodie", "product-stool", "product-notebook", "product-pendant",
                "product-toolbag", "product-lamp"}) {
            assertEquals(ProductDetailResultCode.SUCCESS, service.fetchProduct(id).join().code(), id);
        }
    }
}

package com.minetenant.minetenantserver.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.minetenant.minetenantserver.network.ProductDetailResultCode;

class ProductDetailRequestProcessorTest {

    private final UUID player = UUID.randomUUID();

    private static ProductDetail detail(String id, ProductStatus status) {
        return new ProductDetail(id, "store-a", "Name " + id, "Desc", 1000, status, "fashion", "ocean", "e");
    }

    private static ProductDetailRequestProcessor processor(ProductDetailService service) {
        return new ProductDetailRequestProcessor(service);
    }

    @Test
    void passesProductIdAndReturnsDetail() {
        ProductDetailService service = id -> CompletableFuture.completedFuture(
                ProductDetailResult.success(detail(id, ProductStatus.AVAILABLE)));

        ProductDetailResult result = processor(service).process(player, "product-1").join();

        assertTrue(result.isSuccess());
        assertEquals(detail("product-1", ProductStatus.AVAILABLE), result.detail());
    }

    @Test
    void notFoundIsDistinguishedFromOtherFailures() {
        ProductDetailService service = id -> CompletableFuture.completedFuture(
                ProductDetailResult.failure(ProductDetailResultCode.PRODUCT_NOT_FOUND));

        assertEquals(ProductDetailResultCode.PRODUCT_NOT_FOUND, processor(service).process(player, "gone").join().code());
    }

    @Test
    void latestStatusIsReturnedOnEveryRequest() {
        AtomicInteger calls = new AtomicInteger();
        ProductDetailService service = id -> CompletableFuture.completedFuture(ProductDetailResult.success(
                detail(id, calls.incrementAndGet() == 1 ? ProductStatus.AVAILABLE : ProductStatus.SOLD)));
        ProductDetailRequestProcessor processor = processor(service);

        assertEquals(ProductStatus.AVAILABLE, processor.process(player, "p1").join().detail().status());
        assertEquals(ProductStatus.SOLD, processor.process(player, "p1").join().detail().status());
        assertEquals(2, calls.get());
    }

    @Test
    void invalidInputIsRejectedWithoutCallingService() {
        AtomicInteger calls = new AtomicInteger();
        ProductDetailService service = id -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(ProductDetailResult.success(detail(id, ProductStatus.AVAILABLE)));
        };
        ProductDetailRequestProcessor processor = processor(service);

        String tooLong = "a".repeat(ProductDetailRequestProcessor.MAX_PRODUCT_ID_LENGTH + 1);
        for (String bad : new String[] {null, "", " ", "a b", "../x", "a/b", "a?b=1", "a#b", "a%2Fb", "a\n", tooLong}) {
            assertEquals(ProductDetailResultCode.INVALID_REQUEST, processor.process(player, bad).join().code(),
                    "productId=" + bad);
        }
        assertEquals(ProductDetailResultCode.INVALID_REQUEST, processor.process(null, "p1").join().code());
        assertEquals(0, calls.get());
    }

    @Test
    void maxLengthIdIsAccepted() {
        ProductDetailService service = id -> CompletableFuture.completedFuture(
                ProductDetailResult.success(detail(id, ProductStatus.AVAILABLE)));
        String id = "a".repeat(ProductDetailRequestProcessor.MAX_PRODUCT_ID_LENGTH);

        assertTrue(processor(service).process(player, id).join().isSuccess());
        assertTrue(ProductDetailRequestProcessor.isValidProductId("0f8fad5b-d9cb-469f-a165-70867728950e"));
        assertFalse(ProductDetailRequestProcessor.isValidProductId(null));
    }

    @Test
    void differentProductFromServiceIsRejected() {
        ProductDetailService service = id -> CompletableFuture.completedFuture(
                ProductDetailResult.success(detail("other", ProductStatus.AVAILABLE)));

        ProductDetailResult result = processor(service).process(player, "p1").join();

        assertEquals(ProductDetailResultCode.UNKNOWN, result.code());
        assertNull(result.detail());
    }

    @Test
    void serviceProblemsBecomeUnknown() {
        assertEquals(ProductDetailResultCode.UNKNOWN, processor(id -> null).process(player, "p1").join().code());
        assertEquals(ProductDetailResultCode.UNKNOWN,
                processor(id -> CompletableFuture.completedFuture(null)).process(player, "p1").join().code());
        assertEquals(ProductDetailResultCode.UNKNOWN,
                processor(id -> CompletableFuture.failedFuture(new IllegalStateException("boom")))
                        .process(player, "p1").join().code());
        assertEquals(ProductDetailResultCode.UNKNOWN, processor(id -> {
            throw new IllegalStateException("boom");
        }).process(player, "p1").join().code());
    }

    @Test
    void concurrentRequestsDoNotMixResults() {
        CompletableFuture<ProductDetailResult> slowA = new CompletableFuture<>();
        CompletableFuture<ProductDetailResult> slowB = new CompletableFuture<>();
        ProductDetailService service = id -> id.equals("a") ? slowA : slowB;
        ProductDetailRequestProcessor processor = processor(service);

        CompletableFuture<ProductDetailResult> forA = processor.process(player, "a");
        CompletableFuture<ProductDetailResult> forB = processor.process(player, "b");
        slowB.complete(ProductDetailResult.success(detail("b", ProductStatus.SOLD)));
        slowA.complete(ProductDetailResult.success(detail("a", ProductStatus.AVAILABLE)));

        assertEquals("a", forA.join().detail().id());
        assertEquals("b", forB.join().detail().id());
    }
}

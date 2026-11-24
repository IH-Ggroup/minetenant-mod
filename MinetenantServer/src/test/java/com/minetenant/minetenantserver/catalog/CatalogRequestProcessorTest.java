package com.minetenant.minetenantserver.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.minetenant.minetenantserver.http.ApiError;
import com.minetenant.minetenantserver.http.ApiResult;
import com.minetenant.minetenantserver.network.CatalogResultCode;

class CatalogRequestProcessorTest {

    private static final ProductSummary A1 = new ProductSummary("a1", "store-a", "A shirt", 1000, ProductStatus.AVAILABLE);
    private static final ProductSummary B1 = new ProductSummary("b1", "store-b", "B bag", 2000, ProductStatus.AVAILABLE);

    private final UUID player = UUID.randomUUID();

    private static CatalogRequestProcessor processor(CatalogService service) {
        return new CatalogRequestProcessor(service, id -> true);
    }

    private static CatalogResult done(CompletableFuture<CatalogResult> future) {
        return future.join();
    }

    @Test
    void passesStoreIdAndReturnsProducts() {
        CatalogService service = id -> CompletableFuture.completedFuture(
                CatalogResult.success(id.equals("store-a") ? List.of(A1) : List.of(B1)));

        assertEquals(List.of(A1), done(processor(service).process(player, "store-a")).products());
        assertEquals(List.of(B1), done(processor(service).process(player, "store-b")).products());
    }

    @Test
    void emptyListIsSuccess() {
        CatalogService service = id -> CompletableFuture.completedFuture(CatalogResult.success(List.of()));

        CatalogResult result = done(processor(service).process(player, "store-a"));

        assertEquals(CatalogResultCode.SUCCESS, result.code());
        assertTrue(result.products().isEmpty());
    }

    @Test
    void invalidInputIsRejectedWithoutCallingService() {
        AtomicInteger calls = new AtomicInteger();
        CatalogService service = id -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(CatalogResult.success(List.of()));
        };
        CatalogRequestProcessor processor = processor(service);

        String tooLong = "a".repeat(CatalogRequestProcessor.MAX_STORE_ID_LENGTH + 1);
        for (String bad : new String[] {null, "", " ", "a b", "../x", "a/b", "a?b=1", "a#b", "a\n", tooLong}) {
            assertEquals(CatalogResultCode.INVALID_REQUEST, done(processor.process(player, bad)).code(), "storeId=" + bad);
        }
        assertEquals(CatalogResultCode.INVALID_REQUEST, done(processor.process(null, "store-a")).code());
        assertEquals(0, calls.get());
    }

    @Test
    void unregisteredStoreIsRejectedWithoutCallingService() {
        AtomicInteger calls = new AtomicInteger();
        CatalogService service = id -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(CatalogResult.success(List.of()));
        };
        CatalogRequestProcessor processor = new CatalogRequestProcessor(service, id -> id.equals("store-a"));

        assertEquals(CatalogResultCode.INVALID_REQUEST, done(processor.process(player, "store-b")).code());
        assertEquals(0, calls.get());
        assertEquals(CatalogResultCode.SUCCESS, done(processor.process(player, "store-a")).code());
        assertEquals(1, calls.get());
    }

    @Test
    void registryFailureIsRejected() {
        CatalogService service = id -> CompletableFuture.completedFuture(CatalogResult.success(List.of()));
        CatalogRequestProcessor processor = new CatalogRequestProcessor(service, id -> {
            throw new IllegalStateException("registry down");
        });

        assertEquals(CatalogResultCode.INVALID_REQUEST, done(processor.process(player, "store-a")).code());
    }

    @Test
    void serviceFailuresBecomeUnknown() {
        CatalogService throwing = id -> {
            throw new IllegalStateException("boom");
        };
        CatalogService failedFuture = id -> CompletableFuture.failedFuture(new IllegalStateException("boom"));
        CatalogService nullFuture = id -> null;
        CatalogService nullResult = id -> CompletableFuture.completedFuture(null);

        for (CatalogService service : List.of(throwing, failedFuture, nullFuture, nullResult)) {
            assertEquals(CatalogResultCode.UNKNOWN, done(processor(service).process(player, "store-a")).code());
        }
    }

    @Test
    void apiErrorsAreDistinguished() {
        assertEquals(CatalogResultCode.UNAUTHORIZED, map(ApiError.Kind.UNAUTHORIZED));
        assertEquals(CatalogResultCode.STORE_NOT_FOUND, map(ApiError.Kind.NOT_FOUND));
        assertEquals(CatalogResultCode.SERVER_ERROR, map(ApiError.Kind.SERVER_ERROR));
        assertEquals(CatalogResultCode.TIMEOUT, map(ApiError.Kind.TIMEOUT));
        assertEquals(CatalogResultCode.UNKNOWN, map(ApiError.Kind.NETWORK));
        assertEquals(CatalogResultCode.UNKNOWN, map(ApiError.Kind.INVALID_JSON));
        assertEquals(CatalogResultCode.UNKNOWN, map(ApiError.Kind.HTTP_ERROR));
    }

    @Test
    void successMapsToProducts() {
        CatalogResult result = CatalogResultMapper.from(new ApiResult.Success<>(List.of(A1)));

        assertTrue(result.isSuccess());
        assertEquals(List.of(A1), result.products());
    }

    @Test
    void concurrentRequestsDoNotMixResults() {
        CompletableFuture<CatalogResult> pendingA = new CompletableFuture<>();
        CompletableFuture<CatalogResult> pendingB = new CompletableFuture<>();
        CatalogService service = id -> id.equals("store-a") ? pendingA : pendingB;
        CatalogRequestProcessor processor = processor(service);
        UUID otherPlayer = UUID.randomUUID();

        CompletableFuture<CatalogResult> forA = processor.process(player, "store-a");
        CompletableFuture<CatalogResult> forB = processor.process(otherPlayer, "store-b");
        assertFalse(forA.isDone());
        assertFalse(forB.isDone());

        // 後に呼んだ方を先に完了させても、それぞれ自分の結果だけを受け取る
        pendingB.complete(CatalogResult.success(List.of(B1)));
        assertFalse(forA.isDone());
        assertEquals(List.of(B1), forB.join().products());

        pendingA.complete(CatalogResult.failure(CatalogResultCode.TIMEOUT));
        assertEquals(CatalogResultCode.TIMEOUT, forA.join().code());
        assertEquals(CatalogResultCode.SUCCESS, forB.join().code());
    }

    private static CatalogResultCode map(ApiError.Kind kind) {
        return CatalogResultMapper.from(new ApiResult.Failure<List<ProductSummary>>(ApiError.of(kind))).code();
    }
}

package com.minetenant.minetenantserver.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.minetenant.minetenantserver.network.CatalogResultCode;

class FixtureCatalogServiceTest {

    private static final ProductSummary A1 = new ProductSummary("a1", "store-a", "A shirt", 1000, ProductStatus.AVAILABLE);
    private static final ProductSummary A2 = new ProductSummary("a2", "store-a", "A hat", 500, ProductStatus.SOLD);
    private static final ProductSummary B1 = new ProductSummary("b1", "store-b", "B bag", 2000, ProductStatus.AVAILABLE);

    private final FixtureCatalogService service = new FixtureCatalogService(Map.of(
            "store-a", List.of(A1, A2),
            "store-b", List.of(B1),
            "store-empty", List.of()));

    private CatalogResult fetch(String storeId) {
        return service.fetchProducts(storeId).join();
    }

    @Test
    void returnsOnlyProductsOfRequestedStore() {
        CatalogResult result = fetch("store-a");

        assertTrue(result.isSuccess());
        assertEquals(List.of(A1, A2), result.products());
    }

    @Test
    void emptyStoreSucceedsWithEmptyList() {
        CatalogResult result = fetch("store-empty");

        assertEquals(CatalogResultCode.SUCCESS, result.code());
        assertTrue(result.products().isEmpty());
    }

    @Test
    void unknownStoreIsNotFound() {
        CatalogResult result = fetch("store-x");

        assertEquals(CatalogResultCode.STORE_NOT_FOUND, result.code());
        assertTrue(result.products().isEmpty());
    }

    @Test
    void failureResultHasNoProducts() {
        CatalogResult result = new CatalogResult(CatalogResultCode.TIMEOUT, List.of(A1));

        assertTrue(result.products().isEmpty());
    }

    @Test
    void resultListIsImmutable() {
        List<ProductSummary> products = fetch("store-a").products();

        assertThrows(UnsupportedOperationException.class, () -> products.add(B1));
    }

    @Test
    void productRejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductSummary("x", "store-a", "bad", -1, ProductStatus.AVAILABLE));
    }

    @Test
    void unknownStatusIdIsTreatedAsSold() {
        assertEquals(ProductStatus.AVAILABLE, ProductStatus.fromId(0));
        assertEquals(ProductStatus.SOLD, ProductStatus.fromId(1));
        assertEquals(ProductStatus.SOLD, ProductStatus.fromId(99));
    }

    @Test
    void unknownResultCodeIdIsUnknown() {
        assertEquals(CatalogResultCode.UNKNOWN, CatalogResultCode.fromId(99));
        for (CatalogResultCode code : CatalogResultCode.values()) {
            assertEquals(code, CatalogResultCode.fromId(code.id()));
        }
    }
}

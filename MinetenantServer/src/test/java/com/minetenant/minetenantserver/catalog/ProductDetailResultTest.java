package com.minetenant.minetenantserver.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.minetenant.minetenantserver.network.ProductDetailResultCode;

class ProductDetailResultTest {

    private static ProductDetail detail() {
        return new ProductDetail("p1", "store-a", "Shirt", "A shirt", 1000, ProductStatus.AVAILABLE,
                "fashion", "ocean", "shirt");
    }

    @Test
    void successHasDetail() {
        ProductDetailResult result = ProductDetailResult.success(detail());

        assertTrue(result.isSuccess());
        assertEquals(detail(), result.detail());
    }

    @Test
    void failureHasNoDetail() {
        ProductDetailResult result = ProductDetailResult.failure(ProductDetailResultCode.PRODUCT_NOT_FOUND);

        assertFalse(result.isSuccess());
        assertNull(result.detail());
    }

    @Test
    void failureDropsDetailEvenIfGiven() {
        assertNull(new ProductDetailResult(ProductDetailResultCode.UNKNOWN, detail()).detail());
    }

    @Test
    void successWithoutDetailIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductDetailResult(ProductDetailResultCode.SUCCESS, null));
    }

    @Test
    void negativePriceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ProductDetail("p1", "store-a", "Shirt", "d", -1,
                ProductStatus.AVAILABLE, "fashion", "ocean", "shirt"));
    }

    @Test
    void nullFieldIsRejected() {
        assertThrows(NullPointerException.class, () -> new ProductDetail("p1", "store-a", null, "d", 1,
                ProductStatus.SOLD, "fashion", "ocean", "shirt"));
    }
}

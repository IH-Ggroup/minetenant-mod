package com.minetenant.minetenantserver.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** ClientとServerで数値を揃えるため、通信上の数値を固定して確認する。 */
class ProductDetailResultCodeTest {

    @Test
    void idsAreFixed() {
        assertEquals(0, ProductDetailResultCode.SUCCESS.id());
        assertEquals(1, ProductDetailResultCode.INVALID_REQUEST.id());
        assertEquals(2, ProductDetailResultCode.UNAUTHORIZED.id());
        assertEquals(3, ProductDetailResultCode.PRODUCT_NOT_FOUND.id());
        assertEquals(4, ProductDetailResultCode.SERVER_ERROR.id());
        assertEquals(5, ProductDetailResultCode.TIMEOUT.id());
        assertEquals(6, ProductDetailResultCode.UNKNOWN.id());
    }

    @Test
    void unknownIdBecomesUnknown() {
        assertEquals(ProductDetailResultCode.UNKNOWN, ProductDetailResultCode.fromId(-1));
        assertEquals(ProductDetailResultCode.UNKNOWN, ProductDetailResultCode.fromId(999));
    }
}

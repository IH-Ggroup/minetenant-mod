package com.minetenant.minetenantclient.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** ClientとServerで数値を揃えるため、通信上の数値を固定して確認する。 */
class CatalogResultCodeTest {

    @Test
    void idsAreFixed() {
        assertEquals(0, CatalogResultCode.SUCCESS.id());
        assertEquals(1, CatalogResultCode.INVALID_REQUEST.id());
        assertEquals(2, CatalogResultCode.UNAUTHORIZED.id());
        assertEquals(3, CatalogResultCode.STORE_NOT_FOUND.id());
        assertEquals(4, CatalogResultCode.SERVER_ERROR.id());
        assertEquals(5, CatalogResultCode.TIMEOUT.id());
        assertEquals(6, CatalogResultCode.UNKNOWN.id());
    }

    @Test
    void unknownIdBecomesUnknown() {
        assertEquals(CatalogResultCode.UNKNOWN, CatalogResultCode.fromId(-1));
        assertEquals(CatalogResultCode.UNKNOWN, CatalogResultCode.fromId(999));
    }
}

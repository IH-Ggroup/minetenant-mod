package com.minetenant.minetenantclient.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LinkResultCodeTest {

    @Test
    void fromIdRestoresEveryValue() {
        for (LinkResultCode value : LinkResultCode.values()) {
            assertEquals(value, LinkResultCode.fromId(value.id()));
        }
    }

    @Test
    void unknownIdBecomesUnknown() {
        assertEquals(LinkResultCode.UNKNOWN, LinkResultCode.fromId(-1));
        assertEquals(LinkResultCode.UNKNOWN, LinkResultCode.fromId(999));
    }

    @Test
    void idsAreFixed() {
        assertEquals(0, LinkResultCode.SUCCESS.id());
        assertEquals(1, LinkResultCode.CODE_REJECTED.id());
        assertEquals(2, LinkResultCode.UNKNOWN.id());
    }
}

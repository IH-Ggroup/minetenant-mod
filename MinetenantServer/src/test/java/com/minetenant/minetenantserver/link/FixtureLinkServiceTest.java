package com.minetenant.minetenantserver.link;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.minetenant.minetenantserver.network.LinkResultCode;

class FixtureLinkServiceTest {

    private final FixtureLinkService service = new FixtureLinkService();
    private final UUID player = UUID.randomUUID();

    @Test
    void ordinaryCodeSucceeds() {
        assertEquals(LinkResultCode.SUCCESS, service.link(player, "ABC123"));
    }

    @Test
    void failPrefixIsRejected() {
        assertEquals(LinkResultCode.CODE_REJECTED, service.link(player, "FAIL-1"));
        assertEquals(LinkResultCode.CODE_REJECTED, service.link(player, "fail-1"));
    }

    @Test
    void errorPrefixIsUnknown() {
        assertEquals(LinkResultCode.UNKNOWN, service.link(player, "ERROR-1"));
    }
}

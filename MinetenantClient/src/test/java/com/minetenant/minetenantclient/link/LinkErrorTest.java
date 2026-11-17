package com.minetenant.minetenantclient.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class LinkErrorTest {

    @Test
    void codesAreUnique() {
        Set<String> codes = Arrays.stream(LinkError.values()).map(LinkError::code).collect(Collectors.toSet());
        assertEquals(LinkError.values().length, codes.size());
    }

    @Test
    void codesMatchFormat() {
        for (LinkError error : LinkError.values()) {
            assertTrue(error.code().matches("LINK-\\d{3}"), error.name());
        }
    }

    @Test
    void translationKeysAreUnique() {
        Set<String> keys = Arrays.stream(LinkError.values()).map(LinkError::translationKey).collect(Collectors.toSet());
        assertEquals(LinkError.values().length, keys.size());
    }

    /** 付与済みの番号が変わっていないことの確認。変更・再利用はしない。 */
    @Test
    void assignedCodesAreStable() {
        assertEquals("LINK-001", LinkError.EMPTY.code());
        assertEquals("LINK-002", LinkError.INVALID_FORMAT.code());
        assertEquals("LINK-003", LinkError.CODE_REJECTED.code());
        assertEquals("LINK-999", LinkError.UNKNOWN.code());
    }
}

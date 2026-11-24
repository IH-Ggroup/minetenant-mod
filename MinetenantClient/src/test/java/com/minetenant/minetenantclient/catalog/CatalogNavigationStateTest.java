package com.minetenant.minetenantclient.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CatalogNavigationStateTest {

    @Test
    void startsEmpty() {
        CatalogNavigationState nav = new CatalogNavigationState();
        assertNull(nav.selectedProductId());
        assertEquals(0, nav.listScroll());
    }

    @Test
    void remembersSelectionAndScroll() {
        CatalogNavigationState nav = new CatalogNavigationState();
        nav.remember("m-12", 7);

        assertEquals("m-12", nav.selectedProductId());
        assertEquals(7, nav.listScroll());
    }

    @Test
    void negativeScrollIsTreatedAsZero() {
        CatalogNavigationState nav = new CatalogNavigationState();
        nav.remember("m-1", -3);
        assertEquals(0, nav.listScroll());
    }

    @Test
    void clearForgetsPosition() {
        CatalogNavigationState nav = new CatalogNavigationState();
        nav.remember("m-1", 5);
        nav.clear();

        assertNull(nav.selectedProductId());
        assertEquals(0, nav.listScroll());
    }
}

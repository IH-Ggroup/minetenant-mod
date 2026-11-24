package com.minetenant.minetenantserver.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ProductDetailPacketLimitsTest {

    private static ProductDetail detail(String id, String storeId, String name, String description, String emoji) {
        return new ProductDetail(id, storeId, name, description, 100, ProductStatus.AVAILABLE, "fashion", "ocean", emoji);
    }

    @Test
    void withinLimitsIsUnchanged() {
        ProductDetail original = detail("p1", "s1", "Name", "Desc", "e");

        assertEquals(original, ProductDetailPacketLimits.fit(original));
    }

    @Test
    void longTextIsTruncated() {
        ProductDetail fitted = ProductDetailPacketLimits.fit(detail("p1", "s1",
                "n".repeat(ProductDetailPacketLimits.MAX_NAME_LENGTH + 10),
                "d".repeat(ProductDetailPacketLimits.MAX_DESCRIPTION_LENGTH + 10), "e"));

        assertEquals(ProductDetailPacketLimits.MAX_NAME_LENGTH, fitted.name().length());
        assertEquals(ProductDetailPacketLimits.MAX_DESCRIPTION_LENGTH, fitted.description().length());
    }

    @Test
    void truncationDoesNotSplitSurrogatePair() {
        // 絵文字(サロゲートペア)が上限の境界にかかる場合は、ペアごと落とす
        String emoji = "😀";
        String text = "a".repeat(ProductDetailPacketLimits.MAX_NAME_LENGTH - 1) + emoji;

        String cut = ProductDetailPacketLimits.truncate(text, ProductDetailPacketLimits.MAX_NAME_LENGTH);

        assertEquals(ProductDetailPacketLimits.MAX_NAME_LENGTH - 1, cut.length());
        assertEquals(false, Character.isHighSurrogate(cut.charAt(cut.length() - 1)));
    }

    @Test
    void overlongIdsAreNotSent() {
        String longId = "a".repeat(ProductDetailPacketLimits.MAX_ID_LENGTH + 1);

        assertNull(ProductDetailPacketLimits.fit(detail(longId, "s1", "n", "d", "e")));
        assertNull(ProductDetailPacketLimits.fit(detail("p1", longId, "n", "d", "e")));
    }
}

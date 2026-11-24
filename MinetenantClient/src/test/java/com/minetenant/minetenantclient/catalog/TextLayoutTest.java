package com.minetenant.minetenantclient.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TextLayoutTest {

    /** 1文字=1幅として扱う。 */
    private static int width(String s) {
        return s.length();
    }

    @Test
    void shortTextStaysOnOneLine() {
        assertEquals(List.of("abc"), TextLayout.wrap("abc", 10, 5, TextLayoutTest::width));
    }

    @Test
    void longTextIsWrappedByWidth() {
        assertEquals(List.of("abcd", "efgh", "ij"), TextLayout.wrap("abcdefghij", 4, 5, TextLayoutTest::width));
    }

    @Test
    void newlinesStartNewLinesAndBlankLinesAreKept() {
        assertEquals(List.of("ab", "", "cd"), TextLayout.wrap("ab\n\ncd", 10, 5, TextLayoutTest::width));
    }

    @Test
    void overflowingLinesAreClippedWithEllipsis() {
        List<String> lines = TextLayout.wrap("abcdefghijkl", 4, 2, TextLayoutTest::width);

        assertEquals(2, lines.size());
        assertEquals("abcd", lines.get(0));
        assertEquals("e...", lines.get(1));
        assertTrue(lines.stream().allMatch(l -> width(l) <= 4));
    }

    @Test
    void noRoomMeansNoLines() {
        assertTrue(TextLayout.wrap("abc", 0, 5, TextLayoutTest::width).isEmpty());
        assertTrue(TextLayout.wrap("abc", 10, 0, TextLayoutTest::width).isEmpty());
    }

    @Test
    void surrogatePairsAreNotSplit() {
        List<String> lines = TextLayout.wrap("🐻🐻🐻", 2, 5, s -> s.codePointCount(0, s.length()));

        assertEquals(List.of("🐻🐻", "🐻"), lines);
    }
}

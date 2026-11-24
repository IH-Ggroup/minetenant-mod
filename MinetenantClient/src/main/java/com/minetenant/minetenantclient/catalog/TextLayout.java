package com.minetenant.minetenantclient.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * 長い文章を、幅と行数に収まるよう折り返す。幅の測り方は呼び出し側から受け取る(描画に依存させないため)。
 * 行数を超える分は、最後の行の末尾を「...」にして切り捨てる。
 */
public final class TextLayout {

    private static final String ELLIPSIS = "...";

    private TextLayout() {
    }

    public static List<String> wrap(String text, int maxWidth, int maxLines, ToIntFunction<String> widthOf) {
        List<String> lines = new ArrayList<>();

        if (maxWidth <= 0 || maxLines <= 0) {
            return lines;
        }

        for (String paragraph : text.split("\\R", -1)) {
            wrapParagraph(paragraph, maxWidth, widthOf, lines);
        }

        if (lines.size() <= maxLines) {
            return lines;
        }

        List<String> clipped = new ArrayList<>(lines.subList(0, maxLines));
        clipped.set(maxLines - 1, clip(clipped.get(maxLines - 1) + ELLIPSIS, maxWidth, widthOf));
        return clipped;
    }

    /** 1段落を、収まる文字数ごとに区切る。日本語は単語区切りがないので文字単位で折る。 */
    private static void wrapParagraph(String paragraph, int maxWidth, ToIntFunction<String> widthOf,
            List<String> out) {
        if (paragraph.isEmpty()) {
            out.add("");
            return;
        }

        StringBuilder current = new StringBuilder();

        for (int i = 0; i < paragraph.length(); ) {
            int codePoint = paragraph.codePointAt(i);
            String ch = new String(Character.toChars(codePoint));

            if (current.length() > 0 && widthOf.applyAsInt(current + ch) > maxWidth) {
                out.add(current.toString());
                current.setLength(0);
            }

            current.append(ch);
            i += Character.charCount(codePoint);
        }

        out.add(current.toString());
    }

    /** 末尾が「...」のまま幅に収まるよう、手前の文字を削る。 */
    private static String clip(String text, int maxWidth, ToIntFunction<String> widthOf) {
        String body = text.substring(0, text.length() - ELLIPSIS.length());

        while (!body.isEmpty() && widthOf.applyAsInt(body + ELLIPSIS) > maxWidth) {
            body = body.substring(0, body.length() - 1);
        }

        return body + ELLIPSIS;
    }
}

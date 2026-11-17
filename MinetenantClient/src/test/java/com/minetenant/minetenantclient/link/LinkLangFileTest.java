package com.minetenant.minetenantclient.link;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/** LinkError の翻訳キーが、全言語ファイルにあることの確認。 */
class LinkLangFileTest {

    private static String read(String lang) throws IOException {
        String path = "/assets/minetenantclient/lang/" + lang + ".json";
        try (InputStream in = LinkLangFileTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyErrorHasTranslationInAllLanguages() throws IOException {
        for (String lang : new String[] { "en_us", "ja_jp" }) {
            String json = read(lang);

            for (LinkError error : LinkError.values()) {
                assertTrue(json.contains("\"" + error.translationKey() + "\""), lang + ": " + error.translationKey());
            }
        }
    }
}

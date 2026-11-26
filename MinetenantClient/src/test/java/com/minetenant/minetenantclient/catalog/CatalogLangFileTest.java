package com.minetenant.minetenantclient.catalog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/** 商品GUIで使う翻訳キーが、全言語ファイルにあることの確認。 */
class CatalogLangFileTest {

    private static final List<String> KEYS = List.of(
            ProductSummaryViewModel.STATUS_AVAILABLE_KEY,
            ProductSummaryViewModel.STATUS_SOLD_KEY,
            "minetenantclient.gui.button.product_list",
            "minetenantclient.gui.title.product_list",
            "minetenantclient.gui.product_list.loading",
            "minetenantclient.gui.product_list.empty",
            "minetenantclient.gui.product_list.error",
            "minetenantclient.gui.button.retry",
            "minetenantclient.gui.button.close");

    private static String read(String lang) throws IOException {
        String path = "/assets/minetenantclient/lang/" + lang + ".json";
        try (InputStream in = CatalogLangFileTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyKeyExistsInAllLanguages() throws IOException {
        for (String lang : new String[] { "en_us", "ja_jp" }) {
            String json = read(lang);

            for (String key : KEYS) {
                assertTrue(json.contains("\"" + key + "\""), lang + ": " + key);
            }
        }
    }
}

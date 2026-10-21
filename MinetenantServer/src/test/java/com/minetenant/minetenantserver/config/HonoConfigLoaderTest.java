package com.minetenant.minetenantserver.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HonoConfigLoaderTest {

    private static Properties valid() {
        Properties p = new Properties();
        p.setProperty("hono.base-url", "http://localhost:8787");
        p.setProperty("hono.server-token", "secret-token");
        p.setProperty("hono.connect-timeout-seconds", "5");
        p.setProperty("hono.read-timeout-seconds", "10");
        p.setProperty("hono.poll-interval-seconds", "3");
        return p;
    }

    private static String errorOf(Properties p) {
        return assertThrows(HonoConfigException.class, () -> HonoConfigLoader.parse(p)).getMessage();
    }

    @Test
    void 正常な設定を読める() {
        HonoConfig c = HonoConfigLoader.parse(valid());
        assertEquals("http://localhost:8787", c.baseUrl().toString());
        assertEquals("secret-token", c.serverToken());
        assertEquals(Duration.ofSeconds(5), c.connectTimeout());
        assertEquals(Duration.ofSeconds(10), c.readTimeout());
        assertEquals(Duration.ofSeconds(3), c.pollInterval());
        assertTrue(c.hasToken());
    }

    @Test
    void tokenが空でも読める() {
        Properties p = valid();
        p.setProperty("hono.server-token", "");
        assertFalse(HonoConfigLoader.parse(p).hasToken());

        p.remove("hono.server-token");
        assertFalse(HonoConfigLoader.parse(p).hasToken());
    }

    @Test
    void 不足値はキー名つきで拒否する() {
        Properties p = valid();
        p.remove("hono.base-url");
        assertTrue(errorOf(p).contains("hono.base-url"));
    }

    @Test
    void 不正なURLを拒否する() {
        Properties p = valid();
        p.setProperty("hono.base-url", "localhost:8787");
        assertTrue(errorOf(p).contains("hono.base-url"));

        p.setProperty("hono.base-url", "ftp://localhost");
        assertTrue(errorOf(p).contains("hono.base-url"));
    }

    @Test
    void timeoutは正の数のみ() {
        Properties p = valid();
        p.setProperty("hono.connect-timeout-seconds", "0");
        assertTrue(errorOf(p).contains("hono.connect-timeout-seconds"));

        p = valid();
        p.setProperty("hono.read-timeout-seconds", "abc");
        assertTrue(errorOf(p).contains("hono.read-timeout-seconds"));
    }

    @Test
    void poll間隔は2秒以上() {
        Properties p = valid();
        p.setProperty("hono.poll-interval-seconds", "1");
        assertTrue(errorOf(p).contains("hono.poll-interval-seconds"));

        p.setProperty("hono.poll-interval-seconds", "2");
        assertEquals(Duration.ofSeconds(2), HonoConfigLoader.parse(p).pollInterval());

        p.setProperty("hono.poll-interval-seconds", "10");
        assertEquals(Duration.ofSeconds(10), HonoConfigLoader.parse(p).pollInterval());
    }

    @Test
    void 複数のエラーをまとめて報告する() {
        Properties p = valid();
        p.setProperty("hono.base-url", "bad");
        p.setProperty("hono.poll-interval-seconds", "1");
        String msg = errorOf(p);
        assertTrue(msg.contains("hono.base-url"));
        assertTrue(msg.contains("hono.poll-interval-seconds"));
    }

    @Test
    void toStringにtokenを含めない() {
        HonoConfig c = HonoConfigLoader.parse(valid());
        assertFalse(c.toString().contains("secret-token"));
    }

    @Test
    void 設定ファイルが無ければテンプレートを生成して読める(@TempDir Path dir) {
        Path file = dir.resolve("minetenant-server.properties");
        HonoConfig c = HonoConfigLoader.load(file);
        assertTrue(Files.exists(file));
        assertFalse(c.hasToken());
        assertEquals("http://localhost:8787", c.baseUrl().toString());
    }
}

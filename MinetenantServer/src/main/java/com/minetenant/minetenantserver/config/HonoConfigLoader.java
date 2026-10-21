package com.minetenant.minetenantserver.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class HonoConfigLoader {

    public static final String FILE_NAME = "minetenant-server.properties";

    static final String KEY_BASE_URL = "hono.base-url";
    static final String KEY_TOKEN = "hono.server-token";
    static final String KEY_CONNECT_TIMEOUT = "hono.connect-timeout-seconds";
    static final String KEY_READ_TIMEOUT = "hono.read-timeout-seconds";
    static final String KEY_POLL_INTERVAL = "hono.poll-interval-seconds";

    private static final long MIN_POLL_SECONDS = 2;

    private static final String TEMPLATE = """
            # MineTenant Server 設定（このファイルはgitに入れないこと）
            # Honoのbase URL。endpointは含めない
            hono.base-url=http://localhost:8787
            # Server token。発行後に設定する（未設定でも起動はできる）
            hono.server-token=
            # 接続タイムアウト（秒、1以上）
            hono.connect-timeout-seconds=5
            # 読み取りタイムアウト（秒、1以上）
            hono.read-timeout-seconds=10
            # RESTのpoll間隔（秒、2以上。推奨2〜3）
            hono.poll-interval-seconds=3
            """;

    private HonoConfigLoader() {
    }

    /** ファイルから読む。無ければテンプレートを生成して、その内容で読む。 */
    public static HonoConfig load(Path file) {
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, TEMPLATE, StandardCharsets.UTF_8);
            }
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                props.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            }
            return parse(props);
        } catch (IOException e) {
            throw new HonoConfigException(FILE_NAME + " の読み書きに失敗しました: " + e.getMessage(), e);
        }
    }

    /** 検証して設定を作る。エラーは全項目分をまとめて報告する。 */
    public static HonoConfig parse(Properties props) {
        List<String> errors = new ArrayList<>();

        URI baseUrl = parseUrl(props, errors);
        String token = props.getProperty(KEY_TOKEN, "").trim();
        Duration connect = parseSeconds(props, KEY_CONNECT_TIMEOUT, 1, errors);
        Duration read = parseSeconds(props, KEY_READ_TIMEOUT, 1, errors);
        Duration poll = parseSeconds(props, KEY_POLL_INTERVAL, MIN_POLL_SECONDS, errors);

        if (!errors.isEmpty()) {
            throw new HonoConfigException(FILE_NAME + " に問題があります:\n - " + String.join("\n - ", errors));
        }
        return new HonoConfig(baseUrl, token, connect, read, poll);
    }

    private static URI parseUrl(Properties props, List<String> errors) {
        String raw = props.getProperty(KEY_BASE_URL);
        if (raw == null || raw.isBlank()) {
            errors.add(KEY_BASE_URL + " が未設定です");
            return null;
        }
        try {
            URI uri = URI.create(raw.trim());
            String scheme = uri.getScheme();
            boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            if (!httpScheme || uri.getHost() == null) {
                errors.add(KEY_BASE_URL + " は http:// か https:// で始まるURLにしてください: " + raw);
                return null;
            }
            return uri;
        } catch (IllegalArgumentException e) {
            errors.add(KEY_BASE_URL + " がURLとして不正です: " + raw);
            return null;
        }
    }

    private static Duration parseSeconds(Properties props, String key, long min, List<String> errors) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            errors.add(key + " が未設定です");
            return null;
        }
        try {
            long seconds = Long.parseLong(raw.trim());
            if (seconds < min) {
                errors.add(key + " は" + min + "秒以上にしてください: " + raw);
                return null;
            }
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException e) {
            errors.add(key + " が数値ではありません: " + raw);
            return null;
        }
    }
}

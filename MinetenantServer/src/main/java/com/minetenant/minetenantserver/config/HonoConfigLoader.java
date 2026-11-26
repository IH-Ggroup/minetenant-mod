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
import java.util.function.UnaryOperator;

public final class HonoConfigLoader {

    public static final String FILE_NAME = "minetenant-server.properties";

    static final String KEY_BASE_URL = "hono.base-url";
    /** Fabric Dedicated Serverの資格情報を読む環境変数名(api.mdの「Fabric Dedicated Server認証」)。 */
    public static final String ENV_TOKEN = "MINETENANT_FABRIC_SERVER_TOKEN";
    static final String KEY_CONNECT_TIMEOUT = "hono.connect-timeout-seconds";
    static final String KEY_READ_TIMEOUT = "hono.read-timeout-seconds";
    static final String KEY_POLL_INTERVAL = "hono.poll-interval-seconds";

    private static final long MIN_POLL_SECONDS = 2;
    private static final int MIN_TOKEN_LENGTH = 64;
    private static final int MAX_TOKEN_LENGTH = 256;

    private static final String TEMPLATE = """
            # MineTenant Server 設定
            # Honoのbase URL。endpointは含めない
            hono.base-url=http://localhost:8787
            # Server tokenはこのファイルに書かず、環境変数MINETENANT_FABRIC_SERVER_TOKENで渡す
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
            return parse(props, System::getenv);
        } catch (IOException e) {
            throw new HonoConfigException("Failed to read or write " + FILE_NAME + ": " + e.getMessage(), e);
        }
    }

    /** 検証して設定を作る。エラーは全項目分をまとめて報告する。 */
    public static HonoConfig parse(Properties props, UnaryOperator<String> env) {
        List<String> errors = new ArrayList<>();

        URI baseUrl = parseUrl(props, errors);
        String token = parseToken(env, errors);
        Duration connect = parseSeconds(props, KEY_CONNECT_TIMEOUT, 1, errors);
        Duration read = parseSeconds(props, KEY_READ_TIMEOUT, 1, errors);
        Duration poll = parseSeconds(props, KEY_POLL_INTERVAL, MIN_POLL_SECONDS, errors);

        if (!errors.isEmpty()) {
            throw new HonoConfigException("Invalid " + FILE_NAME + ":\n - " + String.join("\n - ", errors));
        }
        return new HonoConfig(baseUrl, token, connect, read, poll);
    }

    /** 未設定は空文字(Minecraft routeを呼ばない)。設定済みなら形式を検証し、trimしない。値はメッセージに出さない。 */
    private static String parseToken(UnaryOperator<String> env, List<String> errors) {
        String raw = env.apply(ENV_TOKEN);
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        if (!isValidToken(raw)) {
            errors.add(ENV_TOKEN + " must be " + MIN_TOKEN_LENGTH + " to " + MAX_TOKEN_LENGTH
                    + " printable ASCII characters without whitespace");
            return "";
        }
        return raw;
    }

    private static boolean isValidToken(String token) {
        if (token.length() < MIN_TOKEN_LENGTH || token.length() > MAX_TOKEN_LENGTH) {
            return false;
        }
        return token.chars().allMatch(c -> c >= 0x21 && c <= 0x7e);
    }

    private static URI parseUrl(Properties props, List<String> errors) {
        String raw = props.getProperty(KEY_BASE_URL);
        if (raw == null || raw.isBlank()) {
            errors.add(KEY_BASE_URL + " is not set");
            return null;
        }
        try {
            URI uri = URI.create(raw.trim());
            String scheme = uri.getScheme();
            boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            if (!httpScheme || uri.getHost() == null) {
                errors.add(KEY_BASE_URL + " must be a URL starting with http:// or https://: " + raw);
                return null;
            }
            return uri;
        } catch (IllegalArgumentException e) {
            errors.add(KEY_BASE_URL + " is not a valid URL: " + raw);
            return null;
        }
    }

    private static Duration parseSeconds(Properties props, String key, long min, List<String> errors) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            errors.add(key + " is not set");
            return null;
        }
        try {
            long seconds = Long.parseLong(raw.trim());
            if (seconds < min) {
                errors.add(key + " must be at least " + min + " seconds: " + raw);
                return null;
            }
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException e) {
            errors.add(key + " is not a number: " + raw);
            return null;
        }
    }
}

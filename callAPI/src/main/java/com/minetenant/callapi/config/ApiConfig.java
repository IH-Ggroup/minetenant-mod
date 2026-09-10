package com.minetenant.callapi.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ApiConfig {

    private static final String FILE_NAME = "callApi.conf";
    private static final String KEY_API_URL = "api.url";
    private static final String DEFAULT_API_URL = "http://localhost:8787/api/hello";

    private static String apiUrl = DEFAULT_API_URL;

    private ApiConfig() {
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties props = new Properties();

        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("[callapi] " + FILE_NAME + " の読み込みに失敗しました。デフォルト値を使用します: " + e.getMessage());
            }
            apiUrl = props.getProperty(KEY_API_URL, DEFAULT_API_URL).trim();
        } else {
            apiUrl = DEFAULT_API_URL;
            props.setProperty(KEY_API_URL, DEFAULT_API_URL);
            try {
                Files.createDirectories(path.getParent());
                try (OutputStream out = Files.newOutputStream(path)) {
                    props.store(out, "callapi settings - API接続先を api.url で指定");
                }
            } catch (IOException e) {
                System.err.println("[callapi] " + FILE_NAME + " の作成に失敗しました: " + e.getMessage());
            }
        }
    }

    public static String getApiUrl() {
        return apiUrl;
    }
}

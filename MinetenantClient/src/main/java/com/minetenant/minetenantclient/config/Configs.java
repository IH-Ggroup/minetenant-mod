package com.minetenant.minetenantclient.config;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigBase;

import java.util.List;

public class Configs {
    public static class Generic {
        public static final ConfigBoolean ENABLE_FEATURE = new ConfigBoolean("enableFeature", true, "機能の有効化");
        public static final ConfigInteger SPEED = new ConfigInteger("speed", 10, 1, 100, "速度設定");

        public static final List<ConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_FEATURE,
                SPEED
        );
    }
    // --- Keybind（ホットキー）の定義クラスを追加 ---
    public static class Hotkeys {
        // 第1引数: 名前, 第2引数: デフォルトのキー割り当て (例: "H" キー), 第3引数: 説明
        public static final ConfigHotkey OPEN_GUI = new ConfigHotkey("openGui", "M", "GUI設定画面を開く");

        // ホットキー一覧のリスト
        public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
                OPEN_GUI
        );
    }
}

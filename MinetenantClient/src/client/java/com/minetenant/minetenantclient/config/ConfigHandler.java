package com.minetenant.minetenantclient.config;

import java.io.File;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.util.JsonUtils;

import com.minetenant.minetenantclient.Reference;

/**
 * 設定値を config/minetenantclient.json に保存・読み込みする。
 */
public class ConfigHandler implements IConfigHandler {

    public static final ConfigHandler INSTANCE = new ConfigHandler();

    private static final String FILE_NAME = Reference.MOD_ID + ".json";
    private static final String GENERIC_KEY = "Generic";
    private static final String HOTKEYS_KEY = "Hotkeys";

    private ConfigHandler() {
    }

    @Override
    public void load() {
        File file = getFile();

        if (!file.isFile()) {
            return;
        }

        JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject()) {
            JsonObject root = element.getAsJsonObject();
            ConfigUtils.readConfigBase(root, GENERIC_KEY, Configs.Generic.OPTIONS);
            ConfigUtils.readHotkeys(root, HOTKEYS_KEY, Configs.Hotkeys.HOTKEY_LIST);
        }
    }

    @Override
    public void save() {
        File file = getFile();
        File dir = file.getParentFile();

        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, GENERIC_KEY, Configs.Generic.OPTIONS);
            ConfigUtils.writeHotkeys(root, HOTKEYS_KEY, Configs.Hotkeys.HOTKEY_LIST);
            JsonUtils.writeJsonToFile(root, file);
        }
    }

    /** 設定画面を閉じたときなど、設定が変更されたらその場で保存する。 */
    @Override
    public void onConfigsChanged() {
        this.save();
    }

    private static File getFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME).toFile();
    }
}

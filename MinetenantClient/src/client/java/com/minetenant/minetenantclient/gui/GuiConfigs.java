package com.minetenant.minetenantclient.gui;

import java.util.List;

import net.minecraft.client.gui.screen.Screen;

import fi.dy.masa.malilib.gui.GuiConfigsBase;

import com.minetenant.minetenantclient.Reference;
import com.minetenant.minetenantclient.config.Configs;

/**
 * 一般設定・キー割り当ての設定画面。
 * malilibのMOD切り替えプルダウンは「登録した画面クラスと同じクラスか」で表示を判定するため、
 * タブごとにクラスを分けず、1つのクラスでタブを切り替える。
 */
public class GuiConfigs extends GuiConfigsBase {

    public enum Tab {
        GENERIC("minetenantclient.gui.title.configs.generic"),
        HOTKEYS("minetenantclient.gui.title.configs.hotkeys");

        private final String titleKey;

        Tab(String titleKey) {
            this.titleKey = titleKey;
        }
    }

    private final Tab tab;

    public GuiConfigs(Screen parent, Tab tab) {
        super(10, 50, Reference.MOD_ID, parent, tab.titleKey);

        this.tab = tab;
        this.setParent(parent);
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return switch (this.tab) {
            case GENERIC -> ConfigOptionWrapper.createFor(Configs.Generic.OPTIONS);
            case HOTKEYS -> ConfigOptionWrapper.createFor(Configs.Hotkeys.HOTKEY_LIST);
        };
    }
}

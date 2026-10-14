package com.minetenant.minetenantclient.gui;

import java.util.List;

import net.minecraft.client.gui.screen.Screen;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;

import com.minetenant.minetenantclient.Reference;
import com.minetenant.minetenantclient.config.Configs;

/**
 * 一般設定・キー設定の設定画面。上部のタブボタンで同じ画面内を切り替える。
 * malilibのMOD切り替えプルダウンは「登録した画面クラスと同じクラスか」で表示を判定するため、
 * タブごとにクラスを分けず、1つのクラスでタブを切り替える。
 */
public class GuiConfigs extends GuiConfigsBase {

    private static final int TAB_X = 10;
    private static final int TAB_Y = 26;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_SPACING = 2;

    public enum Tab {
        GENERIC("minetenantclient.gui.tab.generic"),
        HOTKEYS("minetenantclient.gui.tab.hotkeys");

        private final String labelKey;

        Tab(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    private Tab tab;

    public GuiConfigs(Screen parent, Tab tab) {
        super(10, 50, Reference.MOD_ID, parent, "minetenantclient.gui.title.configs");

        this.tab = tab;
        this.setParent(parent);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.clearOptions();

        int x = TAB_X;
        for (Tab t : Tab.values()) {
            String label = StringUtils.translate(t.labelKey);
            ButtonGeneric button = new ButtonGeneric(x, TAB_Y, this.getStringWidth(label) + 10, TAB_HEIGHT, label);
            button.setEnabled(this.tab != t);
            this.addButton(button, (btn, mouseButton) -> this.switchTab(t));
            x += button.getWidth() + TAB_SPACING;
        }
    }

    private void switchTab(Tab newTab) {
        this.tab = newTab;
        this.reCreateListWidget();
        this.getListWidget().resetScrollbarPosition();
        this.initGui();
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return switch (this.tab) {
            case GENERIC -> ConfigOptionWrapper.createFor(Configs.Generic.OPTIONS);
            case HOTKEYS -> ConfigOptionWrapper.createFor(Configs.Hotkeys.HOTKEY_LIST);
        };
    }
}

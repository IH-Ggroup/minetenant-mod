package com.minetenant.minetenantclient.gui;

import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;

import com.minetenant.minetenantclient.Reference;

/**
 * Litematica のメインメニューのように、開いた直後は設定画面ではなく
 * 各機能へのボタンが並んだ入り口の画面。
 */
public class GuiMinetenantMain extends BaseScreen {

    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MARGIN = 4;

    public GuiMinetenantMain() {
        this.setTitle(Reference.MOD_NAME);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.clearElements();

        int x = this.getScreenWidth() / 2 - BUTTON_WIDTH / 2;
        int y = this.getScreenHeight() / 2 - (BUTTON_HEIGHT * 2 + BUTTON_MARGIN * 3) / 2;

        y = this.createMenuButton(x, y, "一般設定", () -> new GuiConfigsGeneric(this));
        y = this.createMenuButton(x, y, "キー割り当て", () -> new GuiConfigsHotkeys(this));

        // ここに今後のメニュー項目（Litematica でいう "Load Schematics" や
        // "Save Schematics" のようなもの）を同じ要領で追加していく。
    }

    private int createMenuButton(int x, int y, String label, java.util.function.Supplier<GuiBase> screenSupplier) {
        ButtonGeneric button = new ButtonGeneric(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, label);
        this.addButton(button, (btn, mouseButton) -> BaseScreen.openScreen(screenSupplier.get()));

        return y + BUTTON_HEIGHT + BUTTON_MARGIN;
    }
}

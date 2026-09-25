package com.minetenant.minetenantclient.gui;

import java.util.List;

import net.minecraft.client.gui.screen.Screen;

import fi.dy.masa.malilib.gui.GuiConfigsBase;

import com.minetenant.minetenantclient.Reference;
import com.minetenant.minetenantclient.config.Configs;

public class GuiConfigsHotkeys extends GuiConfigsBase {

    public GuiConfigsHotkeys(Screen parent) {
        super(10, 50, Reference.MOD_ID, parent, "minetenantclient.gui.title.configs.hotkeys");

        this.setParent(parent);
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return ConfigOptionWrapper.createFor(Configs.Hotkeys.HOTKEY_LIST);
    }
}

package com.minetenant.minetenantclient.gui;

import java.util.List;

import net.minecraft.client.gui.screen.Screen;

import fi.dy.masa.malilib.gui.GuiConfigsBase;

import com.minetenant.minetenantclient.Reference;
import com.minetenant.minetenantclient.config.Configs;

public class GuiConfigsGeneric extends GuiConfigsBase {

    public GuiConfigsGeneric(Screen parent) {
        super(10, 50, Reference.MOD_ID, parent, "minetenantclient.gui.title.configs.generic");

        this.setParent(parent);
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return ConfigOptionWrapper.createFor(Configs.Generic.OPTIONS);
    }
}

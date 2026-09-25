package com.minetenant.minetenantclient.config;

import net.minecraft.client.MinecraftClient;

import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;

import com.minetenant.minetenantclient.Reference;
import com.minetenant.minetenantclient.gui.GuiMinetenantMain;

public class HotkeyHandler implements IKeybindProvider, IClientTickHandler {

    public static final HotkeyHandler INSTANCE = new HotkeyHandler();

    private HotkeyHandler() {
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (var hotkey : Configs.Hotkeys.HOTKEY_LIST) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(Reference.MOD_ID, Reference.MOD_NAME, Configs.Hotkeys.HOTKEY_LIST);
    }

    @Override
    public void onClientTick(MinecraftClient mc) {
        if (mc.currentScreen == null && Configs.Hotkeys.OPEN_GUI.getKeybind().isPressed()) {
            BaseScreen.openScreen(new GuiMinetenantMain());
        }
    }
}

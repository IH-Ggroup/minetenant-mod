package com.minetenant.minetenantclient.client;

import net.fabricmc.api.ClientModInitializer;

import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;

import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

import com.minetenant.minetenantclient.Reference;
import com.minetenant.minetenantclient.config.HotkeyHandler;
import com.minetenant.minetenantclient.gui.GuiConfigs;

public class MinetenantclientClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        InputEventHandler.getKeybindManager().registerKeybindProvider(HotkeyHandler.INSTANCE);
        TickHandler.getInstance().registerClientTickHandler(HotkeyHandler.INSTANCE);

        // 初回表示からMOD切り替えプルダウンを出すため、設定画面を先に登録しておく
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(Reference.MOD_ID, Reference.MOD_NAME, () -> new GuiConfigs(null, GuiConfigs.Tab.GENERIC)));
    }
}

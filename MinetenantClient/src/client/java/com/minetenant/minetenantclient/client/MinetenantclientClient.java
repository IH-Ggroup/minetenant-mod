package com.minetenant.minetenantclient.client;

import net.fabricmc.api.ClientModInitializer;

import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;

import com.minetenant.minetenantclient.config.HotkeyHandler;

public class MinetenantclientClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        InputEventHandler.getKeybindManager().registerKeybindProvider(HotkeyHandler.INSTANCE);
        TickHandler.getInstance().registerClientTickHandler(HotkeyHandler.INSTANCE);
    }
}

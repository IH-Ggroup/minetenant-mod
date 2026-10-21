package com.minetenant.minetenantclient.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;

import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

import com.minetenant.minetenantclient.Reference;
import com.minetenant.minetenantclient.config.ConfigHandler;
import com.minetenant.minetenantclient.config.HotkeyHandler;
import com.minetenant.minetenantclient.gui.GuiConfigs;
import com.minetenant.minetenantclient.network.ServerPingClient;
import com.minetenant.minetenantclient.network.PongPayload;

public class MinetenantclientClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 設定の保存・読み込み
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, ConfigHandler.INSTANCE);
        ConfigHandler.INSTANCE.load();

        InputEventHandler.getKeybindManager().registerKeybindProvider(HotkeyHandler.INSTANCE);
        TickHandler.getInstance().registerClientTickHandler(HotkeyHandler.INSTANCE);

        // 疎通確認: Pong受信、タイムアウト判定、切断時のリセット
        ClientPlayNetworking.registerGlobalReceiver(PongPayload.ID, (payload, context) -> ServerPingClient.onPong(payload));
        ClientTickEvents.END_CLIENT_TICK.register(client -> ServerPingClient.onClientTick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ServerPingClient.reset());

        // 初回表示からMOD切り替えプルダウンを出すため、設定画面を先に登録しておく
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(Reference.MOD_ID, Reference.MOD_NAME, () -> new GuiConfigs(null, GuiConfigs.Tab.GENERIC)));
    }
}

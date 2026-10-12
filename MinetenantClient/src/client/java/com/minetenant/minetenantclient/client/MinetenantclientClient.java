package com.minetenant.minetenantclient.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;

import com.minetenant.minetenantclient.config.HotkeyHandler;
import com.minetenant.minetenantclient.network.PingClient;
import com.minetenant.minetenantclient.network.PongPayload;

public class MinetenantclientClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        InputEventHandler.getKeybindManager().registerKeybindProvider(HotkeyHandler.INSTANCE);
        TickHandler.getInstance().registerClientTickHandler(HotkeyHandler.INSTANCE);

        // 疎通確認: Pong受信、タイムアウト判定、切断時のリセット
        ClientPlayNetworking.registerGlobalReceiver(PongPayload.ID, (payload, context) -> PingClient.onPong(payload));
        ClientTickEvents.END_CLIENT_TICK.register(client -> PingClient.onClientTick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PingClient.reset());
    }
}

package com.minetenant.minetenantclient;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import com.minetenant.minetenantclient.network.PingPayload;
import com.minetenant.minetenantclient.network.PongPayload;

public class Minetenantclient implements ModInitializer {

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(PingPayload.ID, PingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PongPayload.ID, PongPayload.CODEC);
    }
}

package com.minetenant.minetenantserver;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minetenant.minetenantserver.network.PingPayload;
import com.minetenant.minetenantserver.network.PongPayload;

public class Minetenantserver implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("minetenantserver");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(PingPayload.ID, PingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PongPayload.ID, PongPayload.CODEC);

        // Ping を受けたら、受け取った値をそのまま Pong で返す
        ServerPlayNetworking.registerGlobalReceiver(PingPayload.ID, (payload, context) -> {
            LOGGER.info("Ping受信: {}", context.player().getName().getString());
            ServerPlayNetworking.send(context.player(), new PongPayload(payload.sentAt()));
        });
    }
}

package com.minetenant.minetenantserver;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minetenant.minetenantserver.config.HonoConfig;
import com.minetenant.minetenantserver.config.HonoConfigLoader;
import com.minetenant.minetenantserver.network.PingPayload;
import com.minetenant.minetenantserver.network.PongPayload;

public class Minetenantserver implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("minetenantserver");

    private static HonoConfig config;

    public static HonoConfig getConfig() {
        return config;
    }

    @Override
    public void onInitialize() {
        // 設定が不正なら例外でサーバー起動を止める
        config = HonoConfigLoader.load(
                FabricLoader.getInstance().getConfigDir().resolve(HonoConfigLoader.FILE_NAME));
        LOGGER.info("Hono設定を読み込みました: {}", config);
        if (!config.hasToken()) {
            LOGGER.warn("server token が未設定です。認証が必要なAPIは呼べません");
        }

        PayloadTypeRegistry.playC2S().register(PingPayload.ID, PingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PongPayload.ID, PongPayload.CODEC);

        // Ping を受けたら、受け取った値をそのまま Pong で返す
        ServerPlayNetworking.registerGlobalReceiver(PingPayload.ID, (payload, context) -> {
            LOGGER.info("Ping受信: {}", context.player().getName().getString());
            ServerPlayNetworking.send(context.player(), new PongPayload(payload.sentAt()));
        });
    }
}

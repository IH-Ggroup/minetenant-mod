package com.minetenant.minetenantserver.network;

import java.util.function.Predicate;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minetenant.minetenantserver.catalog.CatalogRequestProcessor;
import com.minetenant.minetenantserver.catalog.CatalogService;

/**
 * 商品一覧のpacketを受け取り、結果を送信者だけに返す。
 * UUIDは context.player() から取るので、Clientは偽のUUIDを申告できない。
 */
public final class CatalogPacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("minetenantserver");

    private CatalogPacketHandler() {
    }

    public static void register(CatalogService service, Predicate<String> isRegisteredStore) {
        CatalogRequestProcessor processor = new CatalogRequestProcessor(service, isRegisteredStore);

        ServerPlayNetworking.registerGlobalReceiver(CatalogRequest.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            long requestId = payload.requestId();

            // 取得はHTTP通信で非同期。完了後の送信はserver threadで行い、その間に切断したplayerには送らない
            processor.process(player.getUuid(), payload.storeId()).thenAccept(result ->
                    context.server().execute(() -> {
                        LOGGER.info("Catalog request handled: player={}, result={}, count={}",
                                player.getName().getString(), result.code(), result.products().size());
                        if (player.networkHandler != null && player.networkHandler.isConnectionOpen()) {
                            ServerPlayNetworking.send(player,
                                    CatalogResponse.of(requestId, result.code(), result.products()));
                        }
                    }));
        });
    }
}

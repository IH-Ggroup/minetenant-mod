package com.minetenant.minetenantserver.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minetenant.minetenantserver.catalog.ProductDetailRequestProcessor;
import com.minetenant.minetenantserver.catalog.ProductDetailService;

/**
 * 商品詳細のpacketを受け取り、結果を送信者だけに返す。
 * UUIDは context.player() から取るので、Clientは偽のUUIDを申告できない。
 */
public final class ProductDetailPacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("minetenantserver");

    private ProductDetailPacketHandler() {
    }

    public static void register(ProductDetailService service) {
        ProductDetailRequestProcessor processor = new ProductDetailRequestProcessor(service);

        ServerPlayNetworking.registerGlobalReceiver(ProductDetailRequest.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            long requestId = payload.requestId();

            // 取得はHTTP通信で非同期。完了後の送信はserver threadで行い、その間に切断したplayerには送らない
            processor.process(player.getUuid(), payload.productId()).thenAccept(result ->
                    context.server().execute(() -> {
                        LOGGER.info("Product detail request handled: player={}, result={}",
                                player.getName().getString(), result.code());
                        if (player.networkHandler != null && player.networkHandler.isConnectionOpen()) {
                            ServerPlayNetworking.send(player, ProductDetailResponse.of(requestId, result));
                        }
                    }));
        });
    }
}

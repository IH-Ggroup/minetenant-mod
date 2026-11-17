package com.minetenant.minetenantserver.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minetenant.minetenantserver.link.LinkRequestProcessor;
import com.minetenant.minetenantserver.link.LinkService;

/**
 * 連携コードのpacketを受け取り、結果を送信者だけに返す。
 * UUIDは context.player() から取るので、Clientは偽のUUIDを申告できない。
 */
public final class LinkPacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("minetenantserver");

    private LinkPacketHandler() {
    }

    public static void register(LinkService service) {
        LinkRequestProcessor processor = new LinkRequestProcessor(service);

        ServerPlayNetworking.registerGlobalReceiver(LinkAccountRequest.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            LinkResultCode result = processor.process(player.getUuid(), payload.code());
            // コードはログに残さない
            LOGGER.info("Link request handled: player={}, result={}", player.getName().getString(), result);
            ServerPlayNetworking.send(player, new LinkAccountResponse(payload.requestId(), result));
        });
    }
}

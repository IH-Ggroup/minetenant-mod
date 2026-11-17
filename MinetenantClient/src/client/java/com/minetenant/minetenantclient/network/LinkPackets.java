package com.minetenant.minetenantclient.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.minetenant.minetenantclient.link.LinkPacketClient;
import com.minetenant.minetenantclient.link.TickScheduler;

/**
 * 連携packetの実送信と受信の登録。LinkPacketClient を実際の通信につなぐ。
 */
public final class LinkPackets {

    public static final LinkPacketClient CLIENT = new LinkPacketClient(new LinkPacketClient.Transport() {
        @Override
        public boolean canSend() {
            return ClientPlayNetworking.canSend(LinkAccountRequest.ID);
        }

        @Override
        public void send(long requestId, String code) {
            ClientPlayNetworking.send(new LinkAccountRequest(requestId, code));
        }
    }, TickScheduler.INSTANCE);

    private LinkPackets() {
    }

    /** 結果packetの受信を登録する。 */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(LinkAccountResponse.ID,
                (payload, context) -> CLIENT.onResponse(payload.requestId(), payload.result()));
    }
}

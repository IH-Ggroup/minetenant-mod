package com.minetenant.minetenantserver.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 疎通確認用のPingパケット。クライアントとサーバーで同じ定義を持つ。
 * sentAt は Ping を送った時刻(System.nanoTime)で、Pong にそのまま入れて返す。
 */
public record PingPayload(long sentAt) implements CustomPayload {

    public static final CustomPayload.Id<PingPayload> ID = new CustomPayload.Id<>(Identifier.of("minetenant", "ping"));
    public static final PacketCodec<RegistryByteBuf, PingPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_LONG, PingPayload::sentAt, PingPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

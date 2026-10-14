package com.minetenant.minetenantserver.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 疎通確認用のPongパケット。クライアントとサーバーで同じ定義を持つ。
 * sentAt は Ping を送った時刻(System.nanoTime)で、Pong にそのまま入れて返す。
 */
public record PongPayload(long sentAt) implements CustomPayload {

    public static final CustomPayload.Id<PongPayload> ID = new CustomPayload.Id<>(Identifier.of("minetenant", "pong"));
    public static final PacketCodec<RegistryByteBuf, PongPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_LONG, PongPayload::sentAt, PongPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

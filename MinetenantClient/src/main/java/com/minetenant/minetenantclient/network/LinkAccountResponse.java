package com.minetenant.minetenantclient.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 連携結果を返すパケット(Server→Client)。クライアントとサーバーで同じ定義を持つ。
 * requestId は request の値をそのまま返す。username・password・tokenなどは含めない。
 */
public record LinkAccountResponse(long requestId, LinkResultCode result) implements CustomPayload {

    public static final CustomPayload.Id<LinkAccountResponse> ID =
            new CustomPayload.Id<>(Identifier.of("minetenant", "link_account_response"));
    public static final PacketCodec<RegistryByteBuf, LinkAccountResponse> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, LinkAccountResponse::requestId,
            PacketCodecs.VAR_INT.xmap(LinkResultCode::fromId, LinkResultCode::id), LinkAccountResponse::result,
            LinkAccountResponse::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

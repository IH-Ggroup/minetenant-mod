package com.minetenant.minetenantserver.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 連携コードを送るパケット(Client→Server)。クライアントとサーバーで同じ定義を持つ。
 * UUIDなどの本人情報は含めない。Serverが接続中のplayerから取得する。
 */
public record LinkAccountRequest(long requestId, String code) implements CustomPayload {

    /** codeの最大文字数。これを超えるpacketは復号時に拒否される。 */
    public static final int MAX_CODE_LENGTH = 64;

    public static final CustomPayload.Id<LinkAccountRequest> ID =
            new CustomPayload.Id<>(Identifier.of("minetenant", "link_account_request"));
    public static final PacketCodec<RegistryByteBuf, LinkAccountRequest> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, LinkAccountRequest::requestId,
            PacketCodecs.string(MAX_CODE_LENGTH), LinkAccountRequest::code,
            LinkAccountRequest::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

package com.minetenant.minetenantclient.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 商品一覧を要求するパケット(Client→Server)。クライアントとサーバーで同じ定義を持つ。
 * UUIDなどの本人情報は含めない。Serverが接続中のplayerから取得する。
 */
public record CatalogRequest(long requestId, String storeId) implements CustomPayload {

    /** storeIdの最大文字数。これを超えるpacketは復号時に拒否される。 */
    public static final int MAX_STORE_ID_LENGTH = 64;

    public static final CustomPayload.Id<CatalogRequest> ID =
            new CustomPayload.Id<>(Identifier.of("minetenant", "catalog_request"));
    public static final PacketCodec<RegistryByteBuf, CatalogRequest> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, CatalogRequest::requestId,
            PacketCodecs.string(MAX_STORE_ID_LENGTH), CatalogRequest::storeId,
            CatalogRequest::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

package com.minetenant.minetenantserver.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 商品詳細を要求するパケット(Client→Server)。クライアントとサーバーで同じ定義を持つ。
 * 送るのはproductIdだけ。価格や状態、UUIDなどの本人情報は含めない(Serverが取得する)。
 */
public record ProductDetailRequest(long requestId, String productId) implements CustomPayload {

    /** productIdの最大文字数。これを超えるpacketは復号時に拒否される。 */
    public static final int MAX_PRODUCT_ID_LENGTH = 64;

    public static final CustomPayload.Id<ProductDetailRequest> ID =
            new CustomPayload.Id<>(Identifier.of("minetenant", "product_detail_request"));
    public static final PacketCodec<RegistryByteBuf, ProductDetailRequest> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, ProductDetailRequest::requestId,
            PacketCodecs.string(MAX_PRODUCT_ID_LENGTH), ProductDetailRequest::productId,
            ProductDetailRequest::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

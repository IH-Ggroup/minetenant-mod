package com.minetenant.minetenantserver.network;

import java.util.Optional;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import com.minetenant.minetenantserver.catalog.ProductDetail;
import com.minetenant.minetenantserver.catalog.ProductDetailPacketLimits;
import com.minetenant.minetenantserver.catalog.ProductDetailResult;
import com.minetenant.minetenantserver.catalog.ProductStatus;

/**
 * 商品詳細を返すパケット(Server→Client)。クライアントとサーバーで同じ定義を持つ。
 * requestId は request の値をそのまま返す。失敗時の商品は空。sellerの情報やtokenは含めない。
 */
public record ProductDetailResponse(long requestId, ProductDetailResultCode code, Optional<ProductDetail> detail)
        implements CustomPayload {

    public static final CustomPayload.Id<ProductDetailResponse> ID =
            new CustomPayload.Id<>(Identifier.of("minetenant", "product_detail_response"));

    // 項目数がtupleの上限を超えるため、順番に書き込む。並びと上限はClientと揃えること
    private static final PacketCodec<RegistryByteBuf, ProductDetail> DETAIL_CODEC = new PacketCodec<>() {
        private final PacketCodec<ByteBuf, String> id = PacketCodecs.string(ProductDetailPacketLimits.MAX_ID_LENGTH);
        private final PacketCodec<ByteBuf, String> name = PacketCodecs.string(ProductDetailPacketLimits.MAX_NAME_LENGTH);
        private final PacketCodec<ByteBuf, String> description = PacketCodecs.string(ProductDetailPacketLimits.MAX_DESCRIPTION_LENGTH);
        private final PacketCodec<ByteBuf, String> category = PacketCodecs.string(ProductDetailPacketLimits.MAX_CATEGORY_LENGTH);
        private final PacketCodec<ByteBuf, String> theme = PacketCodecs.string(ProductDetailPacketLimits.MAX_THEME_LENGTH);
        private final PacketCodec<ByteBuf, String> emoji = PacketCodecs.string(ProductDetailPacketLimits.MAX_EMOJI_LENGTH);

        @Override
        public ProductDetail decode(RegistryByteBuf buf) {
            String detailId = id.decode(buf);
            String storeId = id.decode(buf);
            String detailName = name.decode(buf);
            String detailDescription = description.decode(buf);
            long price = PacketCodecs.VAR_LONG.decode(buf);
            ProductStatus status = ProductStatus.fromId(PacketCodecs.VAR_INT.decode(buf));
            return new ProductDetail(detailId, storeId, detailName, detailDescription, price, status,
                    category.decode(buf), theme.decode(buf), emoji.decode(buf));
        }

        @Override
        public void encode(RegistryByteBuf buf, ProductDetail value) {
            id.encode(buf, value.id());
            id.encode(buf, value.storeId());
            name.encode(buf, value.name());
            description.encode(buf, value.description());
            PacketCodecs.VAR_LONG.encode(buf, value.price());
            PacketCodecs.VAR_INT.encode(buf, value.status().id());
            category.encode(buf, value.category());
            theme.encode(buf, value.theme());
            emoji.encode(buf, value.emoji());
        }
    };

    public static final PacketCodec<RegistryByteBuf, ProductDetailResponse> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, ProductDetailResponse::requestId,
            PacketCodecs.VAR_INT.xmap(ProductDetailResultCode::fromId, ProductDetailResultCode::id),
            ProductDetailResponse::code,
            PacketCodecs.optional(DETAIL_CODEC), ProductDetailResponse::detail,
            ProductDetailResponse::new);

    /** packetに載せられない商品(idが長すぎるなど)は、送れないので UNKNOWN の失敗にする。 */
    public static ProductDetailResponse of(long requestId, ProductDetailResult result) {
        if (!result.isSuccess()) {
            return new ProductDetailResponse(requestId, result.code(), Optional.empty());
        }

        ProductDetail fitted = ProductDetailPacketLimits.fit(result.detail());
        if (fitted == null) {
            return new ProductDetailResponse(requestId, ProductDetailResultCode.UNKNOWN, Optional.empty());
        }

        return new ProductDetailResponse(requestId, ProductDetailResultCode.SUCCESS, Optional.of(fitted));
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

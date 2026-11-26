package com.minetenant.minetenantclient.network;

import java.util.Optional;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import com.minetenant.minetenantclient.catalog.ProductDetail;
import com.minetenant.minetenantclient.catalog.ProductStatus;

/**
 * 商品詳細を返すパケット(Server→Client)。クライアントとサーバーで同じ定義を持つ。
 * requestId は request の値をそのまま返す。失敗時の商品は空。sellerの情報やtokenは含めない。
 */
public record ProductDetailResponse(long requestId, ProductDetailResultCode code, Optional<ProductDetail> detail)
        implements CustomPayload {

    public static final CustomPayload.Id<ProductDetailResponse> ID =
            new CustomPayload.Id<>(Identifier.of("minetenant", "product_detail_response"));

    /** 文字数の上限。Serverの ProductDetailPacketLimits と同じ値にすること。 */
    private static final class Limits {
        static final int MAX_ID_LENGTH = 256;
        static final int MAX_NAME_LENGTH = 256;
        static final int MAX_DESCRIPTION_LENGTH = 1024;
        static final int MAX_CATEGORY_LENGTH = 64;
        static final int MAX_THEME_LENGTH = 64;
        static final int MAX_EMOJI_LENGTH = 32;

        private Limits() {
        }
    }

    // 項目数がtupleの上限を超えるため、順番に書き込む。並びと上限はServerと揃えること
    private static final PacketCodec<RegistryByteBuf, ProductDetail> DETAIL_CODEC = new PacketCodec<>() {
        private final PacketCodec<ByteBuf, String> id = PacketCodecs.string(Limits.MAX_ID_LENGTH);
        private final PacketCodec<ByteBuf, String> name = PacketCodecs.string(Limits.MAX_NAME_LENGTH);
        private final PacketCodec<ByteBuf, String> description = PacketCodecs.string(Limits.MAX_DESCRIPTION_LENGTH);
        private final PacketCodec<ByteBuf, String> category = PacketCodecs.string(Limits.MAX_CATEGORY_LENGTH);
        private final PacketCodec<ByteBuf, String> theme = PacketCodecs.string(Limits.MAX_THEME_LENGTH);
        private final PacketCodec<ByteBuf, String> emoji = PacketCodecs.string(Limits.MAX_EMOJI_LENGTH);

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

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

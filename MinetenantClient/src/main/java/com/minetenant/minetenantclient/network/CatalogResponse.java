package com.minetenant.minetenantclient.network;

import java.util.List;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import com.minetenant.minetenantclient.catalog.ProductStatus;
import com.minetenant.minetenantclient.catalog.ProductSummary;

/**
 * 商品一覧を返すパケット(Server→Client)。クライアントとサーバーで同じ定義を持つ。
 * requestId は request の値をそのまま返す。失敗時の商品は空。sellerの情報やtokenは含めない。
 */
public record CatalogResponse(long requestId, CatalogResultCode code, List<ProductSummary> products)
        implements CustomPayload {

    /** 1回で返す商品数の上限。ページ分割は一覧GUIのIssueで扱う。 */
    public static final int MAX_PRODUCTS = 256;

    private static final int MAX_TEXT_LENGTH = 256;

    public static final CustomPayload.Id<CatalogResponse> ID =
            new CustomPayload.Id<>(Identifier.of("minetenant", "catalog_response"));

    private static final PacketCodec<RegistryByteBuf, ProductSummary> PRODUCT_CODEC = PacketCodec.tuple(
            PacketCodecs.string(MAX_TEXT_LENGTH), ProductSummary::id,
            PacketCodecs.string(MAX_TEXT_LENGTH), ProductSummary::storeId,
            PacketCodecs.string(MAX_TEXT_LENGTH), ProductSummary::name,
            PacketCodecs.VAR_LONG, ProductSummary::price,
            PacketCodecs.VAR_INT.xmap(ProductStatus::fromId, ProductStatus::id), ProductSummary::status,
            ProductSummary::new);

    public static final PacketCodec<RegistryByteBuf, CatalogResponse> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, CatalogResponse::requestId,
            PacketCodecs.VAR_INT.xmap(CatalogResultCode::fromId, CatalogResultCode::id), CatalogResponse::code,
            PRODUCT_CODEC.collect(PacketCodecs.toList(MAX_PRODUCTS)), CatalogResponse::products,
            CatalogResponse::new);

    public static CatalogResponse of(long requestId, CatalogResultCode code, List<ProductSummary> products) {
        List<ProductSummary> limited = products.size() > MAX_PRODUCTS ? products.subList(0, MAX_PRODUCTS) : products;
        return new CatalogResponse(requestId, code, List.copyOf(limited));
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

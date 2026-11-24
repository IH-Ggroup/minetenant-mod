package com.minetenant.minetenantclient;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import com.minetenant.minetenantclient.network.CatalogRequest;
import com.minetenant.minetenantclient.network.CatalogResponse;
import com.minetenant.minetenantclient.network.LinkAccountRequest;
import com.minetenant.minetenantclient.network.LinkAccountResponse;
import com.minetenant.minetenantclient.network.PingPayload;
import com.minetenant.minetenantclient.network.ProductDetailRequest;
import com.minetenant.minetenantclient.network.ProductDetailResponse;
import com.minetenant.minetenantclient.network.PongPayload;

public class Minetenantclient implements ModInitializer {

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(PingPayload.ID, PingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PongPayload.ID, PongPayload.CODEC);

        // 連携コードの送信と結果の受信
        PayloadTypeRegistry.playC2S().register(LinkAccountRequest.ID, LinkAccountRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(LinkAccountResponse.ID, LinkAccountResponse.CODEC);

        // 商品一覧の要求と結果の受信
        PayloadTypeRegistry.playC2S().register(CatalogRequest.ID, CatalogRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(CatalogResponse.ID, CatalogResponse.CODEC);

        // 商品詳細の要求と結果の受信
        PayloadTypeRegistry.playC2S().register(ProductDetailRequest.ID, ProductDetailRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(ProductDetailResponse.ID, ProductDetailResponse.CODEC);
    }
}

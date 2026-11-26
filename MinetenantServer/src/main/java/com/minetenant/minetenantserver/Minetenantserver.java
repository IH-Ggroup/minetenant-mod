package com.minetenant.minetenantserver;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minetenant.minetenantserver.catalog.FixtureCatalogService;
import com.minetenant.minetenantserver.catalog.HonoProductDetailService;
import com.minetenant.minetenantserver.catalog.ProductStatus;
import com.minetenant.minetenantserver.catalog.ProductSummary;
import com.minetenant.minetenantserver.config.HonoConfig;
import com.minetenant.minetenantserver.config.HonoConfigLoader;
import com.minetenant.minetenantserver.http.HonoApiClient;
import com.minetenant.minetenantserver.link.FixtureLinkService;
import com.minetenant.minetenantserver.network.CatalogPacketHandler;
import com.minetenant.minetenantserver.network.CatalogRequest;
import com.minetenant.minetenantserver.network.CatalogResponse;
import com.minetenant.minetenantserver.network.LinkAccountRequest;
import com.minetenant.minetenantserver.network.LinkAccountResponse;
import com.minetenant.minetenantserver.network.LinkPacketHandler;
import com.minetenant.minetenantserver.network.PingPayload;
import com.minetenant.minetenantserver.network.PongPayload;
import com.minetenant.minetenantserver.network.ProductDetailPacketHandler;
import com.minetenant.minetenantserver.network.ProductDetailRequest;
import com.minetenant.minetenantserver.network.ProductDetailResponse;

public class Minetenantserver implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("minetenantserver");

    private static HonoConfig config;

    private static HonoApiClient apiClient;

    public static HonoConfig getConfig() {
        return config;
    }

    public static HonoApiClient getApiClient() {
        return apiClient;
    }

    @Override
    public void onInitialize() {
        // 設定が不正なら例外でサーバー起動を止める
        config = HonoConfigLoader.load(
                FabricLoader.getInstance().getConfigDir().resolve(HonoConfigLoader.FILE_NAME));
        LOGGER.info("Loaded Hono config: {}", config);
        if (!config.hasToken()) {
            LOGGER.warn("{} is not set. Minecraft APIs cannot be called", HonoConfigLoader.ENV_TOKEN);
        }

        // Minecraft routeへのAuthorization(Bearer)はHonoApiClientがpathを見て付ける
        apiClient = new HonoApiClient(config, Map.of());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> apiClient.close());

        PayloadTypeRegistry.playC2S().register(PingPayload.ID, PingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PongPayload.ID, PongPayload.CODEC);

        // 連携コードの受信と結果の返信
        PayloadTypeRegistry.playC2S().register(LinkAccountRequest.ID, LinkAccountRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(LinkAccountResponse.ID, LinkAccountResponse.CODEC);

        // 連携コード: 今はfixtureで判定する(Hono接続は別Issue)
        LinkPacketHandler.register(new FixtureLinkService());

        // 商品一覧の受信と結果の返信
        PayloadTypeRegistry.playC2S().register(CatalogRequest.ID, CatalogRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(CatalogResponse.ID, CatalogResponse.CODEC);

        // 商品一覧: 今はfixtureで返す(Hono接続は別Issue)。店舗登録(#39)ができるまではfixtureの店舗を登録済みとみなす
        Map<String, List<ProductSummary>> fixtureProducts = Map.of(
                "store-a", List.of(
                        new ProductSummary("product-a1", "store-a", "Sample shirt", 1000, ProductStatus.AVAILABLE),
                        new ProductSummary("product-a2", "store-a", "Sample hat", 500, ProductStatus.SOLD)),
                "store-empty", List.of());
        CatalogPacketHandler.register(new FixtureCatalogService(fixtureProducts), fixtureProducts::containsKey);

        // 商品詳細の受信と結果の返信。Honoから毎回最新を取得する(公開GETなのでServer認証は不要)
        PayloadTypeRegistry.playC2S().register(ProductDetailRequest.ID, ProductDetailRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(ProductDetailResponse.ID, ProductDetailResponse.CODEC);
        ProductDetailPacketHandler.register(new HonoProductDetailService(apiClient));

        // Ping を受けたら、受け取った値をそのまま Pong で返す
        ServerPlayNetworking.registerGlobalReceiver(PingPayload.ID, (payload, context) -> {
            LOGGER.info("Received ping: {}", context.player().getName().getString());
            ServerPlayNetworking.send(context.player(), new PongPayload(payload.sentAt()));
        });
    }
}

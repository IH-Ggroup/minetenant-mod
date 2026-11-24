package com.minetenant.minetenantclient.gui;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.util.StringUtils;

import com.minetenant.minetenantclient.catalog.CatalogNavigationState;
import com.minetenant.minetenantclient.catalog.ProductDetailFixtures;
import com.minetenant.minetenantclient.catalog.ProductDetailUiState;
import com.minetenant.minetenantclient.catalog.ProductListFixtures;
import com.minetenant.minetenantclient.catalog.ProductListUiState;
import com.minetenant.minetenantclient.catalog.ProductDetailViewModel;
import com.minetenant.minetenantclient.catalog.PurchaseConfirmState;
import com.minetenant.minetenantclient.debug.DebugLog;

/**
 * 実APIがつながる前に、商品の画面状態をfixtureで確認するための入り口。
 * 一覧と詳細の往復はここで繋ぎ、購入確認へ進む操作はデバッグログへ記録する。
 */
public final class ProductListFixtureLauncher {

    /** 一覧画面の状態 */
    public enum Fixture {
        LOADING("minetenantclient.gui.button.fixture_loading"),
        EMPTY("minetenantclient.gui.button.fixture_empty"),
        LIST("minetenantclient.gui.button.fixture_list"),
        ERROR("minetenantclient.gui.button.fixture_error"),
        SOLD_MIXED("minetenantclient.gui.button.fixture_sold_mixed"),
        MANY("minetenantclient.gui.button.fixture_many");

        private final String labelKey;

        Fixture(String labelKey) {
            this.labelKey = labelKey;
        }

        public String labelKey() {
            return this.labelKey;
        }
    }

    /** 詳細画面の状態 */
    public enum DetailFixture {
        LOADING("minetenantclient.gui.button.fixture_detail_loading"),
        AVAILABLE("minetenantclient.gui.button.fixture_detail_available"),
        SOLD("minetenantclient.gui.button.fixture_detail_sold"),
        NOT_FOUND("minetenantclient.gui.button.fixture_detail_not_found"),
        ERROR("minetenantclient.gui.button.fixture_detail_error"),
        LONG_TEXT("minetenantclient.gui.button.fixture_detail_long");

        private final String labelKey;

        DetailFixture(String labelKey) {
            this.labelKey = labelKey;
        }

        public String labelKey() {
            return this.labelKey;
        }
    }

    private ProductListFixtureLauncher() {
    }

    public static void open(Screen parent, Fixture fixture) {
        ProductListGui[] holder = new ProductListGui[1];
        CatalogNavigationState navigation = new CatalogNavigationState();

        ProductListUiState state = new ProductListUiState(new ProductListUiState.Listener() {
            @Override
            public void onSelect(String productId) {
                openDetailFor(holder[0], productId);
            }

            @Override
            public void onRetry() {
                DebugLog.add(DebugLog.Level.INFO, StringUtils.translate("minetenantclient.debug.log.product_retry"));
                // 再試行が成功した想定で一覧を表示する
                holder[0].state().showProducts(ProductListFixtures.available());
            }

            @Override
            public void onClose() {
                MinecraftClient.getInstance().setScreen(parent);
            }
        });

        switch (fixture) {
            case LOADING -> state.showLoading();
            case EMPTY -> state.showProducts(List.of());
            case LIST -> state.showProducts(ProductListFixtures.available());
            case ERROR -> state.showError();
            case SOLD_MIXED -> state.showProducts(ProductListFixtures.withSold());
            case MANY -> state.showProducts(ProductListFixtures.many());
        }

        holder[0] = new ProductListGui(parent, state, navigation);
        BaseScreen.openScreen(holder[0]);
    }

    /** 詳細画面の状態を、一覧を経由せず直接開く。 */
    public static void openDetail(Screen parent, DetailFixture fixture) {
        ProductDetailUiState state = newDetailState(parent);

        switch (fixture) {
            case LOADING -> state.showLoading();
            case AVAILABLE -> state.showDetail(ProductDetailFixtures.available());
            case SOLD -> state.showDetail(ProductDetailFixtures.sold());
            case NOT_FOUND -> state.showNotFound();
            case ERROR -> state.showError();
            case LONG_TEXT -> state.showDetail(ProductDetailFixtures.longText());
        }

        BaseScreen.openScreen(new ProductDetailGui(parent, state));
    }

    /** 一覧で選んだ商品の詳細を開く。戻ると一覧(同じ画面)へ戻る。 */
    private static void openDetailFor(ProductListGui list, String productId) {
        ProductDetailUiState state = newDetailState(list);

        ProductDetailFixtures.forId(productId).ifPresentOrElse(state::showDetail, state::showNotFound);

        BaseScreen.openScreen(new ProductDetailGui(list, state));
    }

    /** 購入確認を開く。購入できない商品(sold)のときは開かない。確定はログに記録するだけで、送信はしない。 */
    public static void openPurchaseConfirm(Screen parent, ProductDetailViewModel product) {
        PurchaseConfirmState.open(product, new PurchaseConfirmState.Listener() {
            @Override
            public void onConfirm(String productId) {
                DebugLog.add(DebugLog.Level.INFO,
                        String.format(StringUtils.translate("minetenantclient.debug.log.purchase_confirmed"), productId));
            }

            @Override
            public void onBack() {
                MinecraftClient.getInstance().setScreen(parent);
            }
        }).ifPresent(state -> BaseScreen.openScreen(new PurchaseConfirmGui(parent, state)));
    }

    private static ProductDetailUiState newDetailState(Screen parent) {
        ProductDetailUiState[] holder = new ProductDetailUiState[1];

        holder[0] = new ProductDetailUiState(new ProductDetailUiState.Listener() {
            @Override
            public void onBack() {
                MinecraftClient.getInstance().setScreen(parent);
            }

            @Override
            public void onRetry() {
                DebugLog.add(DebugLog.Level.INFO, StringUtils.translate("minetenantclient.debug.log.product_retry"));
                // 再試行が成功した想定で、fixtureの詳細を表示する
                holder[0].showDetail(ProductDetailFixtures.available());
            }

            @Override
            public void onPurchase(String id) {
                openPurchaseConfirm(MinecraftClient.getInstance().currentScreen, holder[0].detail());
            }
        });

        return holder[0];
    }
}

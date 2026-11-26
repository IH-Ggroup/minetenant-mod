package com.minetenant.minetenantclient.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.util.StringUtils;

import com.minetenant.minetenantclient.catalog.ProductListFixtures;
import com.minetenant.minetenantclient.catalog.ProductListUiState;
import com.minetenant.minetenantclient.debug.DebugLog;

/**
 * 実APIがつながる前に、商品一覧の画面状態をfixtureで確認するための入り口。
 * 選択・再試行・閉じるは、画面を動かす代わりにデバッグログへ記録する。
 */
public final class ProductListFixtureLauncher {

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

    private ProductListFixtureLauncher() {
    }

    public static void open(Screen parent, Fixture fixture) {
        ProductListUiState[] holder = new ProductListUiState[1];

        ProductListUiState state = new ProductListUiState(new ProductListUiState.Listener() {
            @Override
            public void onSelect(String productId) {
                DebugLog.add(DebugLog.Level.INFO,
                        String.format(StringUtils.translate("minetenantclient.debug.log.product_selected"), productId));
            }

            @Override
            public void onRetry() {
                DebugLog.add(DebugLog.Level.INFO, StringUtils.translate("minetenantclient.debug.log.product_retry"));
                // 再試行が成功した想定で一覧を表示する
                holder[0].showProducts(ProductListFixtures.available());
            }

            @Override
            public void onClose() {
                MinecraftClient.getInstance().setScreen(parent);
            }
        });

        holder[0] = state;

        switch (fixture) {
            case LOADING -> state.showLoading();
            case EMPTY -> state.showProducts(java.util.List.of());
            case LIST -> state.showProducts(ProductListFixtures.available());
            case ERROR -> state.showError();
            case SOLD_MIXED -> state.showProducts(ProductListFixtures.withSold());
            case MANY -> state.showProducts(ProductListFixtures.many());
        }

        BaseScreen.openScreen(new ProductListGui(parent, state));
    }
}

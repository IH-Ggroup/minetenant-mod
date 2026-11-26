package com.minetenant.minetenantclient.catalog;

import java.util.List;
import java.util.Objects;

/**
 * 商品一覧画面の状態。GUIのスレッドからだけ使う。
 * 画面は状態を見て描画し、操作はここのメソッド経由で行う。通信はcallbackの先に任せる。
 */
public class ProductListUiState {

    public enum Phase {
        LOADING, EMPTY, LIST, ERROR
    }

    /** 画面が外へ伝える操作。実際の遷移や再取得は呼び出し側で行う。 */
    public interface Listener {
        void onSelect(String productId);

        void onRetry();

        void onClose();
    }

    private final Listener listener;

    private Phase phase = Phase.LOADING;
    private List<ProductSummaryViewModel> items = List.of();

    public ProductListUiState(Listener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    public Phase phase() {
        return this.phase;
    }

    /** LIST のときだけ中身がある。それ以外は空。 */
    public List<ProductSummaryViewModel> items() {
        return this.items;
    }

    public void showLoading() {
        this.phase = Phase.LOADING;
        this.items = List.of();
    }

    /** 取得結果を反映する。0件ならEMPTYにする。 */
    public void showProducts(List<ProductSummary> products) {
        this.items = products.stream().map(ProductSummaryViewModel::from).toList();
        this.phase = this.items.isEmpty() ? Phase.EMPTY : Phase.LIST;
    }

    public void showError() {
        this.phase = Phase.ERROR;
        this.items = List.of();
    }

    /**
     * 商品を選ぶ。一覧表示中で、選べる商品(available)のときだけ通知する。
     *
     * @return 通知したら true
     */
    public boolean select(String productId) {
        if (this.phase != Phase.LIST) {
            return false;
        }

        boolean selectable = this.items.stream().anyMatch(item -> item.id().equals(productId) && item.selectable());

        if (!selectable) {
            return false;
        }

        this.listener.onSelect(productId);
        return true;
    }

    /**
     * 再試行する。失敗表示中だけ受け付け、すぐLOADINGにするので連打しても1回しか通知しない。
     *
     * @return 通知したら true
     */
    public boolean retry() {
        if (this.phase != Phase.ERROR) {
            return false;
        }

        this.showLoading();
        this.listener.onRetry();
        return true;
    }

    public void close() {
        this.listener.onClose();
    }
}

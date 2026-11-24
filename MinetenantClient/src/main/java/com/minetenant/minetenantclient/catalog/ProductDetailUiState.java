package com.minetenant.minetenantclient.catalog;

import java.util.Objects;

/**
 * 商品詳細画面の状態。GUIのスレッドからだけ使う。
 * 購入確認へ進めるのは AVAILABLE のときだけ。SOLD・NOT_FOUND・ERROR では進めない。
 */
public class ProductDetailUiState {

    public enum Phase {
        LOADING, AVAILABLE, SOLD, NOT_FOUND, ERROR
    }

    /** 画面が外へ伝える操作。実際の遷移や再取得は呼び出し側で行う。 */
    public interface Listener {
        void onBack();

        void onRetry();

        /** 購入確認へ進む。購入の確定ではない。 */
        void onPurchase(String productId);
    }

    private final Listener listener;

    private Phase phase = Phase.LOADING;
    private ProductDetailViewModel detail;

    public ProductDetailUiState(Listener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    public Phase phase() {
        return this.phase;
    }

    /** AVAILABLE か SOLD のときだけ中身がある。それ以外は null。 */
    public ProductDetailViewModel detail() {
        return this.detail;
    }

    public void showLoading() {
        this.phase = Phase.LOADING;
        this.detail = null;
    }

    /** 取得した詳細を反映する。状態(sold)に合わせてフェーズを決める。 */
    public void showDetail(ProductDetail product) {
        this.detail = ProductDetailViewModel.from(product);
        this.phase = this.detail.purchasable() ? Phase.AVAILABLE : Phase.SOLD;
    }

    public void showNotFound() {
        this.phase = Phase.NOT_FOUND;
        this.detail = null;
    }

    public void showError() {
        this.phase = Phase.ERROR;
        this.detail = null;
    }

    /** 購入確認へ進めるか。 */
    public boolean canPurchase() {
        return this.phase == Phase.AVAILABLE;
    }

    /**
     * 購入確認へ進む。AVAILABLE のときだけ通知する。
     *
     * @return 通知したら true
     */
    public boolean purchase() {
        if (!this.canPurchase()) {
            return false;
        }

        this.listener.onPurchase(this.detail.id());
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

    public void back() {
        this.listener.onBack();
    }
}

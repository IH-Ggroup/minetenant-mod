package com.minetenant.minetenantclient.catalog;

import java.util.Objects;
import java.util.Optional;

/**
 * 購入確認画面の状態。GUIのスレッドからだけ使う。
 * 「購入を確定」を押した時点が取引成立の要求になるため、確定は1回しか通知しない。
 * 確定後はLOADINGになり、確定も戻るも受け付けない。
 */
public class PurchaseConfirmState {

    public enum Phase {
        CONFIRM, LOADING
    }

    /** キーボードで操作できるボタン */
    public enum Button {
        CONFIRM, BACK
    }

    /** 画面が外へ伝える操作。通信は呼び出し側で行う。 */
    public interface Listener {
        /** 購入の確定を要求する。 */
        void onConfirm(String productId);

        /** 送信せずに詳細へ戻る。 */
        void onBack();
    }

    private final ProductDetailViewModel product;
    private final Listener listener;

    private Phase phase = Phase.CONFIRM;
    /** 誤操作を避けるため、最初のfocusは戻る側にする。 */
    private Button focus = Button.BACK;

    private PurchaseConfirmState(ProductDetailViewModel product, Listener listener) {
        this.product = product;
        this.listener = listener;
    }

    /**
     * 購入確認画面の状態を作る。購入できない商品(sold など)のときは空を返し、確認画面を開かせない。
     */
    public static Optional<PurchaseConfirmState> open(ProductDetailViewModel product, Listener listener) {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(listener, "listener");

        if (!product.purchasable()) {
            return Optional.empty();
        }

        return Optional.of(new PurchaseConfirmState(product, listener));
    }

    public Phase phase() {
        return this.phase;
    }

    public ProductDetailViewModel product() {
        return this.product;
    }

    public Button focus() {
        return this.focus;
    }

    /** ボタンを押せるか。確定後(LOADING)は両方押せない。 */
    public boolean isInteractive() {
        return this.phase == Phase.CONFIRM;
    }

    /**
     * 購入を確定する。確定後は何度押しても通知しない。
     *
     * @return 通知したら true
     */
    public boolean confirm() {
        if (this.phase != Phase.CONFIRM) {
            return false;
        }

        // callbackが同期で返っても二重にならないよう、先にLOADINGにする
        this.phase = Phase.LOADING;
        this.listener.onConfirm(this.product.id());
        return true;
    }

    /**
     * 送信せずに戻る。確定後は戻れない。
     *
     * @return 通知したら true
     */
    public boolean back() {
        if (this.phase != Phase.CONFIRM) {
            return false;
        }

        this.listener.onBack();
        return true;
    }

    /** 左右やTabでfocusを切り替える。確定後は動かさない。 */
    public void moveFocus() {
        if (this.phase != Phase.CONFIRM) {
            return;
        }

        this.focus = this.focus == Button.CONFIRM ? Button.BACK : Button.CONFIRM;
    }

    /** Enterで、focusしているボタンを押す。 */
    public boolean activateFocused() {
        return this.focus == Button.CONFIRM ? this.confirm() : this.back();
    }
}

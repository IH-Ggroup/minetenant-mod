package com.minetenant.minetenantclient.catalog;

/**
 * 一覧と詳細を行き来するときの、一覧側の位置。
 * 詳細へ進む直前に覚え、一覧へ戻ったときに復元する。
 */
public class CatalogNavigationState {

    private String selectedProductId;
    private int listScroll;

    /** 詳細へ進む直前に、選んだ商品とスクロール位置を覚える。 */
    public void remember(String productId, int scroll) {
        this.selectedProductId = productId;
        this.listScroll = Math.max(0, scroll);
    }

    /** 覚えている選択商品。まだ何も選んでいなければ null。 */
    public String selectedProductId() {
        return this.selectedProductId;
    }

    public int listScroll() {
        return this.listScroll;
    }

    public void clear() {
        this.selectedProductId = null;
        this.listScroll = 0;
    }
}

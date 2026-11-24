package com.minetenant.minetenantclient.catalog;

/** 一点物商品の状態。数量ではなく販売可否だけを持つ。通信上は数値で送り、一度付けた数値は変更しない。 */
public enum ProductStatus {
    AVAILABLE(0),
    SOLD(1);

    private final int id;

    ProductStatus(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }

    /** 未知の数値は購入できない扱い(SOLD)にする。 */
    public static ProductStatus fromId(int id) {
        return id == AVAILABLE.id ? AVAILABLE : SOLD;
    }
}

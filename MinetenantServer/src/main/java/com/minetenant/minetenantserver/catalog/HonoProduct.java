package com.minetenant.minetenantserver.catalog;

/**
 * Honoの商品JSON(Backend契約のProduct)をそのまま受ける内部用の型。Clientへは渡さない。
 * sellerIdやcreatedAtなど使わない項目は、JSONにあっても読み捨てる。
 */
record HonoProduct(String id, String storeId, String name, String description, Long price, String status,
        String category, String theme, String emoji) {
}

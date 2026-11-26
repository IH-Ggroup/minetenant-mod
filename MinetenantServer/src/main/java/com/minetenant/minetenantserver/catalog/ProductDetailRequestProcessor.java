package com.minetenant.minetenantserver.catalog;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minetenant.minetenantserver.network.ProductDetailResultCode;

/**
 * Clientから届いた商品詳細の要求を検証して ProductDetailService に渡す。Minecraftのクラスには依存しない。
 * 状態を持たないので、同時に呼ばれてもリクエストごとの結果が混ざらない。
 * 不正な入力や例外でも落とさず、必ず結果を返す。
 */
public class ProductDetailRequestProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger("minetenantserver");

    public static final int MAX_PRODUCT_ID_LENGTH = 64;

    /** productIdはHonoのpathに入るため、英数字・ハイフン・アンダースコアだけ許可する。 */
    private static final Pattern PRODUCT_ID_FORMAT = Pattern.compile("[A-Za-z0-9_-]{1," + MAX_PRODUCT_ID_LENGTH + "}");

    private final ProductDetailService service;

    public ProductDetailRequestProcessor(ProductDetailService service) {
        this.service = service;
    }

    /** Honoへ送ってよいproductIdか。Hono呼び出し側でも同じ判定を使う。 */
    public static boolean isValidProductId(String productId) {
        return productId != null && PRODUCT_ID_FORMAT.matcher(productId).matches();
    }

    /**
     * @param playerUuid Serverが接続中のplayerから取得したUUID
     * @param productId  Clientから届いた商品ID
     */
    public CompletableFuture<ProductDetailResult> process(UUID playerUuid, String productId) {
        if (playerUuid == null || !isValidProductId(productId)) {
            return CompletableFuture.completedFuture(ProductDetailResult.failure(ProductDetailResultCode.INVALID_REQUEST));
        }

        try {
            CompletableFuture<ProductDetailResult> future = this.service.fetchProduct(productId);
            if (future == null) {
                return CompletableFuture.completedFuture(ProductDetailResult.failure(ProductDetailResultCode.UNKNOWN));
            }

            return future.handle((result, error) -> {
                if (error != null) {
                    LOGGER.error("Product detail service failed", error);
                    return ProductDetailResult.failure(ProductDetailResultCode.UNKNOWN);
                }

                return verified(productId, result);
            });
        } catch (RuntimeException e) {
            LOGGER.error("Product detail service failed", e);
            return CompletableFuture.completedFuture(ProductDetailResult.failure(ProductDetailResultCode.UNKNOWN));
        }
    }

    /** 要求と違う商品が返ってきた場合は、別の商品の情報を渡さないよう失敗にする。 */
    private static ProductDetailResult verified(String productId, ProductDetailResult result) {
        if (result == null) {
            return ProductDetailResult.failure(ProductDetailResultCode.UNKNOWN);
        }

        if (result.isSuccess() && !productId.equals(result.detail().id())) {
            LOGGER.error("Product detail service returned a different product");
            return ProductDetailResult.failure(ProductDetailResultCode.UNKNOWN);
        }

        return result;
    }
}

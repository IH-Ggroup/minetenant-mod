package com.minetenant.minetenantserver.catalog;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minetenant.minetenantserver.network.CatalogResultCode;

/**
 * Clientから届いた商品一覧の要求を検証して CatalogService に渡す。Minecraftのクラスには依存しない。
 * 状態を持たないので、同時に呼ばれてもリクエストごとの結果が混ざらない。
 * 不正な入力や例外でも落とさず、必ず結果を返す。
 */
public class CatalogRequestProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger("minetenantserver");

    public static final int MAX_STORE_ID_LENGTH = 64;

    /** storeIdはHonoのpathに入るため、英数字・ハイフン・アンダースコアだけ許可する。 */
    private static final Pattern STORE_ID_FORMAT = Pattern.compile("[A-Za-z0-9_-]{1," + MAX_STORE_ID_LENGTH + "}");

    private final CatalogService service;
    private final Predicate<String> isRegisteredStore;

    /**
     * @param isRegisteredStore Serverに登録済みのstoreIdか。Clientの申告だけで任意の店舗を引かせないために使う
     */
    public CatalogRequestProcessor(CatalogService service, Predicate<String> isRegisteredStore) {
        this.service = service;
        this.isRegisteredStore = isRegisteredStore;
    }

    /**
     * @param playerUuid Serverが接続中のplayerから取得したUUID
     * @param storeId    Clientから届いた店舗ID
     */
    public CompletableFuture<CatalogResult> process(UUID playerUuid, String storeId) {
        if (playerUuid == null || !isValidStoreId(storeId) || !isRegistered(storeId)) {
            return CompletableFuture.completedFuture(CatalogResult.failure(CatalogResultCode.INVALID_REQUEST));
        }

        try {
            CompletableFuture<CatalogResult> future = this.service.fetchProducts(storeId);
            if (future == null) {
                return CompletableFuture.completedFuture(CatalogResult.failure(CatalogResultCode.UNKNOWN));
            }

            return future.handle((result, error) -> {
                if (error != null) {
                    LOGGER.error("Catalog service failed", error);
                    return CatalogResult.failure(CatalogResultCode.UNKNOWN);
                }

                return result == null ? CatalogResult.failure(CatalogResultCode.UNKNOWN) : result;
            });
        } catch (RuntimeException e) {
            LOGGER.error("Catalog service failed", e);
            return CompletableFuture.completedFuture(CatalogResult.failure(CatalogResultCode.UNKNOWN));
        }
    }

    private boolean isRegistered(String storeId) {
        try {
            return this.isRegisteredStore.test(storeId);
        } catch (RuntimeException e) {
            LOGGER.error("Store registry check failed", e);
            return false;
        }
    }

    private static boolean isValidStoreId(String storeId) {
        return storeId != null && STORE_ID_FORMAT.matcher(storeId).matches();
    }
}

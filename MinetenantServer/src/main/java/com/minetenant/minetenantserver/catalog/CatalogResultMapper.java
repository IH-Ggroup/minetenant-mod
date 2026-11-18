package com.minetenant.minetenantserver.catalog;

import java.util.List;

import com.minetenant.minetenantserver.http.ApiError;
import com.minetenant.minetenantserver.http.ApiResult;
import com.minetenant.minetenantserver.network.CatalogResultCode;

/** Honoの呼び出し結果を CatalogResult に変換する。Hono実装の CatalogService から使う。 */
public final class CatalogResultMapper {

    private CatalogResultMapper() {
    }

    public static CatalogResult from(ApiResult<List<ProductSummary>> result) {
        if (result instanceof ApiResult.Success<List<ProductSummary>> success) {
            return CatalogResult.success(success.data());
        }

        return CatalogResult.failure(codeOf(((ApiResult.Failure<List<ProductSummary>>) result).error()));
    }

    static CatalogResultCode codeOf(ApiError error) {
        return switch (error.kind()) {
            case UNAUTHORIZED -> CatalogResultCode.UNAUTHORIZED;
            case NOT_FOUND -> CatalogResultCode.STORE_NOT_FOUND;
            case SERVER_ERROR -> CatalogResultCode.SERVER_ERROR;
            case TIMEOUT -> CatalogResultCode.TIMEOUT;
            default -> CatalogResultCode.UNKNOWN;
        };
    }
}

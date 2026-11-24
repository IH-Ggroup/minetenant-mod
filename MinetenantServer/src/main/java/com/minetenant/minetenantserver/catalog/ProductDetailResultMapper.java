package com.minetenant.minetenantserver.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minetenant.minetenantserver.http.ApiError;
import com.minetenant.minetenantserver.http.ApiResult;
import com.minetenant.minetenantserver.network.ProductDetailResultCode;

/** Honoの呼び出し結果を ProductDetailResult に変換する。例外は投げない。 */
final class ProductDetailResultMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger("minetenantserver");

    private ProductDetailResultMapper() {
    }

    static ProductDetailResult from(ApiResult<HonoProduct> result) {
        if (result instanceof ApiResult.Success<HonoProduct> success) {
            ProductDetail detail = toDetail(success.data());
            return detail == null
                    ? ProductDetailResult.failure(ProductDetailResultCode.UNKNOWN)
                    : ProductDetailResult.success(detail);
        }

        return ProductDetailResult.failure(codeOf(((ApiResult.Failure<HonoProduct>) result).error()));
    }

    static ProductDetailResultCode codeOf(ApiError error) {
        return switch (error.kind()) {
            case UNAUTHORIZED -> ProductDetailResultCode.UNAUTHORIZED;
            case NOT_FOUND -> ProductDetailResultCode.PRODUCT_NOT_FOUND;
            case SERVER_ERROR -> ProductDetailResultCode.SERVER_ERROR;
            case TIMEOUT -> ProductDetailResultCode.TIMEOUT;
            default -> ProductDetailResultCode.UNKNOWN;
        };
    }

    /** 必須項目の欠落や未知のstatusなど、契約に合わない応答は null にする。 */
    private static ProductDetail toDetail(HonoProduct product) {
        ProductStatus status = statusOf(product.status());
        if (status == null || product.price() == null) {
            LOGGER.warn("Hono product response does not match the contract");
            return null;
        }

        try {
            return new ProductDetail(product.id(), product.storeId(), product.name(), product.description(),
                    product.price(), status, product.category(), product.theme(), product.emoji());
        } catch (RuntimeException e) {
            LOGGER.warn("Hono product response does not match the contract");
            return null;
        }
    }

    private static ProductStatus statusOf(String value) {
        if ("available".equals(value)) {
            return ProductStatus.AVAILABLE;
        }
        if ("sold".equals(value)) {
            return ProductStatus.SOLD;
        }
        return null;
    }
}

package com.minetenant.minetenantserver.http;

/** API呼び出しの結果。成功時はdata、失敗時はApiErrorを持つ。 */
public sealed interface ApiResult<T> {

    record Success<T>(T data) implements ApiResult<T> {
    }

    record Failure<T>(ApiError error) implements ApiResult<T> {
    }
}

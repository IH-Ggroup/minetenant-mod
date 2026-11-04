package com.minetenant.minetenantserver.http;

/** API呼び出しの失敗理由。messageは固定文言で、URL・本文・tokenを含めない。 */
public record ApiError(Kind kind, int status) {

    public enum Kind {
        NOT_FOUND("Resource not found"),
        SERVER_ERROR("API server error"),
        HTTP_ERROR("Unexpected HTTP status"),
        INVALID_JSON("Invalid JSON response"),
        TIMEOUT("API request timed out"),
        NETWORK("API connection failed"),
        INVALID_REQUEST("Invalid API request"),
        REJECTED("API request rejected");

        private final String message;

        Kind(String message) {
            this.message = message;
        }
    }

    public static ApiError of(Kind kind) {
        return new ApiError(kind, 0);
    }

    public String message() {
        return status > 0 ? kind.message + " (" + status + ")" : kind.message;
    }
}

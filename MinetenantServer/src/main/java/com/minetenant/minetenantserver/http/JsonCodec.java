package com.minetenant.minetenantserver.http;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/** `{ "data": T }` 形式のレスポンス本文を指定した型へ変換する。 */
public final class JsonCodec {

    private static final Gson GSON = new Gson();

    private JsonCodec() {
    }

    public static <T> ApiResult<T> decode(String body, Class<T> type) {
        return decode(body, (Type) type);
    }

    @SuppressWarnings("unchecked")
    public static <T> ApiResult<T> decode(String body, Type type) {
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject()) {
                return invalid();
            }
            JsonObject object = root.getAsJsonObject();
            JsonElement data = object.get("data");
            if (data == null || data.isJsonNull()) {
                return invalid();
            }
            T value = (T) GSON.fromJson(data, type);
            return value == null ? invalid() : new ApiResult.Success<>(value);
        } catch (JsonParseException e) {
            return invalid();
        }
    }

    /** リクエスト本文をJSON文字列にする。 */
    public static String encode(Object body) {
        return GSON.toJson(body);
    }

    /** エラー応答の `code` と `message` を読む。形式が違っても例外にせず、取れない値はnullにする。 */
    public static ErrorBody decodeError(String body) {
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject()) {
                return new ErrorBody(null, null);
            }
            JsonObject object = root.getAsJsonObject();
            return new ErrorBody(text(object.get("code")), text(object.get("message")));
        } catch (JsonParseException e) {
            return new ErrorBody(null, null);
        }
    }

    public record ErrorBody(String code, String message) {
    }

    private static String text(JsonElement element) {
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static <T> ApiResult<T> invalid() {
        return new ApiResult.Failure<>(ApiError.of(ApiError.Kind.INVALID_JSON));
    }
}

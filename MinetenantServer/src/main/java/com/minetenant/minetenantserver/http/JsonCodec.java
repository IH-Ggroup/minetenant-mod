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

    private static <T> ApiResult<T> invalid() {
        return new ApiResult.Failure<>(ApiError.of(ApiError.Kind.INVALID_JSON));
    }
}

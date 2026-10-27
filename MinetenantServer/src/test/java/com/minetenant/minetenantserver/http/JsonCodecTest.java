package com.minetenant.minetenantserver.http;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.reflect.TypeToken;

class JsonCodecTest {

    record Item(String id, int price) {
    }

    private static ApiError errorOf(ApiResult<?> result) {
        return assertInstanceOf(ApiResult.Failure.class, result).error();
    }

    @Test
    void dataを指定した型へ変換できる() {
        ApiResult<Item> r = JsonCodec.decode("{\"data\":{\"id\":\"a\",\"price\":3}}", Item.class);
        assertEquals(new Item("a", 3), assertInstanceOf(ApiResult.Success.class, r).data());
    }

    @Test
    void 配列もTypeTokenで変換できる() {
        ApiResult<List<Item>> r = JsonCodec.decode("{\"data\":[{\"id\":\"a\",\"price\":1}]}",
                new TypeToken<List<Item>>() { }.getType());
        assertEquals(List.of(new Item("a", 1)), assertInstanceOf(ApiResult.Success.class, r).data());
    }

    @Test
    void 壊れたJSONはINVALID_JSON() {
        assertEquals(ApiError.Kind.INVALID_JSON, errorOf(JsonCodec.decode("{not json", Item.class)).kind());
    }

    @Test
    void dataがないJSONはINVALID_JSON() {
        assertEquals(ApiError.Kind.INVALID_JSON, errorOf(JsonCodec.decode("{\"x\":1}", Item.class)).kind());
    }

    @Test
    void dataがnullのJSONはINVALID_JSON() {
        assertEquals(ApiError.Kind.INVALID_JSON, errorOf(JsonCodec.decode("{\"data\":null}", Item.class)).kind());
    }

    @Test
    void 型が合わないdataはINVALID_JSON() {
        assertEquals(ApiError.Kind.INVALID_JSON,
                errorOf(JsonCodec.decode("{\"data\":{\"id\":\"a\",\"price\":\"x\"}}", Item.class)).kind());
    }

    @Test
    void 空文字やJSONでない本文もINVALID_JSON() {
        assertEquals(ApiError.Kind.INVALID_JSON, errorOf(JsonCodec.decode("", Item.class)).kind());
        assertEquals(ApiError.Kind.INVALID_JSON, errorOf(JsonCodec.decode("[1,2]", Item.class)).kind());
    }

    @Test
    void エラーメッセージに本文を含めない() {
        String message = errorOf(JsonCodec.decode("{secret-body", Item.class)).message();
        assertFalse(message.contains("secret-body"));
    }
}

package com.minetenant.minetenantserver.config;

/** 設定値が不足・不正なときに投げる。メッセージにtoken値は含めない。 */
public class HonoConfigException extends RuntimeException {

    public HonoConfigException(String message) {
        super(message);
    }

    public HonoConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}

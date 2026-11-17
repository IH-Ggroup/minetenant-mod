package com.minetenant.minetenantclient.link;

/** 連携コードの形式検証。本番の形式が決まったら実装だけ差し替える。 */
public interface LinkCodeValidator {

    Result validate(String raw);

    /** 検証結果。正常なら整形済みのコード、不正ならエラーを持つ。 */
    record Result(String code, LinkError error) {

        public static Result ok(String code) {
            return new Result(code, null);
        }

        public static Result invalid(LinkError error) {
            return new Result(null, error);
        }

        public boolean isValid() {
            return this.error == null;
        }
    }
}

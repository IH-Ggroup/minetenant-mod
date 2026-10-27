package com.minetenant.minetenantserver.http;

/**
 * POSTの結果。副作用があるため、成功・業務エラー・結果不明・未送信を型で分ける。
 * Unknownは「処理されたかどうか分からない」状態で、失敗と決めつけず、同じrequestIdで再確認・再送すること。
 */
public sealed interface PostOutcome<T> {

    /** 200/201でdataを変換できた。 */
    record Success<T>(T data) implements PostOutcome<T> {
    }

    /** Honoが応答した確定済みのエラー。codeは本文にあるときだけ入る(401・500などはnull)。 */
    record BusinessError<T>(int status, String code, String message) implements PostOutcome<T> {

        /** messageは利用者入力を含みうるため、toStringに出さない。 */
        @Override
        public String toString() {
            return "BusinessError[status=" + status + ", code=" + code + "]";
        }
    }

    /** timeoutや送信後の切断、成功statusなのに本文が不正など、結果が分からない。 */
    record Unknown<T>(ApiError.Kind reason) implements PostOutcome<T> {
    }

    /** リクエストが送られていない。再送しても二重実行にならない。 */
    record NotSent<T>(ApiError.Kind reason) implements PostOutcome<T> {
    }
}

package com.minetenant.minetenantclient.link;

import java.util.function.Consumer;

/**
 * 連携コードの送信先。通常は LinkPacketClient(packet送信)、画面確認やテストでは FixtureLinkSubmitter を使う。
 * onResult は、実装側が呼び出し元(GUI)のスレッドで呼ぶこと。
 */
public interface LinkSubmitter {

    void submit(String code, Consumer<Outcome> onResult);

    /** 送信の結果。失敗なら理由を持つ。 */
    record Outcome(LinkError error) {

        public static Outcome success() {
            return new Outcome(null);
        }

        public static Outcome failure(LinkError error) {
            return new Outcome(error == null ? LinkError.UNKNOWN : error);
        }

        public boolean isSuccess() {
            return this.error == null;
        }
    }
}

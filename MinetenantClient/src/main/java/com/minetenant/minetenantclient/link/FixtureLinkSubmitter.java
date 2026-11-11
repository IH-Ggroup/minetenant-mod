package com.minetenant.minetenantclient.link;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * 画面確認用の仮の送信先。通信はせず、少し待ってから入力値に応じた結果を返す。
 * <ul>
 *   <li>FAIL で始まる: コード拒否(LINK-003)</li>
 *   <li>ERROR で始まる: 不明なエラー(LINK-999)</li>
 *   <li>それ以外: 成功</li>
 * </ul>
 * packet送信ができたら LinkSubmitter の実装ごと差し替える。
 */
public class FixtureLinkSubmitter implements LinkSubmitter {

    /** 送信中の表示を確認できる程度の待ち時間(2秒) */
    public static final int DELAY_TICKS = 40;

    private final Scheduler scheduler;

    public FixtureLinkSubmitter(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void submit(String code, Consumer<Outcome> onResult) {
        Outcome outcome = this.decide(code);
        this.scheduler.runLater(DELAY_TICKS, () -> onResult.accept(outcome));
    }

    private Outcome decide(String code) {
        String upper = code.toUpperCase(Locale.ROOT);

        if (upper.startsWith("FAIL")) {
            return Outcome.failure(LinkError.CODE_REJECTED);
        }

        if (upper.startsWith("ERROR")) {
            return Outcome.failure(LinkError.UNKNOWN);
        }

        return Outcome.success();
    }
}

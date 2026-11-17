package com.minetenant.minetenantclient.link;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.minetenant.minetenantclient.network.LinkResultCode;

/**
 * 連携コードをServerへ送り、同じrequest IDの結果をcallbackへ返す LinkSubmitter。
 * Minecraftのクラスには依存しない(実際の送信は Transport が担当)。
 * クライアントスレッドからだけ使う。
 */
public class LinkPacketClient implements LinkSubmitter {

    /** 応答待ちの上限(10秒)。これを過ぎたら不明なエラーとして返す。 */
    public static final int TIMEOUT_TICKS = 200;

    /** packetの送信口。 */
    public interface Transport {

        /** Server側が連携packetを受け付けられる状態か(接続中で、Server MODが入っているか)。 */
        boolean canSend();

        void send(long requestId, String code);
    }

    private final Transport transport;
    private final Scheduler scheduler;
    private final Map<Long, Consumer<Outcome>> pending = new HashMap<>();
    private long nextRequestId = 1;

    public LinkPacketClient(Transport transport, Scheduler scheduler) {
        this.transport = transport;
        this.scheduler = scheduler;
    }

    @Override
    public void submit(String code, Consumer<Outcome> onResult) {
        if (!this.transport.canSend()) {
            onResult.accept(Outcome.failure(LinkError.UNKNOWN));
            return;
        }

        long requestId = this.nextRequestId++;
        this.pending.put(requestId, onResult);

        try {
            this.transport.send(requestId, code);
        } catch (RuntimeException e) {
            this.pending.remove(requestId);
            onResult.accept(Outcome.failure(LinkError.UNKNOWN));
            return;
        }

        this.scheduler.runLater(TIMEOUT_TICKS, () -> this.complete(requestId, LinkResultCode.UNKNOWN));
    }

    /** Serverからの結果を受けたときに呼ぶ。待っていないrequest IDは無視する。 */
    public void onResponse(long requestId, LinkResultCode result) {
        this.complete(requestId, result);
    }

    /** 切断時などに、待機中の要求を結果なしで捨てる。 */
    public void reset() {
        this.pending.clear();
    }

    private void complete(long requestId, LinkResultCode result) {
        Consumer<Outcome> callback = this.pending.remove(requestId);

        if (callback == null) {
            return;
        }

        callback.accept(toOutcome(result));
    }

    private static Outcome toOutcome(LinkResultCode result) {
        return switch (result) {
            case SUCCESS -> Outcome.success();
            case CODE_REJECTED -> Outcome.failure(LinkError.CODE_REJECTED);
            case UNKNOWN -> Outcome.failure(LinkError.UNKNOWN);
        };
    }
}

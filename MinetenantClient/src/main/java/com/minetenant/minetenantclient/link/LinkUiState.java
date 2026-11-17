package com.minetenant.minetenantclient.link;

/**
 * 連携コード入力画面の状態。GUIのスレッドからだけ使う。
 * 入力中の文字列は持たない(コードをここに残さないため)。
 */
public class LinkUiState {

    public enum Phase {
        IDLE, LOADING, SUCCESS, ERROR
    }

    private final LinkCodeValidator validator;
    private final LinkSubmitter submitter;

    private Phase phase = Phase.IDLE;
    private LinkError error;
    /** 古い結果callbackを捨てるための世代番号 */
    private int generation;

    public LinkUiState(LinkCodeValidator validator, LinkSubmitter submitter) {
        this.validator = validator;
        this.submitter = submitter;
    }

    public Phase phase() {
        return this.phase;
    }

    /** ERROR のときだけ理由を返す。それ以外は null。 */
    public LinkError error() {
        return this.error;
    }

    /**
     * 検証して送信する。送信中は何もしない。
     *
     * @return 送信を開始したら true
     */
    public boolean submit(String raw) {
        if (this.phase == Phase.LOADING) {
            return false;
        }

        LinkCodeValidator.Result result = this.validator.validate(raw);

        if (!result.isValid()) {
            this.phase = Phase.ERROR;
            this.error = result.error();
            return false;
        }

        // callbackが同期で返っても結果状態になるよう、先にLOADINGにする
        this.phase = Phase.LOADING;
        this.error = null;
        int current = ++this.generation;

        this.submitter.submit(result.code(), outcome -> this.onResult(current, outcome));
        return true;
    }

    /** 画面を閉じるときなどにIDLEへ戻す。送信中の結果は捨てる。 */
    public void reset() {
        this.generation++;
        this.phase = Phase.IDLE;
        this.error = null;
    }

    private void onResult(int expectedGeneration, LinkSubmitter.Outcome outcome) {
        if (expectedGeneration != this.generation || this.phase != Phase.LOADING) {
            return;
        }

        if (outcome.isSuccess()) {
            this.phase = Phase.SUCCESS;
            this.error = null;
        } else {
            this.phase = Phase.ERROR;
            this.error = outcome.error();
        }
    }
}

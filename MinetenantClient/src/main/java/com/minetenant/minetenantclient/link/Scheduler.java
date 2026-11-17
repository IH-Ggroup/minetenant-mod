package com.minetenant.minetenantclient.link;

/** 指定tick後に処理を実行する。テストでは手動実行の偽物に差し替える。 */
public interface Scheduler {

    /** delayTicks tick後に task を実行する。実行はクライアントスレッドで行うこと。 */
    void runLater(int delayTicks, Runnable task);
}

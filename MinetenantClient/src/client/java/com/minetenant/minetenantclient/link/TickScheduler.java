package com.minetenant.minetenantclient.link;

import java.util.ArrayList;
import java.util.List;

/**
 * クライアントtickで動くScheduler。onTick を END_CLIENT_TICK から呼ぶ。
 * 処理はクライアントスレッドで実行されるので、GUIの状態をそのまま更新できる。
 */
public class TickScheduler implements Scheduler {

    public static final TickScheduler INSTANCE = new TickScheduler();

    private static class Entry {
        int remaining;
        final Runnable task;

        Entry(int remaining, Runnable task) {
            this.remaining = remaining;
            this.task = task;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    private TickScheduler() {
    }

    @Override
    public void runLater(int delayTicks, Runnable task) {
        this.entries.add(new Entry(Math.max(0, delayTicks), task));
    }

    public void onTick() {
        if (this.entries.isEmpty()) {
            return;
        }

        List<Runnable> due = new ArrayList<>();

        this.entries.removeIf(entry -> {
            if (--entry.remaining <= 0) {
                due.add(entry.task);
                return true;
            }
            return false;
        });

        due.forEach(Runnable::run);
    }
}

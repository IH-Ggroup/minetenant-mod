package com.minetenant.minetenantclient.debug;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * デバッグ画面に表示するログの保持先。
 * 描画方法に依存しないので、表示側(自前描画 / WidgetListBase)を差し替えても使える。
 * static なので、画面を閉じて開き直してもログが残る。
 */
public final class DebugLog {

    public enum Level { INFO, SUCCESS, ERROR }

    public record Entry(String time, Level level, String text) {
        public String format() {
            return "[" + time + "] " + text;
        }
    }

    private static final int MAX_ENTRIES = 100;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private DebugLog() {
    }

    public static void add(Level level, String text) {
        ENTRIES.add(new Entry(LocalTime.now().format(TIME_FORMAT), level, text));

        while (ENTRIES.size() > MAX_ENTRIES) {
            ENTRIES.remove(0);
        }
    }

    public static List<Entry> getAll() {
        return List.copyOf(ENTRIES);
    }

    public static void clear() {
        ENTRIES.clear();
    }
}

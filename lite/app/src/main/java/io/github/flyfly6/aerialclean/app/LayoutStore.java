package io.github.flyfly6.aerialclean.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 布局偏好。键与默认值见 design.md 5.2；解析/序列化都是静态纯函数，便于单测。
 */
public final class LayoutStore {

    public static final int MIN_SLOTS = 4;
    public static final int MAX_SLOTS = 6;
    public static final int DEFAULT_SLOTS = 5;
    public static final long DEFAULT_SAVER_IDLE_MS = 60_000L;

    private static final String PREFS = "layout";
    private static final String KEY_DOCK = "dock";
    private static final String KEY_HIDDEN = "hidden";
    private static final String KEY_COLUMNS = "columns";
    private static final String KEY_DOCK_COUNT = "dock_count";
    private static final String KEY_SHOW_CLOCK = "show_clock";
    private static final String KEY_SAVER_IDLE_MS = "saver_idle_ms";

    /** 用 commit 而不是 apply：A5 的持久化验收会 force-stop 再拉起，异步落盘可能丢写入。 */
    private final SharedPreferences prefs;

    public LayoutStore(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<String> dock() {
        return parseDock(prefs.getString(KEY_DOCK, ""));
    }

    public void setDock(List<String> pkgs) {
        prefs.edit().putString(KEY_DOCK, joinDock(pkgs)).commit();
    }

    public Set<String> hidden() {
        return new LinkedHashSet<>(prefs.getStringSet(KEY_HIDDEN, Collections.<String>emptySet()));
    }

    public void setHidden(Collection<String> pkgs) {
        prefs.edit().putStringSet(KEY_HIDDEN, new LinkedHashSet<>(pkgs)).commit();
    }

    public int columns() {
        return clampSlots(prefs.getInt(KEY_COLUMNS, DEFAULT_SLOTS));
    }

    public int dockCount() {
        return clampSlots(prefs.getInt(KEY_DOCK_COUNT, DEFAULT_SLOTS));
    }

    public boolean showClock() {
        return prefs.getBoolean(KEY_SHOW_CLOCK, true);
    }

    public long saverIdleMs() {
        // design.md 里这个键是 int；用 getLong 读到 int 会抛 ClassCastException。
        long value = prefs.getInt(KEY_SAVER_IDLE_MS, (int) DEFAULT_SAVER_IDLE_MS);
        return value > 0 ? value : DEFAULT_SAVER_IDLE_MS;
    }

    public void set(int columns, int dockCount, boolean showClock) {
        prefs.edit()
                .putInt(KEY_COLUMNS, clampSlots(columns))
                .putInt(KEY_DOCK_COUNT, clampSlots(dockCount))
                .putBoolean(KEY_SHOW_CLOCK, showClock)
                .commit();
    }

    static int clampSlots(int value) {
        if (value < MIN_SLOTS) {
            return MIN_SLOTS;
        }
        return Math.min(value, MAX_SLOTS);
    }

    static List<String> parseDock(String raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(raw.split(",")));
    }

    static String joinDock(List<String> pkgs) {
        StringBuilder builder = new StringBuilder();
        for (String pkg : pkgs) {
            if (pkg == null || pkg.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(pkg);
        }
        return builder.toString();
    }
}

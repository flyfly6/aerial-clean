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
    public static final int MIN_COLUMNS = 4;
    public static final int MAX_COLUMNS = 8;
    public static final int DEFAULT_SLOTS = 5;
    public static final long DEFAULT_SAVER_IDLE_MS = 60_000L;

    private static final String PREFS = "layout";
    private static final String KEY_DOCK = "dock";
    private static final String KEY_HIDDEN = "hidden";
    private static final String KEY_COLUMNS = "columns";
    private static final String KEY_DOCK_COUNT = "dock_count";
    private static final String KEY_SHOW_CLOCK = "show_clock";
    private static final String KEY_SAVER_IDLE_MS = "saver_idle_ms";
    private static final String KEY_WALLPAPER_SOURCE = "wallpaper_source";
    private static final String KEY_WALLPAPER_RES = "wallpaper_res";
    private static final String KEY_BUILTIN_INDEX = "builtin_index";
    private static final String KEY_DOCK_STYLE = "dock_style";

    public static final int DEFAULT_WALLPAPER_SOURCE = 0; // 0=必应, 1=内置, 2=纯渐变
    public static final int DEFAULT_WALLPAPER_RES = 1;    // 0=720p, 1=1080p, 2=4K
    public static final int DEFAULT_BUILTIN_INDEX = 0;
    public static final int DEFAULT_DOCK_STYLE = 0;       // 0=毛玻璃, 1=半透黑, 2=极简

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

    public int wallpaperSource() {
        return prefs.getInt(KEY_WALLPAPER_SOURCE, DEFAULT_WALLPAPER_SOURCE);
    }

    public void setWallpaperSource(int source) {
        prefs.edit().putInt(KEY_WALLPAPER_SOURCE, source).commit();
    }

    public int wallpaperResolution() {
        return prefs.getInt(KEY_WALLPAPER_RES, DEFAULT_WALLPAPER_RES);
    }

    public void setWallpaperResolution(int resTier) {
        prefs.edit().putInt(KEY_WALLPAPER_RES, resTier).commit();
    }

    public int builtInIndex() {
        return prefs.getInt(KEY_BUILTIN_INDEX, DEFAULT_BUILTIN_INDEX);
    }

    public void setBuiltInIndex(int index) {
        prefs.edit().putInt(KEY_BUILTIN_INDEX, index).commit();
    }

    public int dockStyle() {
        return prefs.getInt(KEY_DOCK_STYLE, DEFAULT_DOCK_STYLE);
    }

    public void setDockStyle(int style) {
        prefs.edit().putInt(KEY_DOCK_STYLE, style).commit();
    }

    public void setColumns(int columns) {
        prefs.edit().putInt(KEY_COLUMNS, Math.max(MIN_COLUMNS, Math.min(columns, MAX_COLUMNS))).commit();
    }

    public void setDockCount(int dockCount) {
        prefs.edit().putInt(KEY_DOCK_COUNT, clampSlots(dockCount)).commit();
    }

    public void setShowClock(boolean showClock) {
        prefs.edit().putBoolean(KEY_SHOW_CLOCK, showClock).commit();
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

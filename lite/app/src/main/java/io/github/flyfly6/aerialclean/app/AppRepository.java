package io.github.flyfly6.aerialclean.app;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 应用枚举与摆放的纯数据层：合并、去重、排除自身、过滤隐藏、拆成网格与 dock。
 *
 * <p>不碰 {@code PackageManager}，设备侧通过 {@link AppSource} 注入，这样纯 JVM 单测能覆盖全部规则。
 */
public final class AppRepository {

    /** 4.4 上没有 {@code Intent.CATEGORY_LEANBACK_LAUNCHER}（21 才有），只用字符串常量。 */
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    public static final String CATEGORY_LEANBACK_LAUNCHER = "android.intent.category.LEANBACK_LAUNCHER";

    public interface AppSource {
        /** 返回该 category 下所有可启动的 Activity（未去重、未排序）。 */
        List<AppEntry> query(String category);
    }

    private final AppSource source;
    private final String selfPackage;

    public AppRepository(AppSource source, String selfPackage) {
        this.source = source;
        this.selfPackage = selfPackage;
    }

    /** 全部可启动应用：去重、排除自身，未过滤隐藏，未排序。 */
    public List<AppEntry> queryAll() {
        return merge(source.query(CATEGORY_LAUNCHER), source.query(CATEGORY_LEANBACK_LAUNCHER), selfPackage);
    }

    public static List<AppEntry> merge(List<AppEntry> launcher, List<AppEntry> leanback, String selfPackage) {
        Map<String, AppEntry> byPkg = new LinkedHashMap<>();
        for (AppEntry entry : launcher) {
            if (!entry.pkg.equals(selfPackage)) {
                byPkg.put(entry.pkg, entry);
            }
        }
        for (AppEntry entry : leanback) {
            // 同包时保留 LAUNCHER 那条：它的 intent 一定带 CATEGORY_LAUNCHER，启动路径最短。
            if (!entry.pkg.equals(selfPackage) && !byPkg.containsKey(entry.pkg)) {
                byPkg.put(entry.pkg, entry);
            }
        }
        return new ArrayList<>(byPkg.values());
    }

    public static List<AppEntry> visible(List<AppEntry> all, Set<String> hidden) {
        if (hidden.isEmpty()) {
            return new ArrayList<>(all);
        }
        List<AppEntry> result = new ArrayList<>(all.size());
        for (AppEntry entry : all) {
            if (!hidden.contains(entry.pkg)) {
                result.add(entry);
            }
        }
        return result;
    }

    /** dock 内容：按保存的顺序挑选有效应用，若不足 dockCount 则从 all 自动补齐，最多 {@code dockCount} 个。 */
    public static List<AppEntry> dock(List<AppEntry> all, List<String> dockPkgs, int dockCount) {
        Map<String, AppEntry> byPkg = new HashMap<>();
        for (AppEntry entry : all) {
            byPkg.put(entry.pkg, entry);
        }
        List<AppEntry> result = new ArrayList<>(dockCount);
        Set<String> added = new java.util.HashSet<>();
        if (dockPkgs != null) {
            for (String pkg : dockPkgs) {
                if (result.size() >= dockCount) {
                    break;
                }
                AppEntry entry = byPkg.get(pkg);
                if (entry != null && !added.contains(pkg)) {
                    result.add(entry);
                    added.add(pkg);
                }
            }
        }
        // 如果有效应用数依然不足 dockCount，从 all 中挑选未添加的应用自动补齐
        if (result.size() < dockCount) {
            for (AppEntry entry : all) {
                if (result.size() >= dockCount) {
                    break;
                }
                if (!added.contains(entry.pkg)) {
                    result.add(entry);
                    added.add(entry.pkg);
                }
            }
        }
        return result;
    }

    /** 网格内容：排除 dock 内的应用，按 label 排序。 */
    public static List<AppEntry> grid(List<AppEntry> all, List<AppEntry> docked, Comparator<String> labelOrder) {
        Set<String> dockedPkgs = new java.util.HashSet<>();
        for (AppEntry entry : docked) {
            dockedPkgs.add(entry.pkg);
        }
        List<AppEntry> result = new ArrayList<>(Math.max(0, all.size() - dockedPkgs.size()));
        for (AppEntry entry : all) {
            if (!dockedPkgs.contains(entry.pkg)) {
                result.add(entry);
            }
        }
        Collections.sort(result, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry left, AppEntry right) {
                return labelOrder.compare(String.valueOf(left.label), String.valueOf(right.label));
            }
        });
        return result;
    }

    /** 生产用的 label 排序：跟随系统语言，中文按拼音、英文按字母。 */
    public static Comparator<String> labelOrder() {
        final Collator collator = Collator.getInstance(Locale.getDefault());
        return new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                return collator.compare(left, right);
            }
        };
    }

    /** 首次启动的默认 dock：按 label 取前 {@code dockCount} 个。 */
    public static List<String> defaultDock(List<AppEntry> all, int dockCount) {
        List<AppEntry> sorted = grid(all, Collections.<AppEntry>emptyList(), labelOrder());
        List<String> result = new ArrayList<>(dockCount);
        for (AppEntry entry : sorted) {
            if (result.size() >= dockCount) {
                break;
            }
            result.add(entry.pkg);
        }
        return result;
    }
}

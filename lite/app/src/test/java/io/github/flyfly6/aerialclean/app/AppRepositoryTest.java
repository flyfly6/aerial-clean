package io.github.flyfly6.aerialclean.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class AppRepositoryTest {

    private static final String SELF = "io.github.flyfly6.aerialclean";

    private static AppEntry app(String pkg, String label) {
        return new AppEntry(pkg, pkg + "/.Main", label, false);
    }

    private static List<String> pkgs(List<AppEntry> entries) {
        List<String> result = new ArrayList<>();
        for (AppEntry entry : entries) {
            result.add(entry.pkg);
        }
        return result;
    }

    private static final Comparator<String> BY_LABEL = new Comparator<String>() {
        @Override
        public int compare(String left, String right) {
            return left.compareTo(right);
        }
    };

    @Test
    public void mergeDedupesByPackageAndDropsSelf() {
        List<AppEntry> launcher = Arrays.asList(
                app(SELF, "AerialClean"),
                app("com.only.launcher", "LauncherOnly"),
                app("com.both", "BothFromLauncher"));
        List<AppEntry> leanback = Arrays.asList(
                app("com.only.leanback", "LeanbackOnly"),
                app("com.both", "BothFromLeanback"));

        List<AppEntry> merged = AppRepository.merge(launcher, leanback, SELF);

        assertEquals(Arrays.asList("com.only.launcher", "com.both", "com.only.leanback"), pkgs(merged));
        // 同包保留 LAUNCHER 那条
        assertEquals("BothFromLauncher", merged.get(1).label.toString());
    }

    @Test
    public void visibleDropsHiddenPackages() {
        List<AppEntry> all = Arrays.asList(app("com.a", "A"), app("com.b", "B"));
        assertEquals(Collections.singletonList("com.b"),
                pkgs(AppRepository.visible(all, new HashSet<>(Collections.singletonList("com.a")))));
        // 空集合时原样返回
        assertEquals(Arrays.asList("com.a", "com.b"), pkgs(AppRepository.visible(all, Collections.<String>emptySet())));
    }

    @Test
    public void dockFollowsSavedOrderSkipsMissingAndTruncates() {
        List<AppEntry> all = Arrays.asList(app("com.a", "A"), app("com.b", "B"), app("com.c", "C"));
        List<String> saved = Arrays.asList("com.c", "com.gone", "com.a", "com.b");

        assertEquals(Arrays.asList("com.c", "com.a", "com.b"), pkgs(AppRepository.dock(all, saved, 5)));
        assertEquals(Arrays.asList("com.c", "com.a"), pkgs(AppRepository.dock(all, saved, 2)));
    }

    @Test
    public void dockFillsFromAllWhenSavedIsShorterThanCount() {
        List<AppEntry> all = Arrays.asList(app("com.a", "A"), app("com.b", "B"), app("com.c", "C"));
        List<String> saved = Collections.singletonList("com.c");

        // saved 只有 1 个，但要求 3 个槽位，自动补齐剩余 2 个应用
        assertEquals(Arrays.asList("com.c", "com.a", "com.b"), pkgs(AppRepository.dock(all, saved, 3)));
    }

    @Test
    public void dockWithoutCountYieldsEmpty() {
        List<AppEntry> all = Collections.singletonList(app("com.a", "A"));
        assertEquals(Collections.emptyList(), pkgs(AppRepository.dock(all, Collections.singletonList("com.a"), 0)));
    }

    @Test
    public void gridExcludesDockedAndSortsByLabel() {
        List<AppEntry> all = Arrays.asList(app("com.z", "Zebra"), app("com.d", "Dock"), app("com.a", "Apple"));
        List<AppEntry> docked = Collections.singletonList(app("com.d", "Dock"));

        assertEquals(Arrays.asList("com.a", "com.z"), pkgs(AppRepository.grid(all, docked, BY_LABEL)));
        // docked 里的包不在 all 时不应出错
        assertEquals(Arrays.asList("com.a", "com.d", "com.z"),
                pkgs(AppRepository.grid(all, Collections.singletonList(app("com.other", "Other")), BY_LABEL)));
    }

    @Test
    public void defaultDockTakesFirstByLabel() {
        List<AppEntry> all = Arrays.asList(app("com.z", "Zebra"), app("com.a", "Apple"), app("com.m", "Mango"));
        assertEquals(Collections.singletonList("com.a"), AppRepository.defaultDock(all, 1));
        assertEquals(Collections.emptyList(), AppRepository.defaultDock(all, 0));
    }
}

package io.github.flyfly6.aerialclean.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 只测纯函数：SharedPreferences 相关路径由设备验收覆盖。
 */
public class LayoutStoreTest {

    @Test
    public void clampSlotsKeepsFourToSix() {
        assertEquals(4, LayoutStore.clampSlots(1));
        assertEquals(4, LayoutStore.clampSlots(4));
        assertEquals(5, LayoutStore.clampSlots(5));
        assertEquals(6, LayoutStore.clampSlots(6));
        assertEquals(6, LayoutStore.clampSlots(9));
    }

    @Test
    public void parseDockSplitsAndPreservesOrder() {
        assertTrue(LayoutStore.parseDock("").isEmpty());
        assertTrue(LayoutStore.parseDock(null).isEmpty());
        assertEquals(Arrays.asList("c.b", "a.b"), LayoutStore.parseDock("c.b,a.b"));
    }

    @Test
    public void dockRoundTripKeepsUnknownPackages() {
        // 卸载的包名留在偏好里，由 AppRepository.dock 在渲染时跳过。
        List<String> pkgs = Arrays.asList("com.uninstalled.app", "com.b", "com.a");
        assertEquals(pkgs, LayoutStore.parseDock(LayoutStore.joinDock(pkgs)));
    }

    @Test
    public void joinDockSkipsEmptyEntries() {
        List<String> pkgs = new ArrayList<>(Arrays.asList("com.a", "", null, "com.b"));
        assertEquals("com.a,com.b", LayoutStore.joinDock(pkgs));
    }

    @Test
    public void joinEmptyDockYieldsEmptyString() {
        assertEquals("", LayoutStore.joinDock(Collections.<String>emptyList()));
        assertTrue(LayoutStore.parseDock(LayoutStore.joinDock(Collections.<String>emptyList())).isEmpty());
    }
}

package io.github.flyfly6.aerialclean.wallpaper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WallpaperConfigTest {

    @Test
    public void testBuiltInAssetPaths() {
        assertEquals("wallpapers/wp_01.webp", WallpaperConfig.getBuiltInAssetPath(0));
        assertEquals("wallpapers/wp_09.webp", WallpaperConfig.getBuiltInAssetPath(8));
        assertEquals("wallpapers/wp_10.webp", WallpaperConfig.getBuiltInAssetPath(9));
        assertEquals("wallpapers/wp_14.webp", WallpaperConfig.getBuiltInAssetPath(13));

        // 越界保护
        assertEquals("wallpapers/wp_01.webp", WallpaperConfig.getBuiltInAssetPath(-1));
        assertEquals("wallpapers/wp_14.webp", WallpaperConfig.getBuiltInAssetPath(100));
    }

    @Test
    public void testBuiltInNamesCount() {
        assertEquals(WallpaperConfig.BUILT_IN_COUNT, WallpaperConfig.BUILT_IN_NAMES.length);
        for (String name : WallpaperConfig.BUILT_IN_NAMES) {
            assertTrue(name != null && name.length() > 0);
        }
    }

    @Test
    public void testTargetDimensions() {
        assertEquals(1280, WallpaperConfig.getTargetWidth(WallpaperConfig.RES_720P));
        assertEquals(720, WallpaperConfig.getTargetHeight(WallpaperConfig.RES_720P));

        assertEquals(1920, WallpaperConfig.getTargetWidth(WallpaperConfig.RES_1080P));
        assertEquals(1080, WallpaperConfig.getTargetHeight(WallpaperConfig.RES_1080P));

        assertEquals(3840, WallpaperConfig.getTargetWidth(WallpaperConfig.RES_4K));
        assertEquals(2160, WallpaperConfig.getTargetHeight(WallpaperConfig.RES_4K));

        // 默认 fallback 到 1080p
        assertEquals(1920, WallpaperConfig.getTargetWidth(999));
        assertEquals(1080, WallpaperConfig.getTargetHeight(999));
    }
}

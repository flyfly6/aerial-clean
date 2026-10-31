package io.github.flyfly6.aerialclean.wallpaper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WallpaperManagerTest {

    @Test
    public void testCalculateInSampleSizeSmallerOrEqual() {
        // 源尺寸小于等于目标尺寸，不降采样
        assertEquals(1, WallpaperManager.calculateInSampleSize(1920, 1080, 1920, 1080));
        assertEquals(1, WallpaperManager.calculateInSampleSize(1280, 720, 1920, 1080));
    }

    @Test
    public void testCalculateInSampleSizePowerOfTwo() {
        // 3840x2160 (4K) 采样到 1920x1080 -> 2倍降采样
        assertEquals(2, WallpaperManager.calculateInSampleSize(3840, 2160, 1920, 1080));

        // 3840x2160 (4K) 采样到 1280x720 -> 2倍还是4倍？
        // halfWidth = 1920, halfHeight = 1080.
        // inSampleSize = 1: halfHeight/1 = 1080 >= 720 && halfWidth/1 = 1920 >= 1280 -> inSampleSize = 2
        // 次轮：halfHeight/2 = 540 < 720 -> 循环退出，故为 2
        assertEquals(2, WallpaperManager.calculateInSampleSize(3840, 2160, 1280, 720));

        // 超大图片 7680x4320 (8K) 采样到 1280x720 -> 4
        // half = 3840x2160. /1 >= 1280 & 720 -> sample=2.
        // /2 = 1920x1080 >= 1280 & 720 -> sample=4.
        // /4 = 960x540 < 1280 & 720 -> 停止
        assertEquals(4, WallpaperManager.calculateInSampleSize(7680, 4320, 1280, 720));
    }
}

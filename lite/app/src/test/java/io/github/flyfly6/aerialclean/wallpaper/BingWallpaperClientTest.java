package io.github.flyfly6.aerialclean.wallpaper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BingWallpaperClientTest {

    @Test
    public void testBuildImageUrlWithResolutions() {
        String raw = "/th?id=OHR.Example_1920x1080.jpg&pid=hp";

        // 720p 替换为 1280x720
        String url720 = BingWallpaperClient.buildImageUrl(raw, WallpaperConfig.RES_720P);
        assertEquals("https://www.bing.com/th?id=OHR.Example_1280x720.jpg&pid=hp", url720);

        // 1080p 保持 1920x1080
        String url1080 = BingWallpaperClient.buildImageUrl(raw, WallpaperConfig.RES_1080P);
        assertEquals("https://www.bing.com/th?id=OHR.Example_1920x1080.jpg&pid=hp", url1080);

        // 4K 替换为 UHD
        String url4k = BingWallpaperClient.buildImageUrl(raw, WallpaperConfig.RES_4K);
        assertEquals("https://www.bing.com/th?id=OHR.Example_UHD.jpg&pid=hp", url4k);
    }

    @Test
    public void testBuildImageUrlWithAbsoluteHttp() {
        String raw = "https://cn.bing.com/th?id=OHR.Example_1920x1080.jpg";
        String url = BingWallpaperClient.buildImageUrl(raw, WallpaperConfig.RES_1080P);
        assertEquals("https://cn.bing.com/th?id=OHR.Example_1920x1080.jpg", url);
    }

    @Test
    public void testBuildImageUrlNullOrEmpty() {
        assertEquals("", BingWallpaperClient.buildImageUrl(null, WallpaperConfig.RES_1080P));
        assertEquals("", BingWallpaperClient.buildImageUrl("", WallpaperConfig.RES_1080P));
    }
}

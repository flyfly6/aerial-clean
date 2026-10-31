package io.github.flyfly6.aerialclean.wallpaper;

/**
 * 桌面壁纸与显示相关的常量配置与参数映射。
 *
 * <p>兼容 Android 4.4 (minSdk 19)，不使用枚举避免额外的类加载开销。
 */
public final class WallpaperConfig {

    public static final int SOURCE_BING_DAILY = 0;
    public static final int SOURCE_BUILT_IN = 1;
    public static final int SOURCE_GRADIENT = 2;

    public static final int RES_720P = 0;
    public static final int RES_1080P = 1;
    public static final int RES_4K = 2;

    public static final int DOCK_STYLE_GLASS = 0;
    public static final int DOCK_STYLE_TRANSLUCENT = 1;
    public static final int DOCK_STYLE_MINIMAL = 2;

    public static final int BUILT_IN_COUNT = 14;

    public static final String[] BUILT_IN_NAMES = new String[]{
            "挪威罗弗敦群岛",
            "优胜美地山谷",
            "夏威夷蔚蓝海岸",
            "阿尔卑斯晨光",
            "冰岛雪山星夜",
            "优胜美地清澈倒影",
            "苍翠林海航拍",
            "云海日出晨曦",
            "托斯卡纳微风丘陵",
            "科莫湖畔风光",
            "撒哈拉金色沙丘",
            "黑沙滩海岸浪花",
            "北欧极地冬雪",
            "大峡谷壮丽晚霞"
    };

    public static String getBuiltInAssetPath(int index) {
        int safeIndex = Math.max(0, Math.min(index, BUILT_IN_COUNT - 1)) + 1;
        String numStr = safeIndex < 10 ? "0" + safeIndex : String.valueOf(safeIndex);
        return "wallpapers/wp_" + numStr + ".webp";
    }

    public static int getTargetWidth(int resTier) {
        switch (resTier) {
            case RES_720P:
                return 1280;
            case RES_4K:
                return 3840;
            case RES_1080P:
            default:
                return 1920;
        }
    }

    public static int getTargetHeight(int resTier) {
        switch (resTier) {
            case RES_720P:
                return 720;
            case RES_4K:
                return 2160;
            case RES_1080P:
            default:
                return 1080;
        }
    }

    private WallpaperConfig() {
    }
}

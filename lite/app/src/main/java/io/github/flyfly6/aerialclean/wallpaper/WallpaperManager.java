package io.github.flyfly6.aerialclean.wallpaper;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 壁纸加载与内存管理中心。
 *
 * <p>核心准则（符合 API 19 规范）：
 * 1. 强制 RGB_565 解码，节约 50% 显存；
 * 2. 基于目标分辨率（720p / 1080p / 4K）按 2 的整数次幂进行 inSampleSize 采样；
 * 3. 异步解码，结果回到主线程回调；
 * 4. 显式 recycle() 释放旧位图，防止 512MB 老电视 OOM。
 */
public class WallpaperManager {

    private static final String TAG = "WallpaperManager";
    public static final String BING_CACHE_FILE = "wallpaper_bing_cache.jpg";

    private static volatile WallpaperManager sInstance;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Bitmap activeBitmap;

    public interface Callback {
        void onLoaded(Bitmap bitmap, String title);

        void onError(Exception e);
    }

    public static WallpaperManager getInstance() {
        if (sInstance == null) {
            synchronized (WallpaperManager.class) {
                if (sInstance == null) {
                    sInstance = new WallpaperManager();
                }
            }
        }
        return sInstance;
    }

    private WallpaperManager() {
    }

    /**
     * 根据设置异步加载壁纸。
     */
    public void loadWallpaper(final Context context, final int source, final int resTier,
                              final int builtInIndex, final Callback callback) {
        final Context appContext = context.getApplicationContext();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap result = null;
                    String title = "";

                    int reqWidth = WallpaperConfig.getTargetWidth(resTier);
                    int reqHeight = WallpaperConfig.getTargetHeight(resTier);

                    if (source == WallpaperConfig.SOURCE_BING_DAILY) {
                        File cacheFile = new File(appContext.getFilesDir(), BING_CACHE_FILE);
                        if (cacheFile.exists() && cacheFile.length() > 0) {
                            FileInputStream fis = new FileInputStream(cacheFile);
                            try {
                                result = decodeSampledBitmapFromStream(fis, reqWidth, reqHeight);
                                title = "必应每日精选";
                            } finally {
                                fis.close();
                            }
                        }
                        // 如果没有本地缓存，降级到内置第 1 张壁纸，同时触发异步拉取
                        if (result == null) {
                            result = loadBuiltInBitmap(appContext, 0, reqWidth, reqHeight);
                            title = WallpaperConfig.BUILT_IN_NAMES[0] + " (必应离线兜底)";
                        }
                    } else if (source == WallpaperConfig.SOURCE_BUILT_IN) {
                        result = loadBuiltInBitmap(appContext, builtInIndex, reqWidth, reqHeight);
                        int safeIdx = Math.max(0, Math.min(builtInIndex, WallpaperConfig.BUILT_IN_COUNT - 1));
                        title = WallpaperConfig.BUILT_IN_NAMES[safeIdx];
                    }

                    final Bitmap finalResult = result;
                    final String finalTitle = title;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (finalResult != null) {
                                Bitmap old = activeBitmap;
                                activeBitmap = finalResult;
                                if (old != null && old != finalResult && !old.isRecycled()) {
                                    old.recycle();
                                }
                                if (callback != null) {
                                    callback.onLoaded(finalResult, finalTitle);
                                }
                            } else {
                                if (callback != null) {
                                    callback.onError(new IllegalStateException("Failed to decode wallpaper bitmap"));
                                }
                            }
                        }
                    });
                } catch (final Exception e) {
                    Log.w(TAG, "Error loading wallpaper", e);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onError(e);
                            }
                        }
                    });
                }
            }
        });
    }

    private Bitmap loadBuiltInBitmap(Context context, int index, int reqWidth, int reqHeight) throws Exception {
        String assetPath = WallpaperConfig.getBuiltInAssetPath(index);
        AssetManager am = context.getAssets();
        InputStream is = am.open(assetPath);
        try {
            return decodeSampledBitmapFromStream(is, reqWidth, reqHeight);
        } finally {
            is.close();
        }
    }

    /**
     * 计算符合目标尺寸的 inSampleSize (2 的整数次幂)。
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        return calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight);
    }

    /**
     * 根据真实尺寸与目标尺寸计算采样倍数。
     */
    public static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * 从输入流中安全采样解码 Bitmap。
     */
    public static Bitmap decodeSampledBitmapFromStream(InputStream is, int reqWidth, int reqHeight) {
        // 由于需要 decode 两次（一次取边界，一次真正解码），先缓冲成 byte[] 数组
        byte[] data;
        try {
            data = readFully(is);
        } catch (Exception e) {
            Log.w(TAG, "Failed to read stream to bytes", e);
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inPreferredConfig = Bitmap.Config.RGB_565; // 内存减半关键
        options.inDither = true; // 16 位色彩抖动，消除渐变色阶条纹
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    private static byte[] readFully(InputStream is) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    public synchronized Bitmap getActiveBitmap() {
        return activeBitmap;
    }
}

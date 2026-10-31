package io.github.flyfly6.aerialclean.wallpaper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 必应每日壁纸获取客户端。
 *
 * <p>特性：
 * 1. 纯原生 HttpURLConnection + org.json，零外部网络库依赖；
 * 2. 异步执行，超时控制严格（Connect 10s / Read 15s）；
 * 3. 流式原子落盘（.tmp 写入后 renameTo 目标缓存文件）；
 * 4. 网络异常不崩溃、不断言，优雅降级。
 */
public class BingWallpaperClient {

    private static final String TAG = "BingWallpaperClient";
    private static final String API_URL = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1";
    private static final String BING_HOST = "https://www.bing.com";

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static volatile Handler sMainHandler;

    private static Handler getMainHandler() {
        if (sMainHandler == null) {
            synchronized (BingWallpaperClient.class) {
                if (sMainHandler == null) {
                    sMainHandler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return sMainHandler;
    }

    public interface Callback {
        void onSuccess(File cacheFile, String title);

        void onFailure(Exception e);
    }

    public static void fetchTodayWallpaper(final Context context, final int resTier, final Callback callback) {
        final Context appContext = context.getApplicationContext();
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1. 获取壁纸元数据 JSON
                    URL apiUrl = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                    conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                    conn.setReadTimeout(READ_TIMEOUT_MS);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 AerialClean-TV/1.0");

                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw new IllegalStateException("Bing API returned response code: " + responseCode);
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    conn.disconnect();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray images = json.optJSONArray("images");
                    if (images == null || images.length() == 0) {
                        throw new IllegalStateException("No images found in Bing response");
                    }

                    JSONObject firstImg = images.getJSONObject(0);
                    String imgUrlPath = firstImg.getString("url");
                    String copyright = firstImg.optString("copyright", "必应每日精选");

                    // 根据分辨率等级替换 URL 后缀
                    String fullImgUrl = buildImageUrl(imgUrlPath, resTier);

                    // 2. 流式下载图片数据到临时文件
                    File targetFile = new File(appContext.getFilesDir(), WallpaperManager.BING_CACHE_FILE);
                    File tmpFile = new File(appContext.getFilesDir(), WallpaperManager.BING_CACHE_FILE + ".tmp");

                    URL downloadUrl = new URL(fullImgUrl);
                    HttpURLConnection imgConn = (HttpURLConnection) downloadUrl.openConnection();
                    imgConn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                    imgConn.setReadTimeout(READ_TIMEOUT_MS);

                    int imgCode = imgConn.getResponseCode();
                    if (imgCode != HttpURLConnection.HTTP_OK) {
                        throw new IllegalStateException("Bing image download failed with code: " + imgCode);
                    }

                    InputStream in = imgConn.getInputStream();
                    FileOutputStream fos = new FileOutputStream(tmpFile);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                    fos.close();
                    in.close();
                    imgConn.disconnect();

                    // 原子重命名
                    if (tmpFile.renameTo(targetFile)) {
                        notifySuccess(targetFile, copyright, callback);
                    } else {
                        // 覆盖写入兜底
                        if (targetFile.exists()) {
                            targetFile.delete();
                        }
                        if (tmpFile.renameTo(targetFile)) {
                            notifySuccess(targetFile, copyright, callback);
                        } else {
                            throw new IllegalStateException("Failed to rename tmp file to target cache");
                        }
                    }

                } catch (final Exception e) {
                    Log.w(TAG, "Failed to fetch Bing daily wallpaper", e);
                    getMainHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onFailure(e);
                            }
                        }
                    });
                }
            }
        });
    }

    public static String buildImageUrl(String rawPath, int resTier) {
        if (rawPath == null || rawPath.length() == 0) {
            return "";
        }
        String path = rawPath;
        if (resTier == WallpaperConfig.RES_720P) {
            path = path.replace("1920x1080", "1280x720");
        } else if (resTier == WallpaperConfig.RES_4K) {
            path = path.replace("1920x1080", "UHD");
        }
        return path.startsWith("http") ? path : BING_HOST + path;
    }

    private static void notifySuccess(final File file, final String title, final Callback callback) {
        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onSuccess(file, title);
                }
            }
        });
    }
}

package io.github.flyfly6.aerialclean.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 电视端天气客户端（参考 aerial-tv 对接 Open-Meteo 免 key API）。
 *
 * <p>兼容 Android 4.4 (API 19)，零第三方网络库，支持离线缓存与优雅降级。
 */
public class WeatherClient {

    private static final String TAG = "WeatherClient";
    private static final String PREF_NAME = "weather_cache";
    private static final String KEY_CACHED_WEATHER = "cached_weather_text";
    private static final String KEY_LAST_FETCH = "last_fetch_time";
    private static final long CACHE_EXPIRE_MS = 60 * 60 * 1000L; // 1 小时缓存

    // 默认北京坐标 (39.9042, 116.4074)
    private static final String DEFAULT_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=39.9042&longitude=116.4074&current_weather=true";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static volatile Handler sMainHandler;

    public interface Callback {
        void onLoaded(String weatherText);
    }

    private static Handler getMainHandler() {
        if (sMainHandler == null) {
            synchronized (WeatherClient.class) {
                if (sMainHandler == null) {
                    sMainHandler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return sMainHandler;
    }

    public static void fetchWeather(final Context context, final Callback callback) {
        final Context appContext = context.getApplicationContext();
        final SharedPreferences sp = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        final String cached = sp.getString(KEY_CACHED_WEATHER, "");
        final long lastFetch = sp.getLong(KEY_LAST_FETCH, 0);

        // 如果缓存未过期，先回调缓存数据
        if (cached != null && cached.length() > 0) {
            callback.onLoaded(cached);
            if (System.currentTimeMillis() - lastFetch < CACHE_EXPIRE_MS) {
                return;
            }
        }

        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(DEFAULT_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestProperty("User-Agent", "AerialClean-TV/1.0");

                    int code = conn.getResponseCode();
                    if (code != HttpURLConnection.HTTP_OK) {
                        return;
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
                    JSONObject current = json.optJSONObject("current_weather");
                    if (current == null) {
                        return;
                    }

                    double temp = current.optDouble("temperature", 0.0);
                    int weatherCode = current.optInt("weathercode", 0);
                    String desc = getWeatherDescription(weatherCode);
                    String icon = getWeatherIcon(weatherCode);
                    int roundedTemp = (int) Math.round(temp);

                    // 按照 aerial-tv 格式：天气图标 + 温度 + 位置 + 天气 + 温度
                    final String weatherText = icon + " " + roundedTemp + "°C 北京 " + desc + " " + roundedTemp + "°C";

                    // 持久化缓存
                    sp.edit()
                            .putString(KEY_CACHED_WEATHER, weatherText)
                            .putLong(KEY_LAST_FETCH, System.currentTimeMillis())
                            .apply();

                    getMainHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onLoaded(weatherText);
                        }
                    });

                } catch (Exception e) {
                    Log.w(TAG, "Failed to fetch weather: " + e.getMessage());
                }
            }
        });
    }

    public static String getWeatherIcon(int code) {
        switch (code) {
            case 0:
                return "☀";
            case 1:
            case 2:
                return "⛅";
            case 3:
                return "☁";
            case 45:
            case 48:
                return "🌫";
            case 51:
            case 53:
            case 55:
            case 61:
            case 63:
            case 65:
                return "🌧";
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86:
                return "🌨";
            case 95:
            case 96:
            case 99:
                return "⛈";
            default:
                return "☀";
        }
    }

    /**
     * WMO 国际标准天气代码映射为中文。
     */
    public static String getWeatherDescription(int code) {
        switch (code) {
            case 0:
                return "晴";
            case 1:
            case 2:
                return "晴间多云";
            case 3:
                return "多云";
            case 45:
            case 48:
                return "雾";
            case 51:
            case 53:
            case 55:
                return "细雨";
            case 61:
            case 63:
            case 65:
                return "雨";
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86:
                return "雪";
            case 95:
            case 96:
            case 99:
                return "雷阵雨";
            default:
                return "晴";
        }
    }
}

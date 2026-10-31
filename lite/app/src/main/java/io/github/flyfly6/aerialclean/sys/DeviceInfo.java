package io.github.flyfly6.aerialclean.sys;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import io.github.flyfly6.aerialclean.R;

/**
 * 设备状态文案。
 *
 * <p>只读连接类型，不碰 SSID（要 ACCESS_WIFI_STATE，且 8.1+ 本就拿不到）。
 */
public final class DeviceInfo {

    private DeviceInfo() {
    }

    /** @return 「有线」「Wi-Fi」「SSID」或「未连接」 */
    public static String networkLabel(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return context.getString(R.string.net_disconnected);
        }
        // getActiveNetworkInfo 已废弃但在 4.4 上是唯一可用入口；getActiveNetwork 要 API 23。
        NetworkInfo info;
        try {
            info = manager.getActiveNetworkInfo();
        } catch (SecurityException denied) {
            // 权限被 ROM 剥掉时不能连累桌面，降级成「未连接」。
            return context.getString(R.string.net_disconnected);
        }
        if (info == null || !info.isConnected()) {
            return context.getString(R.string.net_disconnected);
        }
        if (info.getType() == ConnectivityManager.TYPE_ETHERNET) {
            return context.getString(R.string.net_ethernet);
        }
        if (info.getType() == ConnectivityManager.TYPE_WIFI) {
            String ssid = info.getExtraInfo();
            if (ssid != null && !ssid.isEmpty() && !ssid.startsWith("<")) {
                return ssid.replace("\"", "");
            }
            return context.getString(R.string.net_wifi);
        }
        String name = info.getTypeName();
        return name == null ? context.getString(R.string.net_disconnected) : name;
    }

    /**
     * @return ConnectivityManager.TYPE_WIFI, TYPE_ETHERNET, 或 -1 (未连接)
     */
    public static int networkType(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return -1;
        }
        try {
            NetworkInfo info = manager.getActiveNetworkInfo();
            if (info == null || !info.isConnected()) {
                return -1;
            }
            return info.getType();
        } catch (SecurityException denied) {
            return -1;
        }
    }
}

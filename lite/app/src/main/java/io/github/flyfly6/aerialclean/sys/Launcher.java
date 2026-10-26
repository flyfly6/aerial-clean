package io.github.flyfly6.aerialclean.sys;

import android.content.ComponentName;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import io.github.flyfly6.aerialclean.R;
import io.github.flyfly6.aerialclean.app.AppEntry;

/**
 * 启动第三方应用。失败只提示，不崩。
 */
public final class Launcher {

    private static final String TAG = "AerialClean";

    private Launcher() {
    }

    public static boolean launch(Context context, AppEntry entry) {
        ComponentName component = ComponentName.unflattenFromString(entry.component);
        if (component == null) {
            Log.w(TAG, "bad component: " + entry.component);
            toast(context);
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "launch failed: " + entry.pkg, e);
            toast(context);
            return false;
        }
    }

    private static void toast(Context context) {
        Toast.makeText(context, R.string.launch_failed, Toast.LENGTH_SHORT).show();
    }

    /** 打开系统的应用信息页（4.4 已有 ACTION_APPLICATION_DETAILS_SETTINGS）。 */
    public static void appInfo(Context context, AppEntry entry) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", entry.pkg, null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "app info failed: " + entry.pkg, e);
            Toast.makeText(context, R.string.app_info_failed, Toast.LENGTH_SHORT).show();
        }
    }
}

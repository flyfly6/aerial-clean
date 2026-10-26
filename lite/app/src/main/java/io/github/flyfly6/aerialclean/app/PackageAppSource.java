package io.github.flyfly6.aerialclean.app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备侧的应用枚举：把 PackageManager 的查询结果折成 {@link AppEntry}。
 */
public final class PackageAppSource implements AppRepository.AppSource {

    private final PackageManager pm;

    public PackageAppSource(PackageManager pm) {
        this.pm = pm;
    }

    @Override
    public List<AppEntry> query(String category) {
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(category);
        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);
        List<AppEntry> entries = new ArrayList<>(resolved == null ? 0 : resolved.size());
        if (resolved == null) {
            return entries;
        }
        for (ResolveInfo info : resolved) {
            ActivityInfo activity = info.activityInfo;
            if (activity == null || activity.packageName == null || activity.name == null) {
                continue;
            }
            ComponentName component = new ComponentName(activity.packageName, activity.name);
            CharSequence label = info.loadLabel(pm);
            boolean system = activity.applicationInfo != null
                    && (activity.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            entries.add(new AppEntry(activity.packageName, component.flattenToString(),
                    label == null ? activity.packageName : label, system));
        }
        return entries;
    }
}

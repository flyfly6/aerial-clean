package io.github.flyfly6.aerialclean.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import io.github.flyfly6.aerialclean.R;
import io.github.flyfly6.aerialclean.app.AppEntry;

/**
 * 长按/MENU 打开的操作单。只回传意图，改数据由调用方做。
 */
public final class ActionSheet {

    public interface Listener {
        void onLaunch(AppEntry entry);

        /** 已经在 dock 里就移除，否则加入。 */
        void onToggleDock(AppEntry entry);

        void onAppInfo(AppEntry entry);

        void onHide(AppEntry entry);
    }

    private ActionSheet() {
    }

    public static void show(Context context, final AppEntry entry, boolean inDock, final Listener listener) {
        final String[] items = new String[]{
                context.getString(R.string.action_launch),
                context.getString(inDock ? R.string.action_remove_dock : R.string.action_add_dock),
                context.getString(R.string.action_app_info),
                context.getString(R.string.action_hide),
        };
        new AlertDialog.Builder(context, android.R.style.Theme_Holo_Dialog)
                .setTitle(entry.label)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            listener.onLaunch(entry);
                        } else if (which == 1) {
                            listener.onToggleDock(entry);
                        } else if (which == 2) {
                            listener.onAppInfo(entry);
                        } else {
                            listener.onHide(entry);
                        }
                    }
                })
                .show();
    }
}

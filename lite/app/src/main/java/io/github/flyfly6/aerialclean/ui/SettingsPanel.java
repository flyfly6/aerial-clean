package io.github.flyfly6.aerialclean.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.List;

import io.github.flyfly6.aerialclean.R;
import io.github.flyfly6.aerialclean.app.AppEntry;
import io.github.flyfly6.aerialclean.app.LayoutStore;

/**
 * 设置覆盖层：列数、固定栏槽位、时钟开关、隐藏项恢复。
 *
 * <p>只回传选择，不直接写偏好；调用方写完偏好后自己重开本面板，值立刻可见。
 */
public final class SettingsPanel {

    public interface Listener {
        void onColumns(int columns);

        void onDockCount(int dockCount);

        void onShowClock(boolean show);

        void onUnhide(String pkg);
    }

    private SettingsPanel() {
    }

    public static void show(final Context context, final int columns, final int dockCount,
                            final boolean showClock, final List<AppEntry> hiddenEntries,
                            final Listener listener) {
        final String[] items = new String[]{
                context.getString(R.string.settings_columns, columns),
                context.getString(R.string.settings_dock_count, dockCount),
                context.getString(showClock ? R.string.settings_clock_on : R.string.settings_clock_off),
                context.getString(R.string.settings_hidden, hiddenEntries.size()),
        };
        new AlertDialog.Builder(context, android.R.style.Theme_Holo_Dialog)
                .setTitle(R.string.settings_title)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            chooseSlots(context, R.string.settings_columns_title, columns, listener, true);
                        } else if (which == 1) {
                            chooseSlots(context, R.string.settings_dock_count_title, dockCount, listener, false);
                        } else if (which == 2) {
                            listener.onShowClock(!showClock);
                        } else {
                            showHidden(context, hiddenEntries, listener);
                        }
                    }
                })
                .setNegativeButton(R.string.settings_close, null)
                .show();
    }

    /** 4–6 槽位的单选。 */
    private static void chooseSlots(final Context context, int titleRes, int current,
                                    final Listener listener, final boolean forColumns) {
        final String[] values = new String[LayoutStore.MAX_SLOTS - LayoutStore.MIN_SLOTS + 1];
        for (int i = 0; i < values.length; i++) {
            values[i] = context.getString(R.string.settings_slots_option, LayoutStore.MIN_SLOTS + i);
        }
        new AlertDialog.Builder(context, android.R.style.Theme_Holo_Dialog)
                .setTitle(titleRes)
                .setSingleChoiceItems(values, current - LayoutStore.MIN_SLOTS, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        int slots = LayoutStore.MIN_SLOTS + which;
                        if (forColumns) {
                            listener.onColumns(slots);
                        } else {
                            listener.onDockCount(slots);
                        }
                    }
                })
                .show();
    }

    private static void showHidden(final Context context, final List<AppEntry> hiddenEntries,
                                   final Listener listener) {
        if (hiddenEntries.isEmpty()) {
            new AlertDialog.Builder(context, android.R.style.Theme_Holo_Dialog)
                    .setTitle(R.string.settings_hidden_title)
                    .setMessage(R.string.settings_hidden_empty)
                    .setPositiveButton(R.string.settings_close, null)
                    .show();
            return;
        }
        final String[] labels = new String[hiddenEntries.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = String.valueOf(hiddenEntries.get(i).label);
        }
        new AlertDialog.Builder(context, android.R.style.Theme_Holo_Dialog)
                .setTitle(R.string.settings_hidden_title)
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onUnhide(hiddenEntries.get(which).pkg);
                    }
                })
                .show();
    }
}

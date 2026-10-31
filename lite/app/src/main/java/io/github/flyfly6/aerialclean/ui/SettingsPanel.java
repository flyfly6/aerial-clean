package io.github.flyfly6.aerialclean.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.List;

import io.github.flyfly6.aerialclean.R;
import io.github.flyfly6.aerialclean.app.AppEntry;
import io.github.flyfly6.aerialclean.wallpaper.WallpaperConfig;

/**
 * 现代化 TV 卡片式设置面板。
 *
 * <p>像素级还原设计图规范，全遥控器 D-Pad 可达，支持壁纸来源、分辨率采样、内置选择、
 * 底板样式、网格列数、槽位数、时钟开关与系统原生设置。
 */
public final class SettingsPanel {

    public interface Listener {
        void onColumns(int columns);

        void onDockCount(int dockCount);

        void onShowClock(boolean show);

        void onUnhide(String pkg);

        void onWallpaperSource(int source);

        void onWallpaperResolution(int resTier);

        void onBuiltInIndex(int index);

        void onDockStyle(int style);
    }

    private SettingsPanel() {
    }

    public static void show(final Context context, final int columns, final int dockCount,
                            final boolean showClock, final int wpSource, final int wpRes,
                            final int builtInIdx, final int dockStyle,
                            final List<AppEntry> hiddenEntries,
                            final Listener listener) {

        final Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_modern_settings);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        // 1. 壁纸来源
        final TextView optWpBing = (TextView) dialog.findViewById(R.id.opt_wp_bing);
        final TextView optWpBuiltin = (TextView) dialog.findViewById(R.id.opt_wp_builtin);
        final TextView optWpGradient = (TextView) dialog.findViewById(R.id.opt_wp_gradient);
        final View secBuiltin = dialog.findViewById(R.id.sec_builtin_container);

        updateSourceSelection(wpSource, optWpBing, optWpBuiltin, optWpGradient, secBuiltin);

        optWpBing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSourceSelection(WallpaperConfig.SOURCE_BING_DAILY, optWpBing, optWpBuiltin, optWpGradient, secBuiltin);
                if (listener != null) listener.onWallpaperSource(WallpaperConfig.SOURCE_BING_DAILY);
            }
        });
        optWpBuiltin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSourceSelection(WallpaperConfig.SOURCE_BUILT_IN, optWpBing, optWpBuiltin, optWpGradient, secBuiltin);
                if (listener != null) listener.onWallpaperSource(WallpaperConfig.SOURCE_BUILT_IN);
            }
        });
        optWpGradient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSourceSelection(WallpaperConfig.SOURCE_GRADIENT, optWpBing, optWpBuiltin, optWpGradient, secBuiltin);
                if (listener != null) listener.onWallpaperSource(WallpaperConfig.SOURCE_GRADIENT);
            }
        });

        // 2. 壁纸采样分辨率
        final TextView optRes720 = (TextView) dialog.findViewById(R.id.opt_res_720p);
        final TextView optRes1080 = (TextView) dialog.findViewById(R.id.opt_res_1080p);
        final TextView optRes4k = (TextView) dialog.findViewById(R.id.opt_res_4k);

        updateResSelection(wpRes, optRes720, optRes1080, optRes4k);

        optRes720.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateResSelection(WallpaperConfig.RES_720P, optRes720, optRes1080, optRes4k);
                if (listener != null) listener.onWallpaperResolution(WallpaperConfig.RES_720P);
            }
        });
        optRes1080.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateResSelection(WallpaperConfig.RES_1080P, optRes720, optRes1080, optRes4k);
                if (listener != null) listener.onWallpaperResolution(WallpaperConfig.RES_1080P);
            }
        });
        optRes4k.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateResSelection(WallpaperConfig.RES_4K, optRes720, optRes1080, optRes4k);
                if (listener != null) listener.onWallpaperResolution(WallpaperConfig.RES_4K);
            }
        });

        // 3. 内置风景切换
        final TextView txtBuiltinName = (TextView) dialog.findViewById(R.id.txt_current_builtin_name);
        final TextView btnPrevBuiltin = (TextView) dialog.findViewById(R.id.btn_prev_builtin);
        final TextView btnNextBuiltin = (TextView) dialog.findViewById(R.id.btn_next_builtin);
        final int[] curIdx = new int[]{Math.max(0, Math.min(builtInIdx, WallpaperConfig.BUILT_IN_COUNT - 1))};

        updateBuiltinLabel(txtBuiltinName, curIdx[0]);

        btnPrevBuiltin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                curIdx[0] = (curIdx[0] - 1 + WallpaperConfig.BUILT_IN_COUNT) % WallpaperConfig.BUILT_IN_COUNT;
                updateBuiltinLabel(txtBuiltinName, curIdx[0]);
                if (listener != null) listener.onBuiltInIndex(curIdx[0]);
            }
        });
        btnNextBuiltin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                curIdx[0] = (curIdx[0] + 1) % WallpaperConfig.BUILT_IN_COUNT;
                updateBuiltinLabel(txtBuiltinName, curIdx[0]);
                if (listener != null) listener.onBuiltInIndex(curIdx[0]);
            }
        });

        // 4. 底板样式
        final TextView optDockTrans = (TextView) dialog.findViewById(R.id.opt_dock_translucent);
        final TextView optDockGlass = (TextView) dialog.findViewById(R.id.opt_dock_glass);
        final TextView optDockMini = (TextView) dialog.findViewById(R.id.opt_dock_minimal);

        updateDockStyleSelection(dockStyle, optDockTrans, optDockGlass, optDockMini);

        optDockTrans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDockStyleSelection(WallpaperConfig.DOCK_STYLE_TRANSLUCENT, optDockTrans, optDockGlass, optDockMini);
                if (listener != null) listener.onDockStyle(WallpaperConfig.DOCK_STYLE_TRANSLUCENT);
            }
        });
        optDockGlass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDockStyleSelection(WallpaperConfig.DOCK_STYLE_GLASS, optDockTrans, optDockGlass, optDockMini);
                if (listener != null) listener.onDockStyle(WallpaperConfig.DOCK_STYLE_GLASS);
            }
        });
        optDockMini.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDockStyleSelection(WallpaperConfig.DOCK_STYLE_MINIMAL, optDockTrans, optDockGlass, optDockMini);
                if (listener != null) listener.onDockStyle(WallpaperConfig.DOCK_STYLE_MINIMAL);
            }
        });

        // 5. 桌面网格列数 (4~8 列)
        final TextView[] colViews = new TextView[]{
                (TextView) dialog.findViewById(R.id.opt_col_4),
                (TextView) dialog.findViewById(R.id.opt_col_5),
                (TextView) dialog.findViewById(R.id.opt_col_6),
                (TextView) dialog.findViewById(R.id.opt_col_7),
                (TextView) dialog.findViewById(R.id.opt_col_8)
        };
        updateColSelection(columns, colViews);
        for (int i = 0; i < colViews.length; i++) {
            final int colVal = 4 + i;
            colViews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateColSelection(colVal, colViews);
                    if (listener != null) listener.onColumns(colVal);
                }
            });
        }

        // 6. 常用栏槽位 (4~6 个)
        final TextView[] dockCountViews = new TextView[]{
                (TextView) dialog.findViewById(R.id.opt_dock_4),
                (TextView) dialog.findViewById(R.id.opt_dock_5),
                (TextView) dialog.findViewById(R.id.opt_dock_6)
        };
        updateDockCountSelection(dockCount, dockCountViews);
        for (int i = 0; i < dockCountViews.length; i++) {
            final int slotVal = 4 + i;
            dockCountViews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateDockCountSelection(slotVal, dockCountViews);
                    if (listener != null) listener.onDockCount(slotVal);
                }
            });
        }

        // 7. 桌面大时钟开关
        final TextView optClockToggle = (TextView) dialog.findViewById(R.id.opt_clock_toggle);
        final boolean[] curClock = new boolean[]{showClock};
        updateClockToggle(optClockToggle, curClock[0]);
        optClockToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                curClock[0] = !curClock[0];
                updateClockToggle(optClockToggle, curClock[0]);
                if (listener != null) listener.onShowClock(curClock[0]);
            }
        });

        // 8. 打开电视系统原生设置
        View btnSysSettings = dialog.findViewById(R.id.btn_open_system_settings);
        btnSysSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    context.startActivity(new Intent(Settings.ACTION_SETTINGS));
                } catch (Exception e) {
                    // 部分电视无标准设置页面兜底
                }
            }
        });

        // 初始让第一个当前选中的来源获得焦点
        optWpBing.requestFocus();
        dialog.show();
    }

    private static void updateSourceSelection(int source, TextView bing, TextView builtin, TextView grad, View secBuiltin) {
        bing.setSelected(source == WallpaperConfig.SOURCE_BING_DAILY);
        builtin.setSelected(source == WallpaperConfig.SOURCE_BUILT_IN);
        grad.setSelected(source == WallpaperConfig.SOURCE_GRADIENT);
        if (secBuiltin != null) {
            secBuiltin.setVisibility(source == WallpaperConfig.SOURCE_BUILT_IN ? View.VISIBLE : View.GONE);
        }
    }

    private static void updateResSelection(int resTier, TextView r720, TextView r1080, TextView r4k) {
        r720.setSelected(resTier == WallpaperConfig.RES_720P);
        r1080.setSelected(resTier == WallpaperConfig.RES_1080P);
        r4k.setSelected(resTier == WallpaperConfig.RES_4K);
    }

    private static void updateBuiltinLabel(TextView tv, int index) {
        int safe = Math.max(0, Math.min(index, WallpaperConfig.BUILT_IN_COUNT - 1));
        tv.setText(WallpaperConfig.BUILT_IN_NAMES[safe] + " (" + (safe + 1) + "/" + WallpaperConfig.BUILT_IN_COUNT + ")");
    }

    private static void updateDockStyleSelection(int style, TextView trans, TextView glass, TextView mini) {
        trans.setSelected(style == WallpaperConfig.DOCK_STYLE_TRANSLUCENT);
        glass.setSelected(style == WallpaperConfig.DOCK_STYLE_GLASS);
        mini.setSelected(style == WallpaperConfig.DOCK_STYLE_MINIMAL);
    }

    private static void updateColSelection(int current, TextView[] views) {
        for (int i = 0; i < views.length; i++) {
            views[i].setSelected((4 + i) == current);
        }
    }

    private static void updateDockCountSelection(int current, TextView[] views) {
        for (int i = 0; i < views.length; i++) {
            views[i].setSelected((4 + i) == current);
        }
    }

    private static void updateClockToggle(TextView tv, boolean show) {
        tv.setSelected(show);
        tv.setText(show ? "显示时钟 (开)" : "隐藏时钟 (关)");
    }
}

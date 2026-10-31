package io.github.flyfly6.aerialclean;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.flyfly6.aerialclean.app.AppEntry;
import io.github.flyfly6.aerialclean.app.AppRepository;
import io.github.flyfly6.aerialclean.app.IconLoader;
import io.github.flyfly6.aerialclean.app.LayoutStore;
import io.github.flyfly6.aerialclean.app.PackageAppSource;
import io.github.flyfly6.aerialclean.sys.DeviceInfo;
import io.github.flyfly6.aerialclean.sys.Launcher;
import io.github.flyfly6.aerialclean.ui.ActionSheet;
import io.github.flyfly6.aerialclean.ui.BackdropView;
import io.github.flyfly6.aerialclean.ui.ClockSaver;
import io.github.flyfly6.aerialclean.ui.ClockView;
import io.github.flyfly6.aerialclean.ui.DockView;
import io.github.flyfly6.aerialclean.ui.GridView;
import io.github.flyfly6.aerialclean.ui.NetworkIconView;
import io.github.flyfly6.aerialclean.ui.SettingsPanel;
import io.github.flyfly6.aerialclean.ui.TileAdapter;
import io.github.flyfly6.aerialclean.wallpaper.BingWallpaperClient;
import io.github.flyfly6.aerialclean.wallpaper.WallpaperConfig;
import io.github.flyfly6.aerialclean.wallpaper.WallpaperManager;
import io.github.flyfly6.aerialclean.weather.WeatherClient;

/**
 * AerialClean 电视桌面主入口。
 *
 * <p>遵循 TV 端 10-foot UI 设计规范：
 * 1. 首屏大时钟 + 中文日期/天气 + 右上角状态胶囊 + 底部下沉居中 Dock；
 * 2. 遥控器按 DOWN 或点击进入“全部应用网格”，85% 暗角自动沉浸，按 UP/BACK 瞬间切回首屏 Dock；
 * 3. 遥控器按 MENU 呼出全功能现代化卡片设置面板。
 */
public class HomeActivity extends Activity implements TileAdapter.Listener, ActionSheet.Listener, SettingsPanel.Listener {

    private static final String TAG = "HomeActivity";
    private static final long IDLE_CHECK_MS = 5_000L;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private AppRepository repository;
    private IconLoader iconLoader;
    private LayoutStore layout;

    private BackdropView backdrop;
    private View layerHome;
    private View layerGrid;
    private FrameLayout dockContainer;
    private GridView grid;
    private DockView dock;
    private ClockView clock;
    private NetworkIconView netIcon;
    private TextView wallpaperLabel;
    private TextView gridTitle;
    private ClockSaver saver;

    private boolean isShowingGrid = false;
    private long lastInteraction = SystemClock.uptimeMillis();
    private long saverIdleMs = LayoutStore.DEFAULT_SAVER_IDLE_MS;
    private boolean saverShown;

    private final Runnable idleCheck = new Runnable() {
        @Override
        public void run() {
            if (!saverShown && SystemClock.uptimeMillis() - lastInteraction >= saverIdleMs) {
                showSaver();
            }
            main.postDelayed(this, IDLE_CHECK_MS);
        }
    };

    private List<AppEntry> allApps = Collections.emptyList();
    private Set<String> hidden = new LinkedHashSet<>();
    private List<String> dockPkgs = new ArrayList<>();
    private int columns = LayoutStore.DEFAULT_SLOTS;
    private int dockCount = LayoutStore.DEFAULT_SLOTS;
    private boolean showClock = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        iconLoader = new IconLoader(this, getMemoryClass());
        layout = new LayoutStore(this);
        repository = new AppRepository(new PackageAppSource(getPackageManager()), getPackageName());

        backdrop = (BackdropView) findViewById(R.id.backdrop);
        layerHome = findViewById(R.id.layer_home);
        layerGrid = findViewById(R.id.layer_grid);
        dockContainer = (FrameLayout) findViewById(R.id.dock_container);
        wallpaperLabel = (TextView) findViewById(R.id.wallpaper_label);
        gridTitle = (TextView) findViewById(R.id.grid_title);

        grid = (GridView) findViewById(R.id.grid);
        grid.setIconLoader(iconLoader);
        grid.setListener(this);

        saver = (ClockSaver) findViewById(R.id.saver);
        clock = (ClockView) findViewById(R.id.clock);
        netIcon = (NetworkIconView) findViewById(R.id.net_icon);

        dock = (DockView) findViewById(R.id.dock);
        dock.setIconLoader(iconLoader);
        dock.setListener(this);

        findViewById(R.id.btn_settings_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });

        loadCurrentWallpaper();
        applyDockStyle();
        fetchWeatherAsync();
        reload();
    }

    private void fetchWeatherAsync() {
        WeatherClient.fetchWeather(this, new WeatherClient.Callback() {
            @Override
            public void onLoaded(final String weatherText) {
                if (clock != null) {
                    clock.setWeatherText(weatherText);
                }
            }
        });
    }

    private void showAllAppsGrid(boolean show) {
        isShowingGrid = show;
        if (show) {
            layerHome.setVisibility(View.GONE);
            layerGrid.setVisibility(View.VISIBLE);
            backdrop.setDimRatio(0.85f);
            grid.post(new Runnable() {
                @Override
                public void run() {
                    focusFirstGridItem();
                }
            });
        } else {
            layerGrid.setVisibility(View.GONE);
            layerHome.setVisibility(View.VISIBLE);
            backdrop.setDimRatio(0.0f);
            dock.post(new Runnable() {
                @Override
                public void run() {
                    focusFirstDockItem();
                }
            });
        }
    }

    private void loadCurrentWallpaper() {
        final int source = layout.wallpaperSource();
        final int resTier = layout.wallpaperResolution();
        final int builtInIdx = layout.builtInIndex();

        if (source == WallpaperConfig.SOURCE_GRADIENT) {
            backdrop.setWallpaperBitmap(null);
            saver.setWallpaperBitmap(null);
            wallpaperLabel.setVisibility(View.GONE);
            return;
        }

        WallpaperManager.getInstance().loadWallpaper(this, source, resTier, builtInIdx,
                new WallpaperManager.Callback() {
                    @Override
                    public void onLoaded(Bitmap bitmap, String title) {
                        backdrop.setWallpaperBitmap(bitmap);
                        saver.setWallpaperBitmap(bitmap);
                        updateWallpaperBadge(source, resTier, builtInIdx, title);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.w(TAG, "Failed to load wallpaper, fallback to gradient", e);
                        backdrop.setWallpaperBitmap(null);
                        saver.setWallpaperBitmap(null);
                        wallpaperLabel.setVisibility(View.GONE);
                    }
                });

        // 必应每日精选：若有网络后台异步更新今日壁纸
        if (source == WallpaperConfig.SOURCE_BING_DAILY) {
            BingWallpaperClient.fetchTodayWallpaper(this, resTier, new BingWallpaperClient.Callback() {
                @Override
                public void onSuccess(File cacheFile, String title) {
                    if (layout.wallpaperSource() == WallpaperConfig.SOURCE_BING_DAILY) {
                        WallpaperManager.getInstance().loadWallpaper(HomeActivity.this,
                                WallpaperConfig.SOURCE_BING_DAILY, layout.wallpaperResolution(), 0,
                                new WallpaperManager.Callback() {
                                    @Override
                                    public void onLoaded(Bitmap bitmap, String newTitle) {
                                        backdrop.setWallpaperBitmap(bitmap);
                                        saver.setWallpaperBitmap(bitmap);
                                        updateWallpaperBadge(WallpaperConfig.SOURCE_BING_DAILY, layout.wallpaperResolution(), 0, newTitle);
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                    }
                                });
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.i(TAG, "Bing update check skipped: " + e.getMessage());
                }
            });
        }
    }

    private void updateWallpaperBadge(int source, int resTier, int builtInIdx, String title) {
        if (wallpaperLabel == null) return;
        String resStr = resTier == WallpaperConfig.RES_720P ? "720p" : (resTier == WallpaperConfig.RES_4K ? "4K" : "1080p");
        if (source == WallpaperConfig.SOURCE_BING_DAILY) {
            wallpaperLabel.setText("必应每日精选 · " + resStr);
            wallpaperLabel.setVisibility(View.VISIBLE);
        } else if (source == WallpaperConfig.SOURCE_BUILT_IN) {
            int safeIdx = Math.max(0, Math.min(builtInIdx, WallpaperConfig.BUILT_IN_COUNT - 1));
            wallpaperLabel.setText("内置风景 · " + WallpaperConfig.BUILT_IN_NAMES[safeIdx] + " · " + resStr);
            wallpaperLabel.setVisibility(View.VISIBLE);
        } else {
            wallpaperLabel.setVisibility(View.GONE);
        }
    }

    private void applyDockStyle() {
        int style = layout.dockStyle();
        dock.setDockStyle(style);
        if (style == WallpaperConfig.DOCK_STYLE_TRANSLUCENT) {
            dockContainer.setBackgroundResource(R.drawable.dock_bg_translucent);
        } else if (style == WallpaperConfig.DOCK_STYLE_MINIMAL) {
            dockContainer.setBackgroundColor(0);
        } else {
            dockContainer.setBackgroundResource(R.drawable.dock_bg);
        }
    }

    private void reload() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                long started = SystemClock.uptimeMillis();
                List<AppEntry> all = repository.queryAll();
                Set<String> hiddenNow = layout.hidden();
                List<String> dockNow = layout.dock();
                int targetDockCount = layout.dockCount();

                // 若 dock 为空或槽位不足目标数量，从全部可见应用中按顺序补齐
                if (dockNow.isEmpty() || dockNow.size() < targetDockCount) {
                    List<AppEntry> visible = AppRepository.visible(all, hiddenNow);
                    List<String> updatedDock = new ArrayList<>(dockNow);
                    for (AppEntry entry : visible) {
                        if (updatedDock.size() >= targetDockCount) {
                            break;
                        }
                        if (!updatedDock.contains(entry.pkg)) {
                            updatedDock.add(entry.pkg);
                        }
                    }
                    dockNow = updatedDock;
                    layout.setDock(dockNow);
                }

                List<AppEntry> visible = AppRepository.visible(all, hiddenNow);
                final List<AppEntry> docked = AppRepository.dock(visible, dockNow, targetDockCount);
                final List<AppEntry> gridded = AppRepository.grid(visible, docked, AppRepository.labelOrder());
                final List<AppEntry> allFinal = all;
                final Set<String> hiddenFinal = hiddenNow;
                final List<String> dockFinal = dockNow;
                final int columnsFinal = layout.columns();
                final int dockCountFinal = targetDockCount;
                final boolean clockFinal = layout.showClock();
                final long saverIdleFinal = layout.saverIdleMs();

                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) {
                            return;
                        }
                        allApps = allFinal;
                        hidden = new LinkedHashSet<>(hiddenFinal);
                        dockPkgs = new ArrayList<>(dockFinal);
                        columns = columnsFinal;
                        dockCount = dockCountFinal;
                        showClock = clockFinal;
                        saverIdleMs = saverIdleFinal;
                        applyLayout(gridded, docked, columnsFinal);
                        Log.i(TAG, "grid=" + gridded.size() + " dock=" + docked.size()
                                + " in " + (SystemClock.uptimeMillis() - started) + "ms");
                    }
                });
            }
        });
    }

    private void applyLayout(List<AppEntry> gridded, List<AppEntry> docked, int spanCount) {
        clock.setVisibility(showClock ? View.VISIBLE : View.GONE);
        if (netIcon != null) {
            netIcon.setNetworkType(DeviceInfo.networkType(this));
        }
        if (gridTitle != null) {
            gridTitle.setText("全部应用 (" + gridded.size() + ")");
        }
        grid.setColumns(spanCount);
        grid.submit(gridded);
        dock.submit(docked);

        if (!isShowingGrid) {
            dock.post(new Runnable() {
                @Override
                public void run() {
                    focusFirstDockItem();
                }
            });
        }
    }

    private void focusFirstDockItem() {
        if (dock != null && dock.getChildCount() > 0) {
            View first = dock.getChildAt(0);
            if (first != null && !first.hasFocus()) {
                first.requestFocus();
            }
        }
    }

    private void focusFirstGridItem() {
        if (grid != null && grid.getChildCount() > 0) {
            View first = grid.getChildAt(0);
            if (first != null && !first.hasFocus()) {
                first.requestFocus();
            }
        }
    }

    @Override
    public void onAppClick(AppEntry entry) {
        Launcher.launch(this, entry);
    }

    @Override
    public void onAppLongClick(AppEntry entry) {
        ActionSheet.show(this, entry, dockPkgs.contains(entry.pkg), this);
    }

    @Override
    public void onLaunch(AppEntry entry) {
        Launcher.launch(this, entry);
    }

    @Override
    public void onToggleDock(AppEntry entry) {
        List<String> updated = new ArrayList<>(dockPkgs);
        if (!updated.remove(entry.pkg)) {
            updated.add(entry.pkg);
        }
        dockPkgs = updated;
        layout.setDock(updated);
        reload();
    }

    @Override
    public void onAppInfo(AppEntry entry) {
        Launcher.appInfo(this, entry);
    }

    @Override
    public void onHide(AppEntry entry) {
        Set<String> updated = new LinkedHashSet<>(hidden);
        updated.add(entry.pkg);
        hidden = updated;
        layout.setHidden(updated);
        reload();
    }

    private void openSettings() {
        List<AppEntry> hiddenEntries = new ArrayList<>();
        for (AppEntry entry : allApps) {
            if (hidden.contains(entry.pkg)) {
                hiddenEntries.add(entry);
            }
        }
        SettingsPanel.show(this, columns, dockCount, showClock,
                layout.wallpaperSource(), layout.wallpaperResolution(),
                layout.builtInIndex(), layout.dockStyle(),
                hiddenEntries, this);
    }

    @Override
    public void onColumns(int slots) {
        columns = slots;
        layout.setColumns(slots);
        reload();
    }

    @Override
    public void onDockCount(int slots) {
        dockCount = slots;
        layout.setDockCount(slots);
        reload();
    }

    @Override
    public void onShowClock(boolean show) {
        showClock = show;
        layout.setShowClock(show);
        clock.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onUnhide(String pkg) {
        Set<String> updated = new LinkedHashSet<>(hidden);
        if (updated.remove(pkg)) {
            hidden = updated;
            layout.setHidden(updated);
            reload();
        }
    }

    @Override
    public void onWallpaperSource(int source) {
        layout.setWallpaperSource(source);
        loadCurrentWallpaper();
    }

    @Override
    public void onWallpaperResolution(int resTier) {
        layout.setWallpaperResolution(resTier);
        loadCurrentWallpaper();
    }

    @Override
    public void onBuiltInIndex(int index) {
        layout.setBuiltInIndex(index);
        if (layout.wallpaperSource() == WallpaperConfig.SOURCE_BUILT_IN) {
            loadCurrentWallpaper();
        }
    }

    @Override
    public void onDockStyle(int style) {
        layout.setDockStyle(style);
        applyDockStyle();
    }

    private void showSaver() {
        saverShown = true;
        saver.setVisibility(View.VISIBLE);
    }

    private void exitSaver() {
        saverShown = false;
        saver.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (isShowingGrid) {
            showAllAppsGrid(false);
            return;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        lastInteraction = SystemClock.uptimeMillis();
        if (saverShown) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                exitSaver();
            }
            return true;
        }

        // 统一拦截 BACK 键 (消费 DOWN 与 UP，防误退且支持网格退回首屏)
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (isShowingGrid) {
                    showAllAppsGrid(false);
                }
            }
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();

            // 1. MENU 键呼出全功能设置面板
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                openSettings();
                return true;
            }

            // 2. 全部应用网格视图下的交互流转
            if (isShowingGrid) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    View focused = getCurrentFocus();
                    View directChild = findDirectChildOf(focused, grid);
                    if (directChild != null) {
                        int pos = grid.getChildAdapterPosition(directChild);
                        if (pos >= 0 && pos < columns) {
                            showAllAppsGrid(false);
                            return true;
                        }
                    }
                }
            } else {
                // 3. 首屏常驻桌面下的交互流转：按方向下键直接进入全部应用网格
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    showAllAppsGrid(true);
                    return true;
                }
            }
        }

        return super.dispatchKeyEvent(event);
    }

    private static View findDirectChildOf(View view, View parent) {
        View current = view;
        while (current != null && current.getParent() != parent) {
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                return null;
            }
        }
        return current;
    }

    private static boolean isChildOf(View child, View parent) {
        if (child == null || parent == null) return false;
        if (child == parent) return true;
        ViewParent p = child.getParent();
        while (p != null) {
            if (p == parent) return true;
            p = p.getParent();
        }
        return false;
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        lastInteraction = SystemClock.uptimeMillis();
    }

    private int getMemoryClass() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        return manager.getMemoryClass();
    }

    @Override
    protected void onResume() {
        super.onResume();
        lastInteraction = SystemClock.uptimeMillis();
        main.removeCallbacks(idleCheck);
        main.postDelayed(idleCheck, IDLE_CHECK_MS);
        if (netIcon != null) {
            netIcon.setNetworkType(DeviceInfo.networkType(this));
        }
        fetchWeatherAsync();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            iconLoader.clear();
        }
    }

    @Override
    protected void onPause() {
        main.removeCallbacks(idleCheck);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        iconLoader.shutdown();
        iconLoader.clear();
        super.onDestroy();
    }
}

package io.github.flyfly6.aerialclean;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

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
import io.github.flyfly6.aerialclean.ui.ClockView;
import io.github.flyfly6.aerialclean.ui.DockView;
import io.github.flyfly6.aerialclean.ui.GridView;
import io.github.flyfly6.aerialclean.ui.SettingsPanel;
import io.github.flyfly6.aerialclean.ui.TileAdapter;
import io.github.flyfly6.aerialclean.ui.TileView;

/**
 * 唯一的 HOME Activity。只做装配、后台枚举、焦点与按键分发。
 *
 * <p>偏好只由主线程写；后台线程只读 SharedPreferences 快照，避免跨线程共享可变状态。
 */
public class HomeActivity extends Activity
        implements TileAdapter.Listener, ActionSheet.Listener, SettingsPanel.Listener {

    private static final String TAG = "AerialClean";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private AppRepository repository;
    private IconLoader iconLoader;
    private LayoutStore layout;
    private GridView grid;
    private DockView dock;
    private ClockView clock;
    private TextView netStatus;

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

        grid = (GridView) findViewById(R.id.grid);
        grid.setIconLoader(iconLoader);
        grid.setListener(this);

        clock = (ClockView) findViewById(R.id.clock);
        netStatus = (TextView) findViewById(R.id.net_status);

        dock = (DockView) findViewById(R.id.dock);
        dock.setIconLoader(iconLoader);
        dock.setListener(this);

        findViewById(R.id.btn_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                openSettings();
            }
        });

        reload();
    }

    private void reload() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                long started = SystemClock.uptimeMillis();
                List<AppEntry> all = repository.queryAll();
                Set<String> hiddenNow = layout.hidden();
                List<String> dockNow = layout.dock();
                if (dockNow.isEmpty()) {
                    // 首次启动把默认 dock 落盘，否则用户清空 dock 后会被默认值重新填满。
                    dockNow = AppRepository.defaultDock(all, layout.dockCount());
                    layout.setDock(dockNow);
                }
                List<AppEntry> visible = AppRepository.visible(all, hiddenNow);
                final List<AppEntry> docked = AppRepository.dock(visible, dockNow, layout.dockCount());
                final List<AppEntry> gridded = AppRepository.grid(visible, docked, AppRepository.labelOrder());
                final List<AppEntry> allFinal = all;
                final Set<String> hiddenFinal = hiddenNow;
                final List<String> dockFinal = dockNow;
                final int columnsFinal = layout.columns();
                final int dockCountFinal = layout.dockCount();
                final boolean clockFinal = layout.showClock();
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
        netStatus.setText(DeviceInfo.networkLabel(this));
        grid.setColumns(spanCount);
        grid.submit(gridded);
        dock.submit(docked);
        grid.post(new Runnable() {
            @Override
            public void run() {
                View first = grid.getChildAt(0);
                if (first != null && !first.hasFocus()) {
                    first.requestFocus();
                }
            }
        });
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
        SettingsPanel.show(this, columns, dockCount, showClock, hiddenEntries, this);
    }

    @Override
    public void onColumns(int slots) {
        columns = slots;
        persistSettings();
        reload();
        openSettings();
    }

    @Override
    public void onDockCount(int slots) {
        dockCount = slots;
        persistSettings();
        reload();
        openSettings();
    }

    @Override
    public void onShowClock(boolean show) {
        showClock = show;
        persistSettings();
        reload();
        openSettings();
    }

    @Override
    public void onUnhide(String pkg) {
        Set<String> updated = new LinkedHashSet<>(hidden);
        updated.remove(pkg);
        hidden = updated;
        layout.setHidden(updated);
        reload();
        openSettings();
    }

    private void persistSettings() {
        layout.set(columns, dockCount, showClock);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            AppEntry focused = focusedEntry();
            if (focused != null) {
                onAppLongClick(focused);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private AppEntry focusedEntry() {
        View focus = getCurrentFocus();
        return focus instanceof TileView ? ((TileView) focus).entry() : null;
    }

    private int getMemoryClass() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        return manager.getMemoryClass();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从别的应用回来时网络可能已经变了。
        netStatus.setText(DeviceInfo.networkLabel(this));
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            iconLoader.clear();
        }
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        iconLoader.shutdown();
        iconLoader.clear();
        super.onDestroy();
    }
}

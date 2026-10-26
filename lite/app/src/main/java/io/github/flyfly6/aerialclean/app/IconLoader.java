package io.github.flyfly6.aerialclean.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 图标后台加载 + 内存缓存。缓存上限按 {@code getMemoryClass()/8} 计算。
 */
public final class IconLoader {

    private static final String TAG = "AerialClean";
    private static final int MIN_CACHE_KB = 4 * 1024;

    public interface Callback {
        void onIcon(String pkg, Bitmap icon);
    }

    private final Context context;
    private final LruCache<String, Bitmap> cache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public IconLoader(Context context, int memoryClassMb) {
        this.context = context.getApplicationContext();
        this.cache = new LruCache<String, Bitmap>(Math.max(MIN_CACHE_KB, memoryClassMb * 1024 / 8)) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    public Bitmap peek(String pkg) {
        return cache.get(pkg);
    }

    /** 命中缓存时回调发生在当前线程。 */
    public void load(final String pkg, final int sizePx, final Callback callback) {
        Bitmap cached = cache.get(pkg);
        if (cached != null) {
            callback.onIcon(pkg, cached);
            return;
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = render(pkg, sizePx);
                if (bitmap == null) {
                    return;
                }
                cache.put(pkg, bitmap);
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onIcon(pkg, bitmap);
                    }
                });
            }
        });
    }

    private Bitmap render(String pkg, int sizePx) {
        try {
            Drawable drawable = context.getPackageManager().getApplicationIcon(pkg);
            Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, sizePx, sizePx);
            drawable.draw(canvas);
            return bitmap;
        } catch (PackageManager.NameNotFoundException | OutOfMemoryError e) {
            // 应用已卸载或内存吃紧：保留字母磁贴即可。
            Log.w(TAG, "icon failed: " + pkg, e);
            return null;
        }
    }

    public void clear() {
        cache.evictAll();
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}

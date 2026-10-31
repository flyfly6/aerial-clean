package io.github.flyfly6.aerialclean.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Random;

import io.github.flyfly6.aerialclean.R;

/**
 * 屏保：壁纸图片背景 + 75% 沉浸深色遮罩 + 大时钟随机漂移防烧屏。
 *
 * <p>兼容 Android 4.4，低内存消耗。
 */
public class ClockSaver extends FrameLayout {

    private static final long DRIFT_MS = 60_000L;
    private static final long DATE_MS = 30_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final DateFormat dateFormat =
            android.text.format.DateFormat.getMediumDateFormat(getContext());

    private View clockBox;
    private TextClock timeView;
    private TextView dateView;
    private int driftPx;

    private Bitmap wallpaperBitmap;
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect srcRect = new Rect();
    private final RectF dstRect = new RectF();

    private final Runnable driftTick = new Runnable() {
        @Override
        public void run() {
            drift();
            handler.postDelayed(this, DRIFT_MS);
        }
    };

    private final Runnable dateTick = new Runnable() {
        @Override
        public void run() {
            updateDateIfShowing();
            handler.postDelayed(this, DATE_MS);
        }
    };

    public ClockSaver(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setBackgroundColor(context.getResources().getColor(R.color.saver_backdrop));
        setClickable(true);
        LayoutInflater.from(context).inflate(R.layout.saver_clock, this, true);
        clockBox = findViewById(R.id.saver_clock_box);
        timeView = (TextClock) findViewById(R.id.saver_time);
        dateView = (TextView) findViewById(R.id.saver_date);
        driftPx = getResources().getDimensionPixelSize(R.dimen.saver_drift);

        try {
            Typeface outfit = Typeface.createFromAsset(context.getAssets(), "fonts/outfit.ttf");
            if (outfit != null && timeView != null) {
                timeView.setTypeface(outfit);
            }
        } catch (Exception ignored) {
        }
    }

    public void setWallpaperBitmap(Bitmap bitmap) {
        this.wallpaperBitmap = bitmap;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (wallpaperBitmap != null && !wallpaperBitmap.isRecycled()) {
            int bw = wallpaperBitmap.getWidth();
            int bh = wallpaperBitmap.getHeight();
            int vw = getWidth();
            int vh = getHeight();
            if (vw > 0 && vh > 0) {
                float scale = Math.max((float) vw / bw, (float) vh / bh);
                int scaledW = (int) (bw * scale);
                int scaledH = (int) (bh * scale);
                int dx = (vw - scaledW) / 2;
                int dy = (vh - scaledH) / 2;
                dstRect.set(dx, dy, dx + scaledW, dy + scaledH);
                srcRect.set(0, 0, bw, bh);
                canvas.drawBitmap(wallpaperBitmap, srcRect, dstRect, bitmapPaint);
                // 覆盖 75% 深色遮罩，防烧屏并凸显纯白漂移大时钟
                canvas.drawColor(0xBF000000);
            }
        } else {
            super.onDraw(canvas);
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            updateDateIfShowing();
            drift();
            handler.removeCallbacks(driftTick);
            handler.removeCallbacks(dateTick);
            handler.postDelayed(driftTick, DRIFT_MS);
            handler.postDelayed(dateTick, DATE_MS);
        } else {
            handler.removeCallbacks(driftTick);
            handler.removeCallbacks(dateTick);
        }
    }

    private void drift() {
        if (clockBox == null) {
            return;
        }
        int dx = random.nextInt(driftPx * 2 + 1) - driftPx;
        int dy = random.nextInt(driftPx * 2 + 1) - driftPx;
        clockBox.setTranslationX(dx);
        clockBox.setTranslationY(dy);
    }

    private void updateDateIfShowing() {
        if (dateView != null && isShown()) {
            dateView.setText(dateFormat.format(new Date()));
        }
    }
}

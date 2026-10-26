package io.github.flyfly6.aerialclean.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Random;

import io.github.flyfly6.aerialclean.R;

/**
 * 屏保：全屏黑底 + 大时钟，每分钟把时钟块随机偏移 ±drift，避免烧屏。
 *
 * <p>用应用内覆盖层而不是 DreamService：4.4 上部分厂商没有屏保入口。
 */
public class ClockSaver extends FrameLayout {

    private static final long DRIFT_MS = 60_000L;
    private static final long DATE_MS = 30_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final DateFormat dateFormat =
            android.text.format.DateFormat.getMediumDateFormat(getContext());

    private View clockBox;
    private TextView dateView;
    private int driftPx;

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
        setBackgroundColor(context.getResources().getColor(R.color.saver_backdrop));
        // 盖住桌面时把点击也吃掉，避免穿透到下面的磁贴。
        setClickable(true);
        LayoutInflater.from(context).inflate(R.layout.saver_clock, this, true);
        clockBox = findViewById(R.id.saver_clock_box);
        dateView = (TextView) findViewById(R.id.saver_date);
        driftPx = context.getResources().getDimensionPixelSize(R.dimen.saver_drift);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateDate();
        handler.removeCallbacks(driftTick);
        handler.removeCallbacks(dateTick);
        handler.postDelayed(driftTick, DRIFT_MS);
        handler.postDelayed(dateTick, DATE_MS);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(driftTick);
        handler.removeCallbacks(dateTick);
        super.onDetachedFromWindow();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (clockBox == null) {
            return;
        }
        if (visibility == VISIBLE) {
            updateDate();
        } else {
            // 退出屏保时回到中心，下次从正中开始漂移。
            clockBox.setTranslationX(0f);
            clockBox.setTranslationY(0f);
        }
    }

    private void drift() {
        if (getVisibility() != VISIBLE) {
            return;
        }
        clockBox.setTranslationX((random.nextFloat() * 2f - 1f) * driftPx);
        clockBox.setTranslationY((random.nextFloat() * 2f - 1f) * driftPx);
    }

    private void updateDateIfShowing() {
        if (getVisibility() == VISIBLE) {
            updateDate();
        }
    }

    private void updateDate() {
        if (dateView != null) {
            dateView.setText(dateFormat.format(new Date()));
        }
    }
}

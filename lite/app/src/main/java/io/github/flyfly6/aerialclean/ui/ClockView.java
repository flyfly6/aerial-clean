package io.github.flyfly6.aerialclean.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

import io.github.flyfly6.aerialclean.R;

/**
 * 顶栏时钟：时间由 TextClock 自己走，这里只负责日期，30s 刷一次跨天。
 */
public class ClockView extends LinearLayout {

    private static final long TICK_MS = 30_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    /** 走系统 locale 的中等长度日期（英文 "Oct 26, 2026"，中文 "2026年10月26日"）。 */
    private final DateFormat dateFormat =
            android.text.format.DateFormat.getMediumDateFormat(getContext());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateDate();
            handler.postDelayed(this, TICK_MS);
        }
    };

    private TextView dateView;

    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        dateView = (TextView) findViewById(R.id.clock_date);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateDate();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, TICK_MS);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    private void updateDate() {
        if (dateView != null) {
            dateView.setText(dateFormat.format(new Date()));
        }
    }
}

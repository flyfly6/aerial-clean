package io.github.flyfly6.aerialclean.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.util.AttributeSet;
import android.view.View;

/**
 * 电视端纯矢量网络图标（WiFi / Ethernet / Disconnected）。
 *
 * <p>兼容 Android 4.4，原生 Canvas 硬件加速绘制，零资源依赖，像素级锐利。
 */
public class NetworkIconView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();
    private int currentType = -1; // -1: 未连接, TYPE_WIFI, TYPE_ETHERNET

    public NetworkIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setNetworkType(int type) {
        if (this.currentType != type) {
            this.currentType = type;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        paint.reset();
        paint.setAntiAlias(true);

        if (currentType == ConnectivityManager.TYPE_WIFI) {
            // 绘制天蓝色 WiFi 扇形波纹 (3 弧线 + 1 圆点)
            paint.setColor(Color.parseColor("#38BDF8"));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(w * 0.09f);

            float cx = w * 0.5f;
            float cy = h * 0.82f;

            // 外弧
            arcRect.set(cx - w * 0.42f, cy - w * 0.42f, cx + w * 0.42f, cy + w * 0.42f);
            canvas.drawArc(arcRect, 225, 90, false, paint);

            // 中弧
            arcRect.set(cx - w * 0.26f, cy - w * 0.26f, cx + w * 0.26f, cy + w * 0.26f);
            canvas.drawArc(arcRect, 225, 90, false, paint);

            // 内点
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, w * 0.08f, paint);

        } else if (currentType == ConnectivityManager.TYPE_ETHERNET) {
            // 绘制以太网图标 (两矩形插口互联)
            paint.setColor(Color.parseColor("#38BDF8"));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(w * 0.08f);

            // 主方框
            float pad = w * 0.18f;
            arcRect.set(pad, pad + h * 0.05f, w - pad, h - pad - h * 0.05f);
            canvas.drawRoundRect(arcRect, w * 0.12f, w * 0.12f, paint);

            // 底部插口小方块
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(w * 0.38f, h - pad, w * 0.62f, h - pad * 0.4f, paint);

        } else {
            // 未连接：灰色断开点
            paint.setColor(Color.parseColor("#64748B"));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(w * 0.5f, h * 0.5f, w * 0.18f, paint);
        }
    }
}

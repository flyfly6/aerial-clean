package io.github.flyfly6.aerialclean.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * 桌面背景：冷色纵向渐变 + 底部一圈微弱提亮。
 *
 * <p>两个 Shader 只在尺寸变化时创建，onDraw 里不分配对象。
 */
public class BackdropView extends View {

    private static final int TOP_COLOR = 0xFF16202A;
    private static final int BOTTOM_COLOR = 0xFF0B0E12;
    private static final int GLOW_CENTER = 0x332C5364;
    private static final int GLOW_EDGE = 0x002C5364;

    private final Paint paint = new Paint();
    private Shader base;
    private Shader glow;

    public BackdropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        base = new LinearGradient(0, 0, 0, h, TOP_COLOR, BOTTOM_COLOR, Shader.TileMode.CLAMP);
        float radius = Math.max(w, h) * 0.6f;
        glow = new RadialGradient(w / 2f, h * 1.05f, radius,
                new int[]{GLOW_CENTER, GLOW_EDGE}, null, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (base == null) {
            return;
        }
        paint.setShader(base);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(glow);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }
}

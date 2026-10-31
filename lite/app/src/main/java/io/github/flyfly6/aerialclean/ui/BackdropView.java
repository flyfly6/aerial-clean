package io.github.flyfly6.aerialclean.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * 桌面背景视图：
 * 1. 支持绘制位图静态壁纸（居中裁剪缩放 + 顶部/底部暗角遮罩保障文字对比度）；
 * 2. 支持全部应用展开时的深色遮罩平滑淡入（setDimRatio）；
 * 3. 位图为空时自动平滑回退至冷色纵向渐变 + 底部高光；
 * 4. onDraw 中严格零对象分配，保证 60fps。
 */
public class BackdropView extends View {

    private static final int TOP_COLOR = 0xFF16202A;
    private static final int BOTTOM_COLOR = 0xFF0B0E12;
    private static final int GLOW_CENTER = 0x332C5364;
    private static final int GLOW_EDGE = 0x002C5364;

    private final Paint paint = new Paint();
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint dimPaint = new Paint();

    private final Rect srcRect = new Rect();
    private final Rect dstRect = new Rect();

    private Shader baseGradient;
    private Shader glowGradient;
    private Shader scrimGradient;

    private Bitmap wallpaperBitmap;
    private float dimRatio = 0.0f;

    public BackdropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
    }

    public void setWallpaperBitmap(Bitmap bitmap) {
        this.wallpaperBitmap = bitmap;
        updateCropRect();
        invalidate();
    }

    public void setDimRatio(float ratio) {
        float clamped = Math.max(0.0f, Math.min(ratio, 1.0f));
        if (this.dimRatio != clamped) {
            this.dimRatio = clamped;
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        dstRect.set(0, 0, w, h);

        baseGradient = new LinearGradient(0, 0, 0, h, TOP_COLOR, BOTTOM_COLOR, Shader.TileMode.CLAMP);
        float radius = Math.max(w, h) * 0.6f;
        glowGradient = new RadialGradient(w / 2f, h * 1.05f, radius,
                new int[]{GLOW_CENTER, GLOW_EDGE}, null, Shader.TileMode.CLAMP);

        // 静态壁纸上方的暗角 Scrim：顶部压暗保证时钟清晰，底部微暗保证 Dock 悬浮感
        scrimGradient = new LinearGradient(0, 0, 0, h,
                new int[]{0x99000000, 0x1A000000, 0xB3000000},
                new float[]{0.0f, 0.4f, 1.0f}, Shader.TileMode.CLAMP);

        updateCropRect();
    }

    private void updateCropRect() {
        if (wallpaperBitmap == null || wallpaperBitmap.isRecycled()) {
            return;
        }
        int bw = wallpaperBitmap.getWidth();
        int bh = wallpaperBitmap.getHeight();
        int vw = getWidth();
        int vh = getHeight();
        if (vw <= 0 || vh <= 0 || bw <= 0 || bh <= 0) {
            return;
        }

        // Center Crop 计算
        float vAspect = (float) vw / (float) vh;
        float bAspect = (float) bw / (float) bh;

        if (bAspect > vAspect) {
            // 位图更宽：上下填满，左右居中裁剪
            int cropW = (int) (bh * vAspect);
            int left = (bw - cropW) / 2;
            srcRect.set(left, 0, left + cropW, bh);
        } else {
            // 位图更高：左右填满，上下居中裁剪
            int cropH = (int) (bw / vAspect);
            int top = (bh - cropH) / 2;
            srcRect.set(0, top, bw, top + cropH);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        if (wallpaperBitmap != null && !wallpaperBitmap.isRecycled()) {
            // 1. 绘制壁纸
            canvas.drawBitmap(wallpaperBitmap, srcRect, dstRect, bitmapPaint);

            // 2. 绘制暗角 Scrim
            if (scrimGradient != null) {
                paint.setShader(scrimGradient);
                canvas.drawRect(0, 0, w, h, paint);
            }
        } else {
            // 回退到冷色渐变
            if (baseGradient != null) {
                paint.setShader(baseGradient);
                canvas.drawRect(0, 0, w, h, paint);
                paint.setShader(glowGradient);
                canvas.drawRect(0, 0, w, h, paint);
            }
        }

        // 3. 展开全部应用时的深色遮罩
        if (dimRatio > 0.001f) {
            int alpha = (int) (dimRatio * 255);
            dimPaint.setColor(Color.argb(alpha, 11, 14, 20)); // 深灰黑遮罩 #0B0E14
            canvas.drawRect(0, 0, w, h, dimPaint);
        }
    }
}

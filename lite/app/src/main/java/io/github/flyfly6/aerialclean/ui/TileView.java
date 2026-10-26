package io.github.flyfly6.aerialclean.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.github.flyfly6.aerialclean.R;
import io.github.flyfly6.aerialclean.app.AppEntry;
import io.github.flyfly6.aerialclean.app.IconLoader;

/**
 * 网格里的一个应用磁贴：图标 + 名称，图标未就绪时显示首字母。
 *
 * <p>磁贴会被 RecyclerView 复用，所以图标回调必须校验包名。
 */
public class TileView extends LinearLayout {

    private static final float FOCUS_SCALE = 1.08f;
    private static final long FOCUS_ANIM_MS = 120L;

    private View iconBox;
    private ImageView iconView;
    private TextView letterView;
    private TextView labelView;
    private AppEntry entry;
    private int iconSizePx;

    public TileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(android.view.Gravity.CENTER_HORIZONTAL);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        iconBox = findViewById(R.id.tile_icon_box);
        iconView = findViewById(R.id.tile_icon);
        letterView = findViewById(R.id.tile_letter);
        labelView = findViewById(R.id.tile_label);
        iconSizePx = getResources().getDimensionPixelSize(R.dimen.tile_icon);
    }

    public AppEntry entry() {
        return entry;
    }

    public void bind(AppEntry entry, IconLoader icons) {
        this.entry = entry;
        if (!isFocused()) {
            // 复用时可能继承上一个聚焦磁贴的放大状态。
            setScaleX(1f);
            setScaleY(1f);
        }
        labelView.setText(entry.label);
        letterView.setText(firstLetter(entry.label));
        iconBox.setBackgroundColor(avatarColor(entry.pkg));
        Bitmap cached = icons.peek(entry.pkg);
        applyIcon(cached);
        if (cached == null) {
            icons.load(entry.pkg, iconSizePx, this::onIconLoaded);
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        animate().cancel();
        animate()
                .scaleX(gainFocus ? FOCUS_SCALE : 1f)
                .scaleY(gainFocus ? FOCUS_SCALE : 1f)
                .setDuration(FOCUS_ANIM_MS)
                .start();
    }

    private void onIconLoaded(String pkg, Bitmap bitmap) {
        if (entry != null && entry.pkg.equals(pkg)) {
            applyIcon(bitmap);
        }
    }

    private void applyIcon(Bitmap bitmap) {
        if (bitmap == null) {
            iconView.setVisibility(GONE);
            letterView.setVisibility(VISIBLE);
        } else {
            iconView.setImageBitmap(bitmap);
            iconView.setVisibility(VISIBLE);
            letterView.setVisibility(GONE);
        }
    }

    private static String firstLetter(CharSequence label) {
        if (label == null || label.length() == 0) {
            return "?";
        }
        return String.valueOf(Character.toUpperCase(label.charAt(0)));
    }

    private static int avatarColor(String pkg) {
        int hue = Math.abs(pkg.hashCode()) % 360;
        return Color.HSVToColor(new float[]{hue, 0.45f, 0.38f});
    }
}

package io.github.flyfly6.aerialclean.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import io.github.flyfly6.aerialclean.R;
import io.github.flyfly6.aerialclean.app.AppEntry;
import io.github.flyfly6.aerialclean.app.IconLoader;

/**
 * 底部 dock：单行横向列表。
 */
public class DockView extends RecyclerView {

    private final TileAdapter adapter = new TileAdapter(R.layout.item_tile_dock);

    public DockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        setHasFixedSize(true);
        setClipToPadding(false);
        // 聚焦放大 1.08 会越出磁贴边界，不裁才不会切掉一圈。
        setClipChildren(false);
        setAdapter(adapter);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
    }

    public void setIconLoader(IconLoader icons) {
        adapter.setIconLoader(icons);
    }

    public void setListener(TileAdapter.Listener listener) {
        adapter.setListener(listener);
    }

    public void submit(List<AppEntry> entries) {
        adapter.setItems(entries);
        requestLayout();
    }

    public void clear() {
        adapter.setItems(Collections.<AppEntry>emptyList());
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int count = adapter.getItemCount();
        if (count > 0) {
            int tileWidth = getResources().getDimensionPixelSize(R.dimen.dock_tile_width);
            int tileGap = getResources().getDimensionPixelSize(R.dimen.tile_gap);
            int exactWidth = count * (tileWidth + tileGap * 2) + getPaddingLeft() + getPaddingRight();
            widthSpec = MeasureSpec.makeMeasureSpec(exactWidth, MeasureSpec.EXACTLY);
        }
        int tileHeight = getResources().getDimensionPixelSize(R.dimen.tile_height);
        int exactHeight = tileHeight + getPaddingTop() + getPaddingBottom();
        heightSpec = MeasureSpec.makeMeasureSpec(exactHeight, MeasureSpec.EXACTLY);
        super.onMeasure(widthSpec, heightSpec);
    }

    public void setDockStyle(int style) {
        // 背景由外层 dock_container 统一绘制，自身保持透明，避免双层绘制导致不透明度翻倍
        setBackgroundColor(0);
    }
}

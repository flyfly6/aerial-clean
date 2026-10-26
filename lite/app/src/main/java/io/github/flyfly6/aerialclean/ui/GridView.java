package io.github.flyfly6.aerialclean.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import io.github.flyfly6.aerialclean.app.AppEntry;
import io.github.flyfly6.aerialclean.app.IconLoader;

/**
 * 应用网格。只负责摆放与焦点，枚举、排序都在 app 包。
 */
public class GridView extends RecyclerView {

    private final TileAdapter adapter = new TileAdapter();

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutManager(new GridLayoutManager(context, 5));
        setHasFixedSize(true);
        setClipToPadding(false);
        setAdapter(adapter);
        // 磁贴先取焦点；RV 本身只在没有子项时接收焦点。
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
    }

    public void setIconLoader(IconLoader icons) {
        adapter.setIconLoader(icons);
    }

    public void setListener(TileAdapter.Listener listener) {
        adapter.setListener(listener);
    }

    public void setColumns(int columns) {
        ((GridLayoutManager) getLayoutManager()).setSpanCount(columns);
    }

    public void submit(List<AppEntry> entries) {
        adapter.setItems(entries);
    }

    public void clear() {
        adapter.setItems(Collections.<AppEntry>emptyList());
    }
}

package io.github.flyfly6.aerialclean.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import io.github.flyfly6.aerialclean.app.AppEntry;
import io.github.flyfly6.aerialclean.app.IconLoader;

/**
 * 底部 dock：单行横向列表。
 */
public class DockView extends RecyclerView {

    private final TileAdapter adapter = new TileAdapter();

    public DockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        setHasFixedSize(true);
        setClipToPadding(false);
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
    }

    public void clear() {
        adapter.setItems(Collections.<AppEntry>emptyList());
    }
}

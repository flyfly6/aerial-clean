package io.github.flyfly6.aerialclean.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import io.github.flyfly6.aerialclean.R;
import io.github.flyfly6.aerialclean.app.AppEntry;
import io.github.flyfly6.aerialclean.app.IconLoader;

/**
 * 磁贴适配器。网格与 dock 共用，差别只在布局管理器。
 */
public final class TileAdapter extends RecyclerView.Adapter<TileAdapter.Holder> {

    public interface Listener {
        void onAppClick(AppEntry entry);

        void onAppLongClick(AppEntry entry);
    }

    private List<AppEntry> items = Collections.emptyList();
    private IconLoader icons;
    private Listener listener;

    public void setItems(List<AppEntry> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setIconLoader(IconLoader icons) {
        this.icons = icons;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tile, parent, false);
        final Holder holder = new Holder((TileView) view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                if (listener != null && holder.tile.entry() != null) {
                    listener.onAppClick(holder.tile.entry());
                }
            }
        });
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View pressed) {
                if (listener == null || holder.tile.entry() == null) {
                    return false;
                }
                listener.onAppLongClick(holder.tile.entry());
                return true;
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        holder.tile.bind(items.get(position), icons);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TileView tile;

        Holder(TileView tile) {
            super(tile);
            this.tile = tile;
        }
    }
}

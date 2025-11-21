package com.example.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.models.Item;
import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> implements Filterable {

    private Context context;
    private List<Item> items;           // full list
    private List<Item> filteredItems;   // filtered list
    private OnItemClickListener listener;

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    // Constructor
    public ItemAdapter(Context context, List<Item> items, OnItemClickListener listener) {
        this.context = context;
        this.items = items;
        this.filteredItems = new ArrayList<>(items);
        this.listener = listener;
    }

    public void updateItems(List<Item> newItems) {
        this.items = new ArrayList<>(newItems);
        this.filteredItems = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.items_grid_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = filteredItems.get(position);
        holder.tvName.setText(item.getName());
        holder.tvDesc.setText(item.getDescription());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Item> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(items);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    // First pass: find exact/partial matches
                    for (Item item : items) {
                        if (item.getName().toLowerCase().contains(filterPattern) ||
                                item.getDescription().toLowerCase().contains(filterPattern)) {
                            filtered.add(item);
                        }
                    }

                    // If no matches found, show all OTHER items (exclude nothing)
                    if (filtered.isEmpty()) {
                        for (Item item : items) {
                            // Add all items that DON'T match the search
                            // This shows alternatives when exact match not found
                            filtered.add(item);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredItems.clear();
                filteredItems.addAll((List<Item>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    // ViewHolder class
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvDesc;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvDesc = itemView.findViewById(R.id.tvItemDesc);
        }
    }
}
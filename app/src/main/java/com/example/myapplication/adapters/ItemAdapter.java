package com.example.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.myapplication.models.Item;
import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends BaseAdapter implements Filterable {

    private Context context;
    private List<Item> items;           // full list
    private List<Item> filteredItems;   // filtered list

    // Constructor
    public ItemAdapter(Context context, List<Item> items) {
        this.context = context;
        this.items = items;
        this.filteredItems = new ArrayList<>(items);
    }
    public void updateItems(List<Item> newItems) {
        this.items = new ArrayList<>(newItems);       // full list
        this.filteredItems = new ArrayList<>(newItems); // filtered list also updated
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filteredItems.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.items_grid_card, parent, false);
        }

        TextView tvName = convertView.findViewById(R.id.tvItemName);
        TextView tvDesc = convertView.findViewById(R.id.tvItemDesc);

        Item item = filteredItems.get(position);
        tvName.setText(item.getName());
        tvDesc.setText(item.getDescription());

        return convertView;
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
                    for (Item item : items) {
                        if (item.getName().toLowerCase().contains(filterPattern) ||
                                item.getDescription().toLowerCase().contains(filterPattern)) {
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
}


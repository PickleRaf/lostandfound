package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.models.ChatItem;
import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class InboxAdapter extends RecyclerView.Adapter<InboxAdapter.ChatViewHolder> {

    private List<ChatItem> chatList;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(ChatItem chatItem);
    }

    public InboxAdapter(List<ChatItem> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_items_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem chatItem = chatList.get(position);

        // Display: "Name - Role - Item Title"
        holder.tvChatTitle.setText(chatItem.getDisplayTitle());

        // Format timestamp as relative time
        String timeAgo = getRelativeTime(chatItem.getTimestamp());
        holder.tvTimestamp.setText(timeAgo);

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chatItem));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    // Get relative time string (e.g., "2 hours ago", "Yesterday")
    private String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long mins = TimeUnit.MILLISECONDS.toMinutes(diff);
            return mins + (mins == 1 ? " minute ago" : " minutes ago");
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (diff < TimeUnit.DAYS.toMillis(2)) {
            return "Yesterday";
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + " days ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvChatTitle;
        TextView tvTimestamp;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatTitle = itemView.findViewById(R.id.tvChatTitle);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
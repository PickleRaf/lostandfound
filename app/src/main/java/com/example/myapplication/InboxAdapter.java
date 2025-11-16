package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem chatItem = chatList.get(position);
        holder.tvChatTitle.setText(chatItem.getTitle());

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date(chatItem.getTimestamp()));
        holder.tvTimestamp.setText(dateStr);

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chatItem));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
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
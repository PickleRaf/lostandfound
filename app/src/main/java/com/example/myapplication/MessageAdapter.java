package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages;
    private String currentUserId;

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_item, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        holder.tvMessage.setText(message.getText());

        // Simple alignment: right if sent by current user, left otherwise
        if (message.getSenderId().equals(currentUserId)) {
            holder.tvMessage.setBackgroundResource(R.drawable.bg_message_sent); // create a drawable
            holder.tvMessage.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        } else {
            holder.tvMessage.setBackgroundResource(R.drawable.bg_message_received); // create a drawable
            holder.tvMessage.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageText);
        }
    }
}

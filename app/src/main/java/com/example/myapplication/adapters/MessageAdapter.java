package com.example.myapplication.adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.models.Message;
import com.example.myapplication.R;

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
                .inflate(R.layout.chat_item_message_bubble, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.tvMessage.setText(message.getText());

        // Get the parent layout params
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        if (message.getSenderId().equals(currentUserId)) {
            // Current user's message - RIGHT side (sent)
            params.gravity = Gravity.END;
            params.setMargins(100, 8, 16, 8); // Left margin to push right
            holder.tvMessage.setBackgroundResource(R.drawable.bg_message_sent);
        } else {
            // Other user's message - LEFT side (received)
            params.gravity = Gravity.START;
            params.setMargins(16, 8, 100, 8); // Right margin to push left
            holder.tvMessage.setBackgroundResource(R.drawable.bg_message_received);
        }

        holder.tvMessage.setLayoutParams(params);
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
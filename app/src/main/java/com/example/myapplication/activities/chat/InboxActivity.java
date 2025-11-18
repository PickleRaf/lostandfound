package com.example.myapplication.activities.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.models.ChatItem;
import com.example.myapplication.adapters.InboxAdapter;
import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class InboxActivity extends AppCompatActivity {

    // UI components
    private RecyclerView recyclerViewChats;
    private InboxAdapter adapter;
    private ProgressBar progressBar; // PHASE 1: Loading indicator
    private TextView tvEmptyState; // PHASE 1: Empty state message

    // Data
    private List<ChatItem> chatList;

    // Firebase
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_inbox);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Setup UI elements
        recyclerViewChats = findViewById(R.id.recyclerViewChats);
        progressBar = findViewById(R.id.progressBarInbox);
        tvEmptyState = findViewById(R.id.tvEmptyStateInbox);

        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));

        // Initialize chat list and adapter
        chatList = new ArrayList<>();
        adapter = new InboxAdapter(chatList, chatItem -> {
            // When a chat is clicked, open Chat activity
            Intent intent = new Intent(InboxActivity.this, ChatActivity.class);
            intent.putExtra("chatId", chatItem.getChatId());
            startActivity(intent);
        });
        recyclerViewChats.setAdapter(adapter);

        // PHASE 2: Setup swipe-to-delete functionality
        setupSwipeToDelete();

        // Load user's chats
        loadUserChats();
    }

    // PHASE 2: Setup swipe-to-delete functionality
    private void setupSwipeToDelete() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT // Allow swiping left or right
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // We don't support moving items
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Get the position of swiped item
                int position = viewHolder.getAdapterPosition();
                ChatItem chat = chatList.get(position);

                // Archive the chat in Firestore
                db.collection("chats").document(chat.getChatId())
                        .update("archived_" + currentUserId, true)
                        .addOnSuccessListener(aVoid -> {
                            // Remove from local list and update UI
                            chatList.remove(position);
                            adapter.notifyItemRemoved(position);

                            // Show empty state if no chats left
                            if (chatList.isEmpty()) {
                                showEmptyState(true);
                            }

                            Toast.makeText(InboxActivity.this, "Chat archived",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            // Restore item if archive failed
                            adapter.notifyItemChanged(position);
                            Toast.makeText(InboxActivity.this, "Failed to archive chat",
                                    Toast.LENGTH_SHORT).show();
                        });
            }
        });

        // Attach the ItemTouchHelper to RecyclerView
        itemTouchHelper.attachToRecyclerView(recyclerViewChats);
    }

    // Load chats where current user is the claimer
    private void loadUserChats() {
        // Show loading
        showLoading(true);

        db.collection("chats")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    chatList.clear();

                    // Process each chat document
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // PHASE 2: Skip archived chats
                        Boolean archived = doc.getBoolean("archived_" + currentUserId);
                        if (archived != null && archived) {
                            continue; // Skip this chat
                        }

                        String chatId = doc.getId();
                        String title = doc.getString("title");
                        String itemId = doc.getString("itemId");
                        String userId = doc.getString("userId");
                        String reporterId = doc.getString("reporterId");

                        // Safe timestamp handling
                        long timestamp = 0;
                        try {
                            Object timestampObj = doc.get("timestamp");
                            if (timestampObj instanceof Long) {
                                timestamp = (Long) timestampObj;
                            } else if (timestampObj instanceof Double) {
                                timestamp = ((Double) timestampObj).longValue();
                            } else if (timestampObj instanceof String) {
                                timestamp = Long.parseLong((String) timestampObj);
                            }
                        } catch (Exception e) {
                            timestamp = System.currentTimeMillis(); // Fallback
                        }

                        // Create ChatItem object and add to list
                        ChatItem chatItem = new ChatItem(
                                chatId,
                                title != null ? title : "Unknown Item",
                                itemId,
                                userId,
                                reporterId,
                                timestamp
                        );
                        chatList.add(chatItem);
                    }

                    // Load chats where user is the reporter
                    loadReporterChats();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading chats: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Load chats where current user is the reporter (finder)
    private void loadReporterChats() {
        db.collection("chats")
                .whereEqualTo("reporterId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // PHASE 2: Skip archived chats
                        Boolean archived = doc.getBoolean("archived_" + currentUserId);
                        if (archived != null && archived) {
                            continue;
                        }

                        String chatId = doc.getId();

                        // Check if chat is not already in the list (avoid duplicates)
                        boolean exists = false;
                        for (ChatItem item : chatList) {
                            if (item.getChatId().equals(chatId)) {
                                exists = true;
                                break;
                            }
                        }

                        // Only add if not duplicate
                        if (!exists) {
                            String title = doc.getString("title");
                            String itemId = doc.getString("itemId");
                            String userId = doc.getString("userId");
                            String reporterId = doc.getString("reporterId");

                            // Safe timestamp handling
                            long timestamp = 0;
                            try {
                                Object timestampObj = doc.get("timestamp");
                                if (timestampObj instanceof Long) {
                                    timestamp = (Long) timestampObj;
                                } else if (timestampObj instanceof Double) {
                                    timestamp = ((Double) timestampObj).longValue();
                                } else if (timestampObj instanceof String) {
                                    timestamp = Long.parseLong((String) timestampObj);
                                }
                            } catch (Exception e) {
                                timestamp = System.currentTimeMillis();
                            }

                            ChatItem chatItem = new ChatItem(
                                    chatId,
                                    title != null ? title : "Unknown Item",
                                    itemId,
                                    userId,
                                    reporterId,
                                    timestamp
                            );
                            chatList.add(chatItem);
                        }
                    }

                    // Sort by timestamp (most recent first)
                    chatList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    // Hide loading
                    showLoading(false);

                    // PHASE 1: Show empty state if no chats
                    if (chatList.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                        // Update UI
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading chats: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // PHASE 1: Show/hide loading indicator
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewChats.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // PHASE 1: Show/hide empty state message
    private void showEmptyState(boolean show) {
        if (show) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerViewChats.setVisibility(View.GONE);
            tvEmptyState.setText("No messages yet.\nStart a conversation by claiming an item!");
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerViewChats.setVisibility(View.VISIBLE);
        }
    }
}
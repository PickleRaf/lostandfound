package com.example.myapplication.activities.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.concurrent.atomic.AtomicInteger;

public class InboxActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChats;
    private InboxAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private List<ChatItem> chatList;

    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_inbox);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerViewChats = findViewById(R.id.recyclerViewChats);
        progressBar = findViewById(R.id.progressBarInbox);
        tvEmptyState = findViewById(R.id.tvEmptyStateInbox);

        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));

        chatList = new ArrayList<>();
        adapter = new InboxAdapter(chatList, chatItem -> {
            Intent intent = new Intent(InboxActivity.this, ChatActivity.class);
            intent.putExtra("chatId", chatItem.getChatId());
            startActivity(intent);
        });
        recyclerViewChats.setAdapter(adapter);

        // Swipe-to-archive removal: setupSwipeToDelete() was deleted

        loadUserChats();
    }

    // setupSwipeToDelete() method has been removed

    private void loadUserChats() {
        showLoading(true);

        db.collection("chats")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    chatList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean archived = doc.getBoolean("archived_" + currentUserId);
                        if (archived != null && archived) continue;

                        ChatItem chatItem = createChatItem(doc);
                        chatItem.setOtherUserRole("Finder");
                        chatList.add(chatItem);
                    }

                    loadReporterChats();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading chats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReporterChats() {
        db.collection("chats")
                .whereEqualTo("reporterId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean archived = doc.getBoolean("archived_" + currentUserId);
                        if (archived != null && archived) continue;

                        String chatId = doc.getId();

                        boolean exists = false;
                        for (ChatItem item : chatList) {
                            if (item.getChatId().equals(chatId)) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            ChatItem chatItem = createChatItem(doc);
                            chatItem.setOtherUserRole("Claimer");
                            chatList.add(chatItem);
                        }
                    }

                    chatList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    loadUserNames();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading chats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private ChatItem createChatItem(QueryDocumentSnapshot doc) {
        String chatId = doc.getId();
        String title = doc.getString("title");
        String itemId = doc.getString("itemId");
        String userId = doc.getString("userId");
        String reporterId = doc.getString("reporterId");

        long timestamp = 0;
        try {
            Object timestampObj = doc.get("timestamp");
            if (timestampObj instanceof Long) {
                timestamp = (Long) timestampObj;
            } else if (timestampObj instanceof Double) {
                timestamp = ((Double) timestampObj).longValue();
            } else if (timestampObj instanceof com.google.firebase.Timestamp) {
                timestamp = ((com.google.firebase.Timestamp) timestampObj).toDate().getTime();
            }
        } catch (Exception e) {
            timestamp = System.currentTimeMillis();
        }

        return new ChatItem(chatId, title != null ? title : "Unknown Item", itemId, userId, reporterId, timestamp);
    }

    private void loadUserNames() {
        if (chatList.isEmpty()) {
            showLoading(false);
            showEmptyState(true);
            return;
        }

        AtomicInteger pendingRequests = new AtomicInteger(chatList.size());

        for (ChatItem chat : chatList) {
            String otherUserId;
            if (chat.getUserId().equals(currentUserId)) {
                otherUserId = chat.getReporterId();
            } else {
                otherUserId = chat.getUserId();
            }

            db.collection("users").document(otherUserId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String firstName = userDoc.getString("firstName");
                            String lastName = userDoc.getString("lastName");

                            String displayName = "";
                            if (firstName != null && !firstName.isEmpty()) {
                                displayName = firstName;
                            }
                            if (lastName != null && !lastName.isEmpty()) {
                                displayName += (displayName.isEmpty() ? "" : " ") + lastName;
                            }

                            if (displayName.isEmpty()) {
                                displayName = "Unknown User";
                            }

                            chat.setOtherUserName(displayName);
                        } else {
                            chat.setOtherUserName("Unknown User");
                        }

                        if (pendingRequests.decrementAndGet() == 0) {
                            finishLoading();
                        }
                    })
                    .addOnFailureListener(e -> {
                        chat.setOtherUserName("Unknown User");

                        if (pendingRequests.decrementAndGet() == 0) {
                            finishLoading();
                        }
                    });
        }
    }

    private void finishLoading() {
        showLoading(false);

        if (chatList.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            adapter.notifyDataSetChanged();
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewChats.setVisibility(show ? View.GONE : View.VISIBLE);
    }

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

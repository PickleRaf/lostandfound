package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class InboxActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChats;
    private InboxAdapter adapter;
    private List<ChatItem> chatList;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerViewChats = findViewById(R.id.recyclerViewChats);
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));

        chatList = new ArrayList<>();
        adapter = new InboxAdapter(chatList, chatItem -> {
            // When a chat is clicked, open Chat activity
            Intent intent = new Intent(InboxActivity.this, Chat.class);
            intent.putExtra("chatId", chatItem.getChatId());
            startActivity(intent);
        });
        recyclerViewChats.setAdapter(adapter);

        loadUserChats();
    }

    private void loadUserChats() {
        db.collection("chats")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    chatList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
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
                            timestamp = System.currentTimeMillis(); // fallback
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
                    loadReporterChats();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur chargement chats: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReporterChats() {
        db.collection("chats")
                .whereEqualTo("reporterId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String chatId = doc.getId();

                        // Check if chat is not already in the list
                        boolean exists = false;
                        for (ChatItem item : chatList) {
                            if (item.getChatId().equals(chatId)) {
                                exists = true;
                                break;
                            }
                        }

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
                                timestamp = System.currentTimeMillis(); // fallback
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

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur chargement chats: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
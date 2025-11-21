package com.example.myapplication.activities.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.models.Message;
import com.example.myapplication.adapters.MessageAdapter;
import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private Button btnSend;
    private TextView tvChatHeader, tvEmptyState;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String currentUserId;
    private String chatId;

    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_chat);

        tvChatHeader = findViewById(R.id.tvChatHeader);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        tvEmptyState = findViewById(R.id.tvEmptyStateChat);
        progressBar = findViewById(R.id.progressBarChat);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        chatId = getIntent().getStringExtra("chatId");
        if (chatId == null) {
            Toast.makeText(this, "Error: chat ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);
        recyclerMessages.setAdapter(messageAdapter);

        // Load chat header with name and role
        loadChatHeader();
        listenForMessages();

        btnSend.setOnClickListener(v -> {
            sendMessage();
            Animation pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            btnSend.startAnimation(pulse);
        });
    }

    private void loadChatHeader() {
        DocumentReference chatDocRef = db.collection("chats").document(chatId);
        chatDocRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String oderId = doc.getString("userId");
                String reporterId = doc.getString("reporterId");

                // Determine who the other user is and their role
                String otherUserId;
                String otherUserRole;

                if (currentUserId.equals(oderId)) {
                    // Current user is the claimer, other user is the finder
                    otherUserId = reporterId;
                    otherUserRole = "Finder";
                } else {
                    // Current user is the finder, other user is the claimer
                    otherUserId = oderId;
                    otherUserRole = "Claimer";
                }

                // Fetch the other user's name
                loadOtherUserName(otherUserId, otherUserRole);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(ChatActivity.this, "Error loading chat: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void loadOtherUserName(String otherUserId, String role) {
        db.collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String displayName = "Unknown User";
                    if (userDoc.exists()) {
                        String firstName = userDoc.getString("firstName");
                        String lastName = userDoc.getString("lastName");

                        StringBuilder name = new StringBuilder();
                        if (firstName != null && !firstName.isEmpty()) {
                            name.append(firstName);
                        }
                        if (lastName != null && !lastName.isEmpty()) {
                            if (name.length() > 0) name.append(" ");
                            name.append(lastName);
                        }
                        if (name.length() > 0) {
                            displayName = name.toString();
                        }
                    }
                    // Set header as "Name / Role"
                    tvChatHeader.setText(displayName + " / " + role);
                })
                .addOnFailureListener(e -> {
                    tvChatHeader.setText("Unknown User / " + role);
                });
    }

    private void listenForMessages() {
        showLoading(true);

        CollectionReference messagesRef = db.collection("chats")
                .document(chatId)
                .collection("messages");

        messagesRef.orderBy("timestamp").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                showLoading(false);
                Toast.makeText(ChatActivity.this, "Error loading messages: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            messageList.clear();

            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    String sender = doc.getString("senderId");
                    String text = doc.getString("text");
                    long timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
                    messageList.add(new Message(sender, text, timestamp));
                }
            }

            showLoading(false);

            if (messageList.isEmpty()) {
                showEmptyState(true);
            } else {
                showEmptyState(false);
                messageAdapter.notifyDataSetChanged();
                if (messageList.size() > 0) {
                    recyclerMessages.scrollToPosition(messageList.size() - 1);
                }
            }
        });
    }

    private void sendMessage() {
        String msgText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(msgText)) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", currentUserId);
        msg.put("text", msgText);
        msg.put("timestamp", System.currentTimeMillis());

        db.collection("chats").document(chatId)
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(ref -> {
                    etMessage.setText("");
                    Animation slideOut = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
                    etMessage.startAnimation(slideOut);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ChatActivity.this, "Error sending: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean show) {
        if (show) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerMessages.setVisibility(View.GONE);
            tvEmptyState.setText("No messages yet.\nStart the conversation!");
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerMessages.setVisibility(View.VISIBLE);
        }
    }
}
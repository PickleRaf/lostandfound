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

    // UI components
    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private Button btnSend;
    private TextView tvItemName, tvEmptyState;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseFirestore db;
    private String currentUserId;
    private String chatId;

    // Data
    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_chat);

        // Link UI elements
        tvItemName = findViewById(R.id.tvItemName);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        tvEmptyState = findViewById(R.id.tvEmptyStateChat);
        progressBar = findViewById(R.id.progressBarChat);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get chat ID passed from previous screen
        chatId = getIntent().getStringExtra("chatId");
        if(chatId == null) {
            Toast.makeText(this, "Error: chat ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup RecyclerView for messages
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));

        // Stack messages from bottom (like WhatsApp)
        ((LinearLayoutManager) recyclerMessages.getLayoutManager()).setStackFromEnd(true);
        recyclerMessages.setAdapter(messageAdapter);

        // Fetch chat title (item name) from Firestore
        loadChatInfo();

        // Start listening for messages
        listenForMessages();

        // SEND BUTTON with animation
        btnSend.setOnClickListener(v -> {
            sendMessage();
            // PHASE 3: Add animation to send button
            Animation pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            btnSend.startAnimation(pulse);
        });
    }

    // Load chat information
    private void loadChatInfo() {
        DocumentReference chatDocRef = db.collection("chats").document(chatId);
        chatDocRef.get().addOnSuccessListener(doc -> {
            if(doc.exists()) {
                String title = doc.getString("title");
                if(title != null) {
                    tvItemName.setText(title);
                } else {
                    tvItemName.setText("Unknown Item");
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(ChatActivity.this, "Error loading title: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    // Real-time listener for messages
    private void listenForMessages() {
        // Show loading initially
        showLoading(true);

        CollectionReference messagesRef = db.collection("chats")
                .document(chatId)
                .collection("messages");

        // Listen for changes in messages
        messagesRef.orderBy("timestamp").addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                showLoading(false);
                Toast.makeText(ChatActivity.this, "Error loading messages: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Clear and rebuild message list
            messageList.clear();

            if (queryDocumentSnapshots != null) {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String sender = doc.getString("senderId");
                    String text = doc.getString("text");
                    long timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;

                    // Add message to list
                    messageList.add(new Message(sender, text, timestamp));
                }
            }

            // Hide loading
            showLoading(false);

            // Show empty state if no messages
            if (messageList.isEmpty()) {
                showEmptyState(true);
            } else {
                showEmptyState(false);
                // Update UI
                messageAdapter.notifyDataSetChanged();

                // Scroll to bottom to show latest message
                if (messageList.size() > 0) {
                    recyclerMessages.scrollToPosition(messageList.size() - 1);
                }
            }
        });
    }

    // Send a message
    private void sendMessage() {
        String msgText = etMessage.getText().toString().trim();

        // Don't send empty messages
        if (TextUtils.isEmpty(msgText)) return;

        // Create message data
        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", currentUserId);
        msg.put("text", msgText);
        msg.put("timestamp", System.currentTimeMillis());

        // Save to Firestore
        db.collection("chats").document(chatId)
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(documentReference -> {
                    // Clear input field
                    etMessage.setText("");

                    // PHASE 3: Animate message sending
                    Animation slideOut = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
                    etMessage.startAnimation(slideOut);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ChatActivity.this, "Error sending: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    // Show/hide loading indicator
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Show/hide empty state
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
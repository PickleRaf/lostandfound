package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chat extends AppCompatActivity {

    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private Button btnSend;
    private TextView tvItemName;

    private FirebaseFirestore db;
    private String currentUserId;
    private String chatId; // now dynamic

    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);

        tvItemName = findViewById(R.id.tvItemName);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        chatId = getIntent().getStringExtra("chatId");
        if(chatId == null) {
            Toast.makeText(this, "Erreur: chat ID manquant", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // RecyclerView setup
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        ((LinearLayoutManager) recyclerMessages.getLayoutManager()).setStackFromEnd(true);
        recyclerMessages.setAdapter(messageAdapter);

        // Fetch the chat document to get item title
        DocumentReference chatDocRef = db.collection("chats").document(chatId);
        chatDocRef.get().addOnSuccessListener(doc -> {
            if(doc.exists()) {
                String title = doc.getString("title"); // use Firestore field "title"
                if(title != null) {
                    tvItemName.setText(title);
                } else {
                    tvItemName.setText("Unknown Item");
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(Chat.this, "Erreur chargement titre: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );

        // Listen for messages
        listenForMessages();

        // Send button
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void listenForMessages() {
        CollectionReference messagesRef = db.collection("chats")
                .document(chatId)
                .collection("messages");

        messagesRef.orderBy("timestamp").addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Toast.makeText(Chat.this, "Erreur chargement messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            messageList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String sender = doc.getString("senderId");
                String text = doc.getString("text");
                long timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
                messageList.add(new Message(sender, text, timestamp));
            }
            messageAdapter.notifyDataSetChanged();
            recyclerMessages.scrollToPosition(messageList.size() - 1);
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
                .addOnSuccessListener(documentReference -> etMessage.setText(""))
                .addOnFailureListener(e -> Toast.makeText(Chat.this, "Erreur envoi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}

package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DisplayLostItem extends AppCompatActivity {

    private TextView tvDesc, tvLoc;
    private Button btnBack;
    private Button btnMsg;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.displaylostitem);

        tvDesc = findViewById(R.id.showDesc);
        tvLoc = findViewById(R.id.showLoc);
        btnBack = findViewById(R.id.btnBackDLI);
        btnMsg = findViewById(R.id.itsminebtn);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get data from intent
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String location = getIntent().getStringExtra("location");
        String reporterId = getIntent().getStringExtra("userID");
        String itemId = getIntent().getStringExtra("itemID");

        // Set the TextViews
        if(description != null) tvDesc.setText(description);
        if(location != null) tvLoc.setText(location);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // "Its Mine!" button logic
        btnMsg.setOnClickListener(v -> {
            assert mAuth.getCurrentUser() != null;
            String currentUserId = mAuth.getCurrentUser().getUid();

            // Prevent claiming own item
            if(currentUserId.equals(reporterId)) {
                Toast.makeText(this, "You cannot claim your own item!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate unique chat ID
            String chatId = itemId + "_" + reporterId + "_" + currentUserId;

            db.collection("chats").document(chatId)
                    .get()
                    .addOnSuccessListener(docSnapshot -> {
                        if(docSnapshot.exists()) {
                            // Chat already exists
                            Toast.makeText(this, "Chat already exists! Check your messages.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Create new chat
                            Map<String, Object> chatData = new HashMap<>();
                            chatData.put("itemId", itemId);
                            chatData.put("reporterId", reporterId);
                            chatData.put("userId", currentUserId);
                            chatData.put("timestamp", FieldValue.serverTimestamp());
                            chatData.put("title",title);

                            db.collection("chats").document(chatId)
                                    .set(chatData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Navigate to chat activity
                                        Intent intent = new Intent(DisplayLostItem.this, Chat.class);
                                        intent.putExtra("Title",title);
                                        intent.putExtra("chatId", chatId);
                                        intent.putExtra("itemId", itemId);
                                        intent.putExtra("reporterId", reporterId);
                                        startActivity(intent);
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Failed to create chat: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error checking chat: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });
    }
}


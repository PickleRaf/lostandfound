package com.example.myapplication.activities.items;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.activities.chat.ChatActivity;
import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ItemDetailActivity extends AppCompatActivity {

    // UI elements
    private TextView tvDesc, tvLoc, tvStatus;
    private Button btnBack, btnMsg, btnResolve, btnReport; // PHASE 2 & 3: New buttons
    private ProgressBar progressBar;

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Item data
    private String title, description, location, reporterId, itemId;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.items_activity_item_detail);

        // Link UI elements
        tvDesc = findViewById(R.id.showDesc);
        tvLoc = findViewById(R.id.showLoc);
        tvStatus = findViewById(R.id.tvItemStatus);
        btnBack = findViewById(R.id.btnBackDLI);
        btnMsg = findViewById(R.id.itsminebtn);
        btnResolve = findViewById(R.id.btnResolve);
        btnReport = findViewById(R.id.btnReportUser);
        progressBar = findViewById(R.id.progressBarDLI);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get item details passed from previous screen
        title = getIntent().getStringExtra("title");
        description = getIntent().getStringExtra("description");
        location = getIntent().getStringExtra("location");
        reporterId = getIntent().getStringExtra("userID");
        itemId = getIntent().getStringExtra("itemID");

        // Display item information
        if(description != null) tvDesc.setText(description);
        if(location != null) tvLoc.setText(location);

        // Load item status
        loadItemStatus();

        // BACK BUTTON
        btnBack.setOnClickListener(v -> finish());

        // "IT'S MINE!" BUTTON - Create chat with finder
        btnMsg.setOnClickListener(v -> {
            assert mAuth.getCurrentUser() != null;
            String currentUserId = mAuth.getCurrentUser().getUid();

            // Prevent user from claiming their own item
            if(currentUserId.equals(reporterId)) {
                Toast.makeText(this, "You cannot claim your own item!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading
            showLoading(true);

            // Generate unique chat ID (prevents duplicate chats)
            String chatId = itemId + "_" + reporterId + "_" + currentUserId;

            // Check if chat already exists
            db.collection("chats").document(chatId)
                    .get()
                    .addOnSuccessListener(docSnapshot -> {
                        showLoading(false);

                        if(docSnapshot.exists()) {
                            // Chat already exists - open it
                            Toast.makeText(this, "Opening existing chat...", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(ItemDetailActivity.this, ChatActivity.class);
                            intent.putExtra("chatId", chatId);
                            startActivity(intent);
                        } else {
                            // Create new chat document
                            Map<String, Object> chatData = new HashMap<>();
                            chatData.put("itemId", itemId);
                            chatData.put("reporterId", reporterId);
                            chatData.put("userId", currentUserId);
                            chatData.put("timestamp", FieldValue.serverTimestamp());
                            chatData.put("title", title);

                            // Save chat to Firestore
                            db.collection("chats").document(chatId)
                                    .set(chatData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Navigate to chat screen
                                        Intent intent = new Intent(ItemDetailActivity.this, ChatActivity.class);
                                        intent.putExtra("Title", title);
                                        intent.putExtra("chatId", chatId);
                                        intent.putExtra("itemId", itemId);
                                        intent.putExtra("reporterId", reporterId);
                                        startActivity(intent);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to create chat: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Error checking chat: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });

        // PHASE 2: RESOLVE BUTTON - Mark item as resolved (only for reporter)
        btnResolve.setOnClickListener(v -> {
            assert mAuth.getCurrentUser() != null;
            String currentUserId = mAuth.getCurrentUser().getUid();

            // Only reporter can mark as resolved
            if(!currentUserId.equals(reporterId)) {
                Toast.makeText(this, "Only the reporter can mark this as resolved",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Show confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Mark as Resolved?")
                    .setMessage("This will hide the item from search results. This cannot be undone.")
                    .setPositiveButton("Yes, Resolve", (dialog, which) -> {
                        markAsResolved();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // PHASE 3: REPORT USER BUTTON
        btnReport.setOnClickListener(v -> {
            assert mAuth.getCurrentUser() != null;
            String currentUserId = mAuth.getCurrentUser().getUid();

            // Cannot report yourself
            if(currentUserId.equals(reporterId)) {
                Toast.makeText(this, "You cannot report yourself", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show report dialog
            showReportDialog();
        });
    }

    // PHASE 2: Load item status from Firestore
    private void loadItemStatus() {
        db.collection("lost_items").document(itemId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String status = doc.getString("status");
                        String currentUserId = mAuth.getCurrentUser().getUid();

                        if ("resolved".equals(status)) {
                            tvStatus.setText("Status: RESOLVED ✓");
                            tvStatus.setVisibility(View.VISIBLE);
                            btnMsg.setEnabled(false);
                            btnResolve.setVisibility(View.GONE);
                        } else {
                            tvStatus.setVisibility(View.GONE);
                            // Show resolve button only to reporter
                            btnResolve.setVisibility(currentUserId.equals(reporterId) ?
                                    View.VISIBLE : View.GONE);
                        }
                    }
                });
    }

    // PHASE 2: Mark item as resolved
    private void markAsResolved() {
        showLoading(true);

        db.collection("lost_items").document(itemId)
                .update("status", "resolved")
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Item marked as resolved!", Toast.LENGTH_SHORT).show();
                    finish(); // Return to previous screen
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // PHASE 3: Show report user dialog
    private void showReportDialog() {
        final String[] reportReasons = {
                "Spam or misleading information",
                "Inappropriate behavior",
                "Scam or fraud attempt",
                "Harassment",
                "Other"
        };

        new AlertDialog.Builder(this)
                .setTitle("Report User")
                .setItems(reportReasons, (dialog, which) -> {
                    String reason = reportReasons[which];
                    submitReport(reason);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // PHASE 3: Submit report to Firestore
    private void submitReport(String reason) {
        showLoading(true);

        String currentUserId = mAuth.getCurrentUser().getUid();

        Map<String, Object> report = new HashMap<>();
        report.put("reportedUser", reporterId);
        report.put("reportedBy", currentUserId);
        report.put("itemId", itemId);
        report.put("reason", reason);
        report.put("timestamp", FieldValue.serverTimestamp());

        db.collection("reports")
                .add(report)
                .addOnSuccessListener(documentReference -> {
                    showLoading(false);
                    Toast.makeText(this, "Report submitted. Thank you for keeping the community safe.",
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to submit report: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Show/hide loading indicator
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnMsg.setEnabled(!show);
        btnResolve.setEnabled(!show);
        btnReport.setEnabled(!show);
    }
}
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ItemDetailActivity extends AppCompatActivity {

    // UI elements
    private TextView tvDesc, tvLoc, tvStatus, tvReporterName, tvReportedTime;
    private Button btnBack, btnMsg, btnResolve, btnReport;
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
        tvReporterName = findViewById(R.id.tvReporterName);
        tvReportedTime = findViewById(R.id.tvReportedTime);
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

        // Load item status and additional details
        loadItemDetails();

        // Load reporter name
        loadReporterName();

        // BACK BUTTON
        btnBack.setOnClickListener(v -> finish());

        // "IT'S MINE!" BUTTON - Create chat with finder
        btnMsg.setOnClickListener(v -> {
            assert mAuth.getCurrentUser() != null;
            String currentUserId = mAuth.getCurrentUser().getUid();

            if(currentUserId.equals(reporterId)) {
                Toast.makeText(this, "You cannot claim your own item!", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoading(true);

            String chatId = itemId + "_" + reporterId + "_" + currentUserId;

            db.collection("chats").document(chatId)
                    .get()
                    .addOnSuccessListener(docSnapshot -> {
                        showLoading(false);

                        if(docSnapshot.exists()) {
                            Toast.makeText(this, "Opening existing chat...", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(ItemDetailActivity.this, ChatActivity.class);
                            intent.putExtra("chatId", chatId);
                            startActivity(intent);
                        } else {
                            Map<String, Object> chatData = new HashMap<>();
                            chatData.put("itemId", itemId);
                            chatData.put("reporterId", reporterId);
                            chatData.put("userId", currentUserId);
                            chatData.put("timestamp", FieldValue.serverTimestamp());
                            chatData.put("title", title);

                            db.collection("chats").document(chatId)
                                    .set(chatData)
                                    .addOnSuccessListener(aVoid -> {
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

        // RESOLVE BUTTON
        btnResolve.setOnClickListener(v -> {
            assert mAuth.getCurrentUser() != null;
            String currentUserId = mAuth.getCurrentUser().getUid();

            if(!currentUserId.equals(reporterId)) {
                Toast.makeText(this, "Only the reporter can mark this as resolved",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Mark as Resolved?")
                    .setMessage("This will hide the item from search results. This cannot be undone.")
                    .setPositiveButton("Yes, Resolve", (dialog, which) -> markAsResolved())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // REPORT USER BUTTON
        btnReport.setOnClickListener(v -> {
            assert mAuth.getCurrentUser() != null;
            String currentUserId = mAuth.getCurrentUser().getUid();

            if(currentUserId.equals(reporterId)) {
                Toast.makeText(this, "You cannot report yourself", Toast.LENGTH_SHORT).show();
                return;
            }

            showReportDialog();
        });
    }

    // Load item details including timestamp
    private void loadItemDetails() {
        db.collection("lost_items").document(itemId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String status = doc.getString("status");
                        String currentUserId = mAuth.getCurrentUser().getUid();

                        // Get and display timestamp
                        Timestamp timestamp = doc.getTimestamp("dateReported");
                        if (timestamp != null) {
                            Date date = timestamp.toDate();
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                            String formattedDate = sdf.format(date);
                            tvReportedTime.setText("Reported: " + formattedDate);
                            tvReportedTime.setVisibility(View.VISIBLE);
                        }

                        if ("resolved".equals(status)) {
                            tvStatus.setText("Status: RESOLVED ✓");
                            tvStatus.setVisibility(View.VISIBLE);
                            btnMsg.setEnabled(false);
                            btnResolve.setVisibility(View.GONE);
                        } else {
                            tvStatus.setText("Status: ACTIVE ");
                            tvStatus.setVisibility(View.VISIBLE);
                            btnResolve.setVisibility(currentUserId.equals(reporterId) ?
                                    View.VISIBLE : View.GONE);
                        }
                    }
                });
    }

    // Load reporter's name from users collection
    private void loadReporterName() {
        if (reporterId == null) return;

        db.collection("users").document(reporterId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String firstName = doc.getString("firstName");
                        String lastName = doc.getString("lastName");

                        String displayName = "";
                        if (firstName != null && !firstName.isEmpty()) {
                            displayName = firstName;
                        }
                        if (lastName != null && !lastName.isEmpty()) {
                            displayName += (displayName.isEmpty() ? "" : " ") + lastName;
                        }

                        if (!displayName.isEmpty()) {
                            tvReporterName.setText("Posted by: " + displayName);
                        } else {
                            tvReporterName.setText("Posted by: Anonymous");
                        }
                        tvReporterName.setVisibility(View.VISIBLE);
                    } else {
                        tvReporterName.setText("Posted by: Unknown User");
                        tvReporterName.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    tvReporterName.setText("Posted by: Unknown User");
                    tvReporterName.setVisibility(View.VISIBLE);
                });
    }

    // Mark item as resolved
    private void markAsResolved() {
        showLoading(true);

        db.collection("lost_items").document(itemId)
                .update("status", "resolved")
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Item marked as resolved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Show report user dialog
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

    // Submit report to Firestore
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
package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Activity2 extends AppCompatActivity {

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // UI elements
    private TextView welcomeText;
    private TextView tvStatsReported, tvStatsChats, tvStatsResolved; // PHASE 3: Statistics
    private Button btnRPI, btnLFI, btnInbox, btnBack, btnProfile; // PHASE 2: Profile button
    private ImageButton btnSettings;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_2);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Link UI elements
        welcomeText = findViewById(R.id.usernamegetter);
        tvStatsReported = findViewById(R.id.tvStatsReported);
        tvStatsChats = findViewById(R.id.tvStatsChats);
        tvStatsResolved = findViewById(R.id.tvStatsResolved);
        btnBack = findViewById(R.id.btnBack);
        btnRPI = findViewById(R.id.btnReportLost);
        btnLFI = findViewById(R.id.btnLookForItem);
        btnSettings = findViewById(R.id.btnSettings);
        btnInbox = findViewById(R.id.btnInbox);
        btnProfile = findViewById(R.id.btnProfile);

        // Get username or email for welcome message
        String username = getIntent().getStringExtra("username");
        if (username != null && !username.isEmpty()) {
            welcomeText.setText("Welcome " + username + "!");
        } else if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            String name = email.substring(0, email.indexOf("@"));
            welcomeText.setText("Welcome " + name + "!");
        } else {
            welcomeText.setText("Welcome!");
        }

        // PHASE 3: Load statistics dashboard
        loadStatistics();

        // PHASE 3: Add animations to buttons
        Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        btnRPI.startAnimation(slideUp);
        btnLFI.startAnimation(slideUp);

        // LOGOUT BUTTON
        btnBack.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Activity2.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // REPORT ITEM BUTTON
        btnRPI.setOnClickListener(v -> {
            Intent intent = new Intent(Activity2.this, reportitem.class);
            startActivity(intent);
        });

        // LOOK FOR ITEMS BUTTON
        btnLFI.setOnClickListener(v -> {
            Intent intent = new Intent(Activity2.this, Lookforitems.class);
            startActivity(intent);
        });

        // SETTINGS BUTTON
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(Activity2.this, SettingsActivity.class);
            startActivity(intent);
        });

        // INBOX BUTTON
        btnInbox.setOnClickListener(v -> {
            Intent intent = new Intent(Activity2.this, InboxActivity.class);
            startActivity(intent);
        });

        // PHASE 2: PROFILE BUTTON
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Activity2.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload statistics when returning to dashboard
        loadStatistics();
    }

    // PHASE 3: Load user statistics
    private void loadStatistics() {
        String userId = mAuth.getCurrentUser().getUid();

        // Count items reported by user
        db.collection("lost_items")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalReported = querySnapshot.size();
                    tvStatsReported.setText("Items Reported: " + totalReported);
                });

        // Count resolved items
        db.collection("lost_items")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "resolved")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalResolved = querySnapshot.size();
                    tvStatsResolved.setText("Items Resolved: " + totalResolved);
                });

        // Count active chats
        db.collection("chats")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int userChats = querySnapshot.size();

                    // Also count chats where user is reporter
                    db.collection("chats")
                            .whereEqualTo("reporterId", userId)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                int reporterChats = snapshot.size();
                                int totalChats = userChats + reporterChats;
                                tvStatsChats.setText("Active Chats: " + totalChats);
                            });
                });
    }
}
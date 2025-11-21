package com.example.myapplication.activities.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.activities.chat.InboxActivity;
import com.example.myapplication.activities.items.SearchItemsActivity;
import com.example.myapplication.activities.profile.ProfileActivity;
import com.example.myapplication.R;
import com.example.myapplication.activities.items.ReportItemActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvStatsReported, tvStatsChats, tvStatsResolved;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        TextView welcomeText = findViewById(R.id.usernamegetter);
        tvStatsReported = findViewById(R.id.tvStatsReported);
        tvStatsChats = findViewById(R.id.tvStatsChats);
        tvStatsResolved = findViewById(R.id.tvStatsResolved);
        Button btnRPI = findViewById(R.id.btnReportLost);
        Button btnLFI = findViewById(R.id.btnLookForItem);
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        Button btnInbox = findViewById(R.id.btnInbox);
        Button btnProfile = findViewById(R.id.btnProfile);

        // Load user's first and last name from Firestore
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");
                            if (firstName != null && lastName != null) {
                                String fullName = firstName + " " + lastName;
                                welcomeText.setText("Welcome\n" + fullName + "!");
                            } else {
                                welcomeText.setText(R.string.welcome_default);
                            }
                        } else {
                            welcomeText.setText(R.string.welcome_default);
                        }
                    })
                    .addOnFailureListener(e -> welcomeText.setText(R.string.welcome_default));
        } else {
            welcomeText.setText(R.string.welcome_default);
        }

        loadStatistics();

        Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        btnRPI.startAnimation(slideUp);
        btnLFI.startAnimation(slideUp);

        btnRPI.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ReportItemActivity.class);
            startActivity(intent);
        });

        btnLFI.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, SearchItemsActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnInbox.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, InboxActivity.class);
            startActivity(intent);
        });

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStatistics();
    }

    private void loadStatistics() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        db.collection("lost_items")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalReported = querySnapshot.size();
                    tvStatsReported.setText(getString(R.string.stats_items_reported, totalReported));
                });

        db.collection("lost_items")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "resolved")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalResolved = querySnapshot.size();
                    tvStatsResolved.setText(getString(R.string.stats_items_resolved, totalResolved));
                });

        db.collection("chats")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int userChats = querySnapshot.size();
                    db.collection("chats")
                            .whereEqualTo("reporterId", userId)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                int reporterChats = snapshot.size();
                                int totalChats = userChats + reporterChats;
                                tvStatsChats.setText(getString(R.string.stats_active_chats, totalChats));
                            });
                });
    }
}
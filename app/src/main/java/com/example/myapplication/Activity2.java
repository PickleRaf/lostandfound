package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class Activity2 extends AppCompatActivity {


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_2);

        TextView welcomeText;
        welcomeText = findViewById(R.id.usernamegetter);

        String username = getIntent().getStringExtra("username");

        if (username != null && !username.isEmpty()) {
            welcomeText.setText("Welcome " + username + "!");
        } else {
            welcomeText.setText("Welcome!");
        }

        Button btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Activity2.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        Button btnRPI = findViewById(R.id.btnReportLost);

        btnRPI.setOnClickListener(v -> {
            Intent intent = new Intent(Activity2.this, reportitem.class);
            startActivity(intent);
        });

        Button btnLFI = findViewById(R.id.btnLookForItem);

        btnLFI.setOnClickListener(v -> {
            Intent intent = new Intent(Activity2.this, Lookforitems.class);
            startActivity(intent);
        });

        ImageButton btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(Activity2.this, SettingsActivity.class);
            startActivity(intent);

        });

        Button btnInbox = findViewById(R.id.btnInbox);
        btnInbox.setOnClickListener(v -> {
            Intent intent = new Intent(Activity2.this, InboxActivity.class);
            startActivity(intent);
        });


    }
}


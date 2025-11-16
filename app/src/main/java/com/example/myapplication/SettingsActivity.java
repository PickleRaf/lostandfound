package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button btnBack = findViewById(R.id.btnBacksettings);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, Activity2.class);
            startActivity(intent);
        });
    }
}



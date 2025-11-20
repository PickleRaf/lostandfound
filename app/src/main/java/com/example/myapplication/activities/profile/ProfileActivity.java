package com.example.myapplication.activities.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    // UI elements
    private TextView tvFirstName, tvLastName, tvStudentId, tvDateOfBirth, tvMajor, tvEmail;
    private EditText etPhone, etAddress;
    private Button btnSave, btnBack;
    private ProgressBar progressBar;

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Current user ID
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // Link UI elements - Read-only fields
        tvFirstName = findViewById(R.id.tvFirstName);
        tvLastName = findViewById(R.id.tvLastName);
        tvStudentId = findViewById(R.id.tvStudentId);
        tvDateOfBirth = findViewById(R.id.tvDateOfBirth);
        tvMajor = findViewById(R.id.tvMajor);
        tvEmail = findViewById(R.id.tvEmail);

        // Editable fields
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);

        // Buttons
        btnSave = findViewById(R.id.btnSaveProfile);
        btnBack = findViewById(R.id.btnBackProfile);
        progressBar = findViewById(R.id.progressBarProfile);

        // Load user profile
        loadUserProfile();

        // SAVE BUTTON
        btnSave.setOnClickListener(v -> saveProfile());

        // BACK BUTTON
        btnBack.setOnClickListener(v -> finish());
    }

    // Load user profile from Firestore
    private void loadUserProfile() {
        showLoading(true);

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    showLoading(false);
                    if (doc.exists()) {
                        // Load ID card data (read-only)
                        String firstName = doc.getString("firstName");
                        String lastName = doc.getString("lastName");
                        String studentId = doc.getString("studentId");
                        String dateOfBirth = doc.getString("dateOfBirth");
                        String major = doc.getString("major");
                        String email = doc.getString("email");

                        // Load editable data
                        String phone = doc.getString("phone");
                        String address = doc.getString("address");

                        // Display read-only data
                        tvFirstName.setText(firstName != null && !firstName.isEmpty() ? firstName : "N/A");
                        tvLastName.setText(lastName != null && !lastName.isEmpty() ? lastName : "N/A");
                        tvStudentId.setText(studentId != null && !studentId.isEmpty() ? studentId : "N/A");
                        tvDateOfBirth.setText(dateOfBirth != null && !dateOfBirth.isEmpty() ? dateOfBirth : "N/A");
                        tvMajor.setText(major != null && !major.isEmpty() ? major : "N/A");
                        tvEmail.setText(email != null && !email.isEmpty() ? email : mAuth.getCurrentUser().getEmail());

                        // Display editable data
                        if (phone != null && !phone.isEmpty()) etPhone.setText(phone);
                        if (address != null && !address.isEmpty()) etAddress.setText(address);
                    } else {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Save profile (only phone and address)
    private void saveProfile() {
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        // Validate phone number if not empty
        if (!phone.isEmpty() && !phone.matches("\\d{10,15}")) {
            etPhone.setError("Invalid phone number");
            Toast.makeText(this, "Please enter a valid phone number (10-15 digits)", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Update only phone and address
        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", phone);
        updates.put("address", address);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error updating profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Show/hide loading indicator
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        btnBack.setEnabled(!show);
    }
}
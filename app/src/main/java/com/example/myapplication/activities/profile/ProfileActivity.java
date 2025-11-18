package com.example.myapplication.activities.profile;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    // UI elements
    private EditText etName, etPhone, etAddress;
    private ImageView ivProfilePic;
    private Button btnSave, btnBack, btnSelectPhoto;
    private ProgressBar progressBar;

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    // Profile photo URI
    private Uri profilePhotoUri;
    private ActivityResultLauncher<String> imagePickerLauncher;

    // Current user ID
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // Link UI elements
        etName = findViewById(R.id.etProfileName);
        etPhone = findViewById(R.id.etProfilePhone);
        etAddress = findViewById(R.id.etProfileAddress);
        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnBack = findViewById(R.id.btnBackProfile);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        progressBar = findViewById(R.id.progressBarProfile);

        // Initialize image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        profilePhotoUri = uri;
                        ivProfilePic.setImageURI(uri);
                        Toast.makeText(this, "Photo selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Load existing profile
        loadUserProfile();

        // SELECT PHOTO BUTTON
        btnSelectPhoto.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        // SAVE BUTTON
        btnSave.setOnClickListener(v -> {
            saveProfile();
        });

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
                        // Load existing data
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        String address = doc.getString("address");
                        String photoUrl = doc.getString("profilePhotoUrl");

                        if (name != null) etName.setText(name);
                        if (phone != null) etPhone.setText(phone);
                        if (address != null) etAddress.setText(address);

                        // Load profile photo (you'll need Glide or Picasso library)
                        // For now, just show placeholder
                    } else {
                        // New profile - show email as default name
                        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
                            String email = mAuth.getCurrentUser().getEmail();
                            etName.setText(email.substring(0, email.indexOf("@")));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Save profile to Firestore
    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        // Validate inputs
        if (name.isEmpty()) {
            etName.setError("Name is required");
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.isEmpty()) {
            etPhone.setError("Phone is required");
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate phone number format (basic validation)
        if (!phone.matches("\\d{10,15}")) {
            etPhone.setError("Invalid phone number");
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Upload photo first if selected
        if (profilePhotoUri != null) {
            uploadProfilePhoto(() -> saveProfileData(name, phone, address));
        } else {
            saveProfileData(name, phone, address);
        }
    }

    // Upload profile photo to Firebase Storage
    private void uploadProfilePhoto(Runnable onSuccess) {
        StorageReference photoRef = storage.getReference()
                .child("profile_photos/" + userId + ".jpg");

        photoRef.putFile(profilePhotoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        profilePhotoUri = uri;
                        onSuccess.run();
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show();
                });
    }

    // Save profile data to Firestore
    private void saveProfileData(String name, String phone, String address) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name);
        profile.put("phone", phone);
        profile.put("address", address);
        profile.put("email", mAuth.getCurrentUser().getEmail());

        if (profilePhotoUri != null) {
            profile.put("profilePhotoUrl", profilePhotoUri.toString());
        }

        db.collection("users").document(userId)
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error saving profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Show/hide loading indicator
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        btnSelectPhoto.setEnabled(!show);
    }
}
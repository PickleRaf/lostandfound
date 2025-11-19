package com.example.myapplication.activities.auth;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.myapplication.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    // UI elements
    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button btnRegister, btnRegisterBck, btnSelectID;
    private ImageView ivIDPreview;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // ID card data
    private Uri idCardUri; // final compressed file URI to upload
    private String currentPhotoPath; // temp camera file path
    private boolean isIDVerified = false;

    // ML Kit
    private TextRecognizer textRecognizer;
    private FaceDetector faceDetector; // optional, used to check presence of face/photo

    // Activity result launchers
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    // Background executor for compression / heavy tasks
    private final Executor bgExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_register);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // ML Kit init
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Face detector (fast, accurate enough for presence check)
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();
        faceDetector = FaceDetection.getClient(highAccuracyOpts);

        // Link UI
        emailInput = findViewById(R.id.registermail);
        passwordInput = findViewById(R.id.RegisterPword);
        confirmPasswordInput = findViewById(R.id.RegisterConfirmPword);
        btnRegister = findViewById(R.id.btnRegister);
        btnRegisterBck = findViewById(R.id.btnRegisterBck);
        btnSelectID = findViewById(R.id.btnSelectID);
        ivIDPreview = findViewById(R.id.ivIDPreview);
        progressBar = findViewById(R.id.progressBarRegister);

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result && currentPhotoPath != null) {
                        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                        if (bitmap != null) {
                            ivIDPreview.setImageBitmap(bitmap);
                            ivIDPreview.setVisibility(View.VISIBLE);
                            verifyIDCardAsync(bitmap);
                        } else {
                            Toast.makeText(this, "Captured image not readable", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Photo capture canceled or failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                            if (bitmap != null) {
                                ivIDPreview.setImageBitmap(bitmap);
                                ivIDPreview.setVisibility(View.VISIBLE);
                                // compress gallery image before marking as idCardUri
                                compressAndSaveThenMarkVerified(bitmap);
                            } else {
                                Toast.makeText(this, "Unable to load selected image", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Error reading selected image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Button listeners
        btnRegisterBck.setOnClickListener(v -> finish());

        btnSelectID.setOnClickListener(v -> showImageSourceDialog());

        btnRegister.setOnClickListener(v -> createAccount());
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Select ID Card Source")
                .setItems(new String[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
                        } else {
                            openCamera();
                        }
                    } else {
                        galleryLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(this,
                        "com.example.myapplication.fileprovider",
                        photoFile);
                currentPhotoPath = photoFile.getAbsolutePath();
                cameraLauncher.launch(photoUri);
            } else {
                Toast.makeText(this, "Could not create image file", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "ID_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void verifyIDCardAsync(Bitmap bitmap) {
        setUiBusy(true);
        bgExecutor.execute(() -> {
            try {
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                Task<Text> ocrTask = textRecognizer.process(image);
                Text textResult = com.google.android.gms.tasks.Tasks.await(ocrTask);
                String extracted = textResult.getText().toLowerCase(Locale.ROOT);

                boolean ocrValid = validateIDCard(extracted);
                boolean faceFound = false;
                try {
                    Task<java.util.List<Face>> faceTask = faceDetector.process(image);
                    java.util.List<Face> faces = com.google.android.gms.tasks.Tasks.await(faceTask);
                    faceFound = (faces != null && !faces.isEmpty());
                } catch (Exception ignored) {}

                final boolean finalOcrValid = ocrValid;
                final boolean finalFaceFound = faceFound;
                final String finalExtracted = extracted;

                runOnUiThread(() -> {
                    setUiBusy(false);
                    if (finalOcrValid) {
                        if (!finalFaceFound) {
                            new AlertDialog.Builder(RegisterActivity.this)
                                    .setTitle("No photo detected")
                                    .setMessage("We couldn't detect a face/photo on this ID. Try again or proceed anyway?")
                                    .setPositiveButton("Try Again", (d, w) -> {
                                        ivIDPreview.setImageDrawable(null);
                                        ivIDPreview.setVisibility(View.GONE);
                                        idCardUri = null;
                                        showImageSourceDialog();
                                    })
                                    .setNegativeButton("Proceed Anyway", (d, w) -> compressAndSaveThenMarkVerified(bitmap))
                                    .setNeutralButton("See Detected Text", (d, w) -> showDetectedTextDialog(finalExtracted))
                                    .show();
                        } else {
                            compressAndSaveThenMarkVerified(bitmap);
                        }
                    } else {
                        showVerificationFailedDialog(finalExtracted);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setUiBusy(false);
                    Toast.makeText(RegisterActivity.this, "Error verifying ID: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void compressAndSaveThenMarkVerified(Bitmap bitmap) {
        setUiBusy(true);
        bgExecutor.execute(() -> {
            try {
                Uri compressed = saveCompressedImage(bitmap);
                runOnUiThread(() -> {
                    setUiBusy(false);
                    if (compressed != null) {
                        idCardUri = compressed;
                        isIDVerified = true;
                        btnSelectID.setText("✓ ID Verified");
                        btnSelectID.setEnabled(false);
                        Toast.makeText(RegisterActivity.this, "ID verified and saved", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Failed to process image", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setUiBusy(false);
                    Toast.makeText(RegisterActivity.this, "Error saving image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Uri saveCompressedImage(Bitmap bitmap) {
        try {
            final int MAX_DIM = 1200;
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int max = Math.max(w, h);
            Bitmap scaled = bitmap;
            if (max > MAX_DIM) {
                float scale = (float) MAX_DIM / max;
                scaled = Bitmap.createScaledBitmap(bitmap, Math.round(w * scale), Math.round(h * scale), true);
            }
            File outFile = new File(getCacheDir(), "verified_id_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(outFile);
            scaled.compress(Bitmap.CompressFormat.JPEG, 75, fos);
            fos.flush();
            fos.close();
            return Uri.fromFile(outFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showDetectedTextDialog(String extracted) {
        new AlertDialog.Builder(this)
                .setTitle("Detected Text")
                .setMessage(extracted == null || extracted.isEmpty() ? "(no text found)" : extracted)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showVerificationFailedDialog(String extractedText) {
        new AlertDialog.Builder(this)
                .setTitle("ID Verification Failed")
                .setMessage("The image doesn't appear to be a valid university ID card. Please ensure the image is clear and complete.")
                .setPositiveButton("Try Again", (dialog, which) -> {
                    ivIDPreview.setImageDrawable(null);
                    ivIDPreview.setVisibility(View.GONE);
                    idCardUri = null;
                    showImageSourceDialog();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("See Detected Text", (dialog, which) -> showDetectedTextDialog(extractedText))
                .show();
    }

    private boolean validateIDCard(String text) {
        if (text == null || text.length() < 20) return false;
        text = text.toLowerCase(Locale.ROOT);

        boolean hasUniversity = text.contains("university") || text.contains("college") || text.contains("institute") || text.contains("universit") || text.contains("جامعة") || text.contains("الجامعة");
        boolean hasStudent = text.contains("student") || text.contains("étudiant") || text.contains("id") || text.contains("card") || text.contains("matricule") || text.contains("student id");
        boolean hasIdNumber = Pattern.compile("\\b\\d{5,12}\\b").matcher(text).find();
        boolean hasYear = Pattern.compile("\\b20\\d{2}\\b").matcher(text).find();
        boolean hasName = Pattern.compile("\\b[a-zA-Z]{2,}\\s+[a-zA-Z]{2,}\\b").matcher(text).find();

        int score = 0;
        if (hasIdNumber) score++;
        if (hasYear) score++;
        if (hasName) score++;

        return hasUniversity && hasStudent && score >= 1;
    }

    private void createAccount() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (!validateInputs(email, password, confirmPassword)) return;

        if (!isIDVerified || idCardUri == null) {
            Toast.makeText(this, "Please upload and verify your university ID card first", Toast.LENGTH_LONG).show();
            return;
        }

        setUiBusy(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            uploadIDCard(user.getUid(), () -> {
                                user.sendEmailVerification()
                                        .addOnCompleteListener(verifyTask -> {
                                            setUiBusy(false);
                                            if (verifyTask.isSuccessful()) {
                                                showSuccessDialog();
                                            } else {
                                                Toast.makeText(this, "Account created but failed to send verification email", Toast.LENGTH_LONG).show();
                                            }
                                        });
                            });
                        } else {
                            setUiBusy(false);
                            Toast.makeText(this, "Account created but user object is null", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        setUiBusy(false);
                        String msg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(this, "Registration failed: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadIDCard(String userId, Runnable onSuccess) {
        if (idCardUri == null) {
            onSuccess.run();
            return;
        }

        setUiBusy(true);
        StorageReference idCardRef = storage.getReference().child("id_cards/" + userId + ".jpg");
        idCardRef.putFile(idCardUri)
                .addOnSuccessListener(taskSnapshot -> idCardRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("idCardUrl", uri.toString());
                            userData.put("idVerified", true);
                            userData.put("verificationDate", System.currentTimeMillis());

                            db.collection("users").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        setUiBusy(false);
                                        onSuccess.run();
                                    })
                                    .addOnFailureListener(e -> {
                                        setUiBusy(false);
                                        Toast.makeText(RegisterActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            setUiBusy(false);
                            Toast.makeText(RegisterActivity.this, "Failed to get download URL", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    setUiBusy(false);
                    Toast.makeText(RegisterActivity.this, "ID card upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInputs(String email, String password, String confirmPassword) {
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Invalid email format");
            return false;
        }
        if (password.length() < 8) {
            passwordInput.setError("Password must be at least 8 characters");
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            passwordInput.setError("Password must contain uppercase letter");
            return false;
        }
        if (!password.matches(".*[a-z].*")) {
            passwordInput.setError("Password must contain lowercase letter");
            return false;
        }
        if (!password.matches(".*\\d.*")) {
            passwordInput.setError("Password must contain a number");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            return false;
        }
        return true;
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Registration Successful! ✓")
                .setMessage("Your account has been created and your ID card has been verified.\n\nPlease check your email for the verification link before logging in.")
                .setPositiveButton("Go to Login", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void setUiBusy(boolean busy) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!busy);
        btnSelectID.setEnabled(!busy && !isIDVerified);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied. You can enable it in settings.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textRecognizer != null) textRecognizer.close();
        if (faceDetector != null) faceDetector.close();
    }
}

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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
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

    // ID card extracted data
    private String currentPhotoPath;
    private boolean isIDVerified = false;
    private Map<String, String> extractedIDData;

    // ML Kit
    private TextRecognizer textRecognizer;
    private FaceDetector faceDetector;

    // Activity result launchers
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    // Background executor
    private final Executor bgExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_register);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ML Kit init
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

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
                            verifyAndExtractIDDataAsync(bitmap);
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
                                verifyAndExtractIDDataAsync(bitmap);
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

    private void verifyAndExtractIDDataAsync(Bitmap bitmap) {
        setUiBusy(true);
        bgExecutor.execute(() -> {
            try {
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                Task<Text> ocrTask = textRecognizer.process(image);
                Text textResult = com.google.android.gms.tasks.Tasks.await(ocrTask);
                String extractedText = textResult.getText();

                // Extract ID card fields
                Map<String, String> idData = extractIDCardFields(extractedText);

                boolean isValid = validateExtractedData(idData);

                // Check for face
                boolean faceFound = false;
                try {
                    Task<java.util.List<Face>> faceTask = faceDetector.process(image);
                    java.util.List<Face> faces = com.google.android.gms.tasks.Tasks.await(faceTask);
                    faceFound = (faces != null && !faces.isEmpty());
                } catch (Exception ignored) {}

                final boolean finalValid = isValid;
                final boolean finalFaceFound = faceFound;
                final String finalExtracted = extractedText;

                runOnUiThread(() -> {
                    setUiBusy(false);
                    if (finalValid) {
                        if (!finalFaceFound) {
                            new AlertDialog.Builder(RegisterActivity.this)
                                    .setTitle("No photo detected")
                                    .setMessage("We couldn't detect a face/photo on this ID. Try again or proceed anyway?")
                                    .setPositiveButton("Try Again", (d, w) -> resetIDSelection())
                                    .setNegativeButton("Proceed Anyway", (d, w) -> markIDVerified(idData))
                                    .setNeutralButton("See Extracted Data", (d, w) -> showExtractedDataDialog(idData, finalExtracted))
                                    .show();
                        } else {
                            markIDVerified(idData);
                        }
                    } else {
                        showVerificationFailedDialog(idData, finalExtracted);
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

    private Map<String, String> extractIDCardFields(String text) {
        Map<String, String> data = new HashMap<>();

        // Initialize all fields
        data.put("firstName", "");
        data.put("lastName", "");
        data.put("dateOfBirth", "");
        data.put("major", "");
        data.put("studentId", "");

        if (text == null || text.isEmpty()) {
            return data;
        }

        // Process line by line for better accuracy
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            String nextLine = (i + 1 < lines.length) ? lines[i + 1].trim() : "";

            // Check if current line is a label
            if (line.toLowerCase().contains("last name")) {
                // Value might be on same line after colon or on next line
                Pattern sameLinePattern = Pattern.compile("last\\s+name\\s*:?\\s*(.+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = sameLinePattern.matcher(line);
                if (matcher.find() && !matcher.group(1).trim().isEmpty()) {
                    data.put("lastName", matcher.group(1).trim());
                } else if (!nextLine.isEmpty() && !nextLine.contains(":")) {
                    data.put("lastName", nextLine);
                }
            }
            else if (line.toLowerCase().contains("first name")) {
                Pattern sameLinePattern = Pattern.compile("first\\s+name\\s*:?\\s*(.+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = sameLinePattern.matcher(line);
                if (matcher.find() && !matcher.group(1).trim().isEmpty()) {
                    data.put("firstName", matcher.group(1).trim());
                } else if (!nextLine.isEmpty() && !nextLine.contains(":")) {
                    data.put("firstName", nextLine);
                }
            }
            else if (line.toLowerCase().contains("date of birth")) {
                Pattern sameLinePattern = Pattern.compile("date\\s+of\\s+birth\\s*:?\\s*(.+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = sameLinePattern.matcher(line);
                if (matcher.find() && !matcher.group(1).trim().isEmpty()) {
                    data.put("dateOfBirth", matcher.group(1).trim());
                } else if (!nextLine.isEmpty() && !nextLine.contains(":")) {
                    data.put("dateOfBirth", nextLine);
                }
            }
            else if (line.toLowerCase().matches(".*\\bmajor\\b.*:.*")) {
                Pattern sameLinePattern = Pattern.compile("major\\s*:?\\s*(.+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = sameLinePattern.matcher(line);
                if (matcher.find() && !matcher.group(1).trim().isEmpty()) {
                    data.put("major", matcher.group(1).trim());
                } else if (!nextLine.isEmpty() && !nextLine.contains(":")) {
                    data.put("major", nextLine);
                }
            }
            else if (line.toLowerCase().contains("student id")) {
                Pattern sameLinePattern = Pattern.compile("student\\s+id\\s*:?\\s*(.+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = sameLinePattern.matcher(line);
                if (matcher.find() && !matcher.group(1).trim().isEmpty()) {
                    data.put("studentId", matcher.group(1).trim());
                } else if (!nextLine.isEmpty() && !nextLine.contains(":")) {
                    data.put("studentId", nextLine);
                }
            }
        }

        // Fallback: look for standalone ID pattern if not found
        if (data.get("studentId").isEmpty()) {
            Pattern idPattern = Pattern.compile("\\b(\\d{3}-\\d{3}-\\d{4})\\b");
            Matcher idMatcher = idPattern.matcher(text);
            if (idMatcher.find()) {
                data.put("studentId", idMatcher.group(1));
            }
        }

        // Fallback: look for date pattern if not found
        if (data.get("dateOfBirth").isEmpty()) {
            Pattern datePattern = Pattern.compile("\\b([A-Z][a-z]+\\s+\\d{1,2},?\\s+\\d{4})\\b");
            Matcher dateMatcher = datePattern.matcher(text);
            if (dateMatcher.find()) {
                String potentialDate = dateMatcher.group(1);
                // Make sure it's not part of a label
                if (!text.substring(Math.max(0, dateMatcher.start() - 20), dateMatcher.start()).toLowerCase().contains("university")) {
                    data.put("dateOfBirth", potentialDate);
                }
            }
        }

        return data;
    }

    private boolean validateExtractedData(Map<String, String> data) {
        // At minimum, we need student ID and at least one name field
        boolean hasStudentId = !data.get("studentId").isEmpty();
        boolean hasName = !data.get("firstName").isEmpty() || !data.get("lastName").isEmpty();

        return hasStudentId && hasName;
    }

    private void markIDVerified(Map<String, String> data) {
        extractedIDData = data;
        isIDVerified = true;
        btnSelectID.setText("✓ ID Verified");
        btnSelectID.setEnabled(false);

        StringBuilder summary = new StringBuilder("ID verified successfully!\n\n");
        if (!data.get("firstName").isEmpty()) summary.append("First Name: ").append(data.get("firstName")).append("\n");
        if (!data.get("lastName").isEmpty()) summary.append("Last Name: ").append(data.get("lastName")).append("\n");
        if (!data.get("studentId").isEmpty()) summary.append("Student ID: ").append(data.get("studentId")).append("\n");
        if (!data.get("dateOfBirth").isEmpty()) summary.append("Date of Birth: ").append(data.get("dateOfBirth")).append("\n");
        if (!data.get("major").isEmpty()) summary.append("Major: ").append(data.get("major")).append("\n");

        Toast.makeText(this, summary.toString(), Toast.LENGTH_LONG).show();
    }

    private void resetIDSelection() {
        ivIDPreview.setImageDrawable(null);
        ivIDPreview.setVisibility(View.GONE);
        extractedIDData = null;
        isIDVerified = false;
        showImageSourceDialog();
    }

    private void showExtractedDataDialog(Map<String, String> data, String rawText) {
        StringBuilder message = new StringBuilder();
        message.append("Extracted Fields:\n\n");
        message.append("First Name: ").append(data.get("firstName").isEmpty() ? "(not found)" : data.get("firstName")).append("\n");
        message.append("Last Name: ").append(data.get("lastName").isEmpty() ? "(not found)" : data.get("lastName")).append("\n");
        message.append("Student ID: ").append(data.get("studentId").isEmpty() ? "(not found)" : data.get("studentId")).append("\n");
        message.append("Date of Birth: ").append(data.get("dateOfBirth").isEmpty() ? "(not found)" : data.get("dateOfBirth")).append("\n");
        message.append("Major: ").append(data.get("major").isEmpty() ? "(not found)" : data.get("major")).append("\n\n");
        message.append("Raw Text:\n").append(rawText.isEmpty() ? "(no text detected)" : rawText);

        new AlertDialog.Builder(this)
                .setTitle("Extracted Data")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showVerificationFailedDialog(Map<String, String> data, String rawText) {
        new AlertDialog.Builder(this)
                .setTitle("ID Verification Failed")
                .setMessage("Could not extract required fields from the ID card. Please ensure the image is clear and contains:\n\n• Student ID\n• First/Last Name\n\nMissing or incomplete data detected.")
                .setPositiveButton("Try Again", (dialog, which) -> resetIDSelection())
                .setNegativeButton("Cancel", null)
                .setNeutralButton("See Extracted Data", (dialog, which) -> showExtractedDataDialog(data, rawText))
                .show();
    }

    private void createAccount() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (!validateInputs(email, password, confirmPassword)) return;

        if (!isIDVerified || extractedIDData == null) {
            Toast.makeText(this, "Please upload and verify your university ID card first", Toast.LENGTH_LONG).show();
            return;
        }

        setUiBusy(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserDataToFirestore(user.getUid(), () -> {
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

    private void saveUserDataToFirestore(String userId, Runnable onSuccess) {
        Map<String, Object> userData = new HashMap<>();

        // Add extracted ID card data
        userData.put("firstName", extractedIDData.get("firstName"));
        userData.put("lastName", extractedIDData.get("lastName"));
        userData.put("studentId", extractedIDData.get("studentId"));
        userData.put("dateOfBirth", extractedIDData.get("dateOfBirth"));
        userData.put("major", extractedIDData.get("major"));

        // Add verification metadata
        userData.put("idVerified", true);
        userData.put("verificationDate", System.currentTimeMillis());
        userData.put("email", emailInput.getText().toString().trim());

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    setUiBusy(false);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    setUiBusy(false);
                    Toast.makeText(RegisterActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                .setMessage("Your account has been created and your ID card data has been verified.\n\nPlease check your email for the verification link before logging in.")
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
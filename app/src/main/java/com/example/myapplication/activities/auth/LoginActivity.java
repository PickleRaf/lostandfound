package com.example.myapplication.activities.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.activities.main.DashboardActivity;
import com.example.myapplication.R;
import com.example.myapplication.activities.profile.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    // Firebase authentication instance
    private FirebaseAuth mAuth;

    // UI elements
    private EditText usernameInput, passwordInput;
    private Button btnStart;
    private Button forgotPasswordButton;
    private ProgressBar progressBar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_login);

        // Initialize Firebase Auth instance
        mAuth = FirebaseAuth.getInstance();

        // Link UI elements from XML to Java variables
        usernameInput = findViewById(R.id.etUsername);
        passwordInput = findViewById(R.id.etPassword);
        btnStart = findViewById(R.id.btnStart);
        progressBar = findViewById(R.id.progressBar);
        forgotPasswordButton = findViewById(R.id.forgotpword);

        // EMAIL/PASSWORD LOGIN BUTTON
        btnStart.setOnClickListener(v -> {
            // Get text from input fields and remove whitespace
            String email = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            // Input validation
            if (!validateInputs(email, password)) {
                return; // Stop if validation fails
            }

            // Show loading indicator
            showLoading(true);

            // Attempt to sign in with email and password
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        showLoading(false); // Hide loading

                        if (task.isSuccessful()) {
                            // Check email verification
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                // Email verified - proceed to dashboard
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                finish();
                            } else {
                                // Email not verified - show dialog
                                showEmailVerificationDialog();
                            }
                        } else {
                            // Login failed - show appropriate error message
                            Exception e = task.getException();
                            if (e instanceof FirebaseAuthInvalidCredentialsException ||
                                    e instanceof FirebaseAuthInvalidUserException) {
                                Toast.makeText(this, R.string.login_error_incorrect_password, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, getString(R.string.login_error_generic) + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        forgotPasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ResetActivity.class);
            startActivity(intent);
        });

        // REGISTER BUTTON - Navigate to registration screen
        Button btnToRegister = findViewById(R.id.btnToRegister);
        btnToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Check if email is verified
            if (currentUser.isEmailVerified()) {
                // User is logged in and verified - skip login screen
                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                finish();
            } else {
                // Show verification dialog
                showEmailVerificationDialog();
            }
        }
    }

    // Input validation method
    private boolean validateInputs(String email, String password) {
        // Check if fields are empty
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.login_error_empty_credentials, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            usernameInput.setError(getString(R.string.login_error_invalid_email_format));
            Toast.makeText(this, R.string.login_error_invalid_email_address, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate password length
        if (password.length() < 6) {
            passwordInput.setError(getString(R.string.login_error_password_too_short_error));
            Toast.makeText(this, R.string.login_error_password_too_short_toast, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // Email verification dialog
    private void showEmailVerificationDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.email_not_verified_title)
                .setMessage(R.string.email_not_verified_message)
                .setPositiveButton(R.string.resend_email_button, (dialog, which) -> {
                    showLoading(true);
                    user.sendEmailVerification()
                            .addOnCompleteListener(task -> {
                                showLoading(false);
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, R.string.verification_email_sent_toast, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, R.string.verification_email_failed_toast, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton(R.string.go_back, (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                })
                .setCancelable(false)
                .show();
    }

    // Show/hide loading indicator
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnStart.setEnabled(!show);
    }
}

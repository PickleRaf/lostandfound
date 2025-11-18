package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {

    // Firebase authentication instance
    private FirebaseAuth mAuth;

    // UI elements
    private EditText usernameInput, passwordInput;
    private Button btnStart;
    private ImageButton googleBtn;
    private ProgressBar progressBar; // PHASE 1: Loading indicator

    // Google sign-in client
    private GoogleSignInClient mGoogleSignInClient;

    // Request code for Google sign-in activity result
    private final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth instance
        mAuth = FirebaseAuth.getInstance();

        // Link UI elements from XML to Java variables
        usernameInput = findViewById(R.id.etUsername);
        passwordInput = findViewById(R.id.etPassword);
        btnStart = findViewById(R.id.btnStart);
        googleBtn = findViewById(R.id.googleBtn);
        progressBar = findViewById(R.id.progressBar); // PHASE 1: Loading indicator

        // Configure Google Sign-In options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // EMAIL/PASSWORD LOGIN BUTTON
        btnStart.setOnClickListener(v -> {
            // Get text from input fields and remove whitespace
            String email = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            // PHASE 1: Input validation
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
                            // PHASE 1: Check email verification
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                // Email verified - proceed to dashboard
                                startActivity(new Intent(MainActivity.this, Activity2.class));
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
                                Toast.makeText(this, "Email or password incorrect", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        // GOOGLE SIGN-IN BUTTON
        googleBtn.setOnClickListener(v -> {
            showLoading(true);
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        // REGISTER BUTTON - Navigate to registration screen
        Button btnToRegister = findViewById(R.id.btnToRegister);
        btnToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // PHASE 1: Check if email is verified
            if (currentUser.isEmailVerified()) {
                // User is logged in and verified - skip login screen
                startActivity(new Intent(MainActivity.this, Activity2.class));
                finish();
            } else {
                // Show verification dialog
                showEmailVerificationDialog();
            }
        }
    }

    // Handle Google Sign-In result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

                    mAuth.signInWithCredential(credential)
                            .addOnCompleteListener(this, task1 -> {
                                showLoading(false);
                                if (task1.isSuccessful()) {
                                    startActivity(new Intent(MainActivity.this, Activity2.class));
                                    finish();
                                } else {
                                    Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            } catch (ApiException e) {
                showLoading(false);
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // PHASE 1: Input validation method
    private boolean validateInputs(String email, String password) {
        // Check if fields are empty
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            usernameInput.setError("Invalid email format");
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate password length
        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // PHASE 1: Email verification dialog
    private void showEmailVerificationDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Email Not Verified")
                .setMessage("Please verify your email before logging in. Check your inbox for the verification link.")
                .setPositiveButton("Resend Email", (dialog, which) -> {
                    showLoading(true);
                    user.sendEmailVerification()
                            .addOnCompleteListener(task -> {
                                showLoading(false);
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Verification email sent!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Failed to send email", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                })
                .setCancelable(false)
                .show();
    }

    // PHASE 1: Show/hide loading indicator
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnStart.setEnabled(!show);
        googleBtn.setEnabled(!show);
    }
}
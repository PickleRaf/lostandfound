package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText usernameInput, passwordInput;
    private Button btnStart;
    private ImageButton googleBtn;

    private GoogleSignInClient mGoogleSignInClient;
    private final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase Auth instance
        mAuth = FirebaseAuth.getInstance();

        // Link UI elements
        usernameInput = findViewById(R.id.etUsername);
        passwordInput = findViewById(R.id.etPassword);
        btnStart = findViewById(R.id.btnStart);
        googleBtn = findViewById(R.id.googleBtn);

        // Config Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // depuis google-services.json
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Login email/password
        btnStart.setOnClickListener(v -> {
            String email = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(MainActivity.this, Activity2.class));
                            finish();
                        } else {
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

        // Login Google
        googleBtn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });


        Button btnToRegister = findViewById(R.id.btnToRegister);

        btnToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Vérifie si un utilisateur est déjà connecté
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(MainActivity.this, Activity2.class));
            finish();
        }
    }

    // Résultat Google Sign-In
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
                                if (task1.isSuccessful()) {
                                    startActivity(new Intent(MainActivity.this, Activity2.class));
                                    finish();
                                } else {
                                    Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}


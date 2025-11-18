package com.example.myapplication.activities.auth;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button btnRegister, btnRegisterBck;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_register); // ton XML de register

        // Firebase Auth instance
        mAuth = FirebaseAuth.getInstance();

        // Link UI elements
        emailInput = findViewById(R.id.registermail);
        passwordInput = findViewById(R.id.RegisterPword);
        confirmPasswordInput = findViewById(R.id.RegisterConfirmPword);
        btnRegister = findViewById(R.id.btnRegister);
        btnRegisterBck = findViewById(R.id.btnRegisterBck);

        // Retour vers login
        btnRegisterBck.setOnClickListener(v -> finish()); // ferme l'activité et revient au login

        // Créer compte
        btnRegister.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Créer l'utilisateur Firebase
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.sendEmailVerification()
                                        .addOnCompleteListener(verifyTask -> {
                                            if (verifyTask.isSuccessful()) {
                                                Toast.makeText(this,
                                                        "Account created! Check your email for verification.",
                                                        Toast.LENGTH_LONG).show();
                                                // Reste sur la page, pas de login automatique
                                            } else {
                                                Toast.makeText(this,
                                                        "Failed to send verification email.",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(this,
                                    "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}

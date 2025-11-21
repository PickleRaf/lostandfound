package com.example.myapplication.activities.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.myapplication.R;
import com.example.myapplication.activities.auth.LoginActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class SettingsActivity extends AppCompatActivity {

    // UI Elements
    private Button btnChangePassword, btnLogout, btnDeleteAccount, btnBack;
    private SwitchCompat switchNotifications, switchChatSounds, switchEmailNotifs;
    private TextView tvAppVersion, tvBuildNumber;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // SharedPreferences for notification settings
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "LOFOSettings";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_CHAT_SOUNDS = "chat_sounds_enabled";
    private static final String KEY_EMAIL_NOTIFS = "email_notifications_enabled";

    // Permission launcher for notifications
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_settings);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Link UI elements
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnBack = findViewById(R.id.btnBacksettings);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchChatSounds = findViewById(R.id.switchChatSounds);
        switchEmailNotifs = findViewById(R.id.switchEmailNotifs);
        tvAppVersion = findViewById(R.id.tvAppVersion);
        tvBuildNumber = findViewById(R.id.tvBuildNumber);
        progressBar = findViewById(R.id.progressBarSettings);

        // Setup notification permission launcher
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        switchNotifications.setChecked(true);
                        saveNotificationPreference(KEY_NOTIFICATIONS, true);
                        Toast.makeText(this, R.string.notifications_enabled, Toast.LENGTH_SHORT).show();
                    } else {
                        switchNotifications.setChecked(false);
                        saveNotificationPreference(KEY_NOTIFICATIONS, false);
                        Toast.makeText(this, R.string.notifications_permission_denied, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Load saved preferences
        loadSavedPreferences();

        // Display app version info
        displayAppVersion();

        // Setup click listeners
        setupListeners();
    }

    private void loadSavedPreferences() {
        switchNotifications.setChecked(prefs.getBoolean(KEY_NOTIFICATIONS, true));
        switchChatSounds.setChecked(prefs.getBoolean(KEY_CHAT_SOUNDS, true));
        switchEmailNotifs.setChecked(prefs.getBoolean(KEY_EMAIL_NOTIFS, true));
    }

    private void saveNotificationPreference(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    private void displayAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            long versionCode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = pInfo.getLongVersionCode();
            } else {
                versionCode = pInfo.versionCode;
            }
            tvAppVersion.setText(getString(R.string.settings_version, version));
            tvBuildNumber.setText(getString(R.string.settings_build, versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            tvAppVersion.setText(R.string.settings_version_na);
            tvBuildNumber.setText(R.string.settings_build_na);
        }
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Change Password
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Logout
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        // Delete Account
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());

        // Notification toggles
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Request notification permission for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    saveNotificationPreference(KEY_NOTIFICATIONS, true);
                    Toast.makeText(this, R.string.notifications_enabled, Toast.LENGTH_SHORT).show();
                }
            } else {
                saveNotificationPreference(KEY_NOTIFICATIONS, false);
                Toast.makeText(this, R.string.notifications_disabled, Toast.LENGTH_SHORT).show();
            }
        });

        switchChatSounds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference(KEY_CHAT_SOUNDS, isChecked);
            Toast.makeText(this,
                    isChecked ? R.string.chat_sounds_enabled : R.string.chat_sounds_disabled,
                    Toast.LENGTH_SHORT).show();
        });

        switchEmailNotifs.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference(KEY_EMAIL_NOTIFS, isChecked);
            updateEmailNotificationPreference(isChecked);
        });
    }

    private void updateEmailNotificationPreference(boolean enabled) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .update("emailNotifications", enabled)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this,
                                    enabled ? R.string.email_notifications_enabled : R.string.email_notifications_disabled,
                                    Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, R.string.settings_update_preference_failed, Toast.LENGTH_SHORT).show());
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.dialog_logout_confirm, (dialog, which) -> {
                    // Sign out from Firebase
                    mAuth.signOut();

                    // Clear preferences if needed
                    // prefs.edit().clear().apply(); // Uncomment if you want to clear settings on logout

                    Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();

                    // Navigate to login screen
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, R.string.settings_error_no_user, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user signed in with Google
        if (user.getProviderData().size() > 1 &&
                user.getProviderData().get(1).getProviderId().equals("google.com")) {
            Toast.makeText(this, R.string.password_google_user_error,
                    Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etCurrentPassword = new EditText(this);
        etCurrentPassword.setHint(R.string.dialog_current_password_hint);
        etCurrentPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etCurrentPassword);

        EditText etNewPassword = new EditText(this);
        etNewPassword.setHint(R.string.dialog_new_password_hint);
        etNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPassword);

        EditText etConfirmPassword = new EditText(this);
        etConfirmPassword.setHint(R.string.dialog_confirm_password_hint);
        etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmPassword);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_change_password_title)
                .setView(layout)
                .setPositiveButton(R.string.dialog_change_button, (dialog, which) -> {
                    String currentPwd = etCurrentPassword.getText().toString().trim();
                    String newPwd = etNewPassword.getText().toString().trim();
                    String confirmPwd = etConfirmPassword.getText().toString().trim();

                    if (validatePasswordChange(currentPwd, newPwd, confirmPwd)) {
                        changePassword(user, currentPwd, newPwd);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private boolean validatePasswordChange(String current, String newPwd, String confirm) {
        if (current.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, R.string.password_validation_empty_fields, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (newPwd.length() < 8) {
            Toast.makeText(this, R.string.password_validation_min_length, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!newPwd.matches(".*[A-Z].*")) {
            Toast.makeText(this, R.string.password_validation_uppercase, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!newPwd.matches(".*[a-z].*")) {
            Toast.makeText(this, R.string.password_validation_lowercase, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!newPwd.matches(".*\\d.*")) {
            Toast.makeText(this, R.string.password_validation_number, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!newPwd.equals(confirm)) {
            Toast.makeText(this, R.string.password_validation_mismatch, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (current.equals(newPwd)) {
            Toast.makeText(this, R.string.password_validation_same_as_current, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void changePassword(FirebaseUser user, String currentPwd, String newPwd) {
        showLoading(true);

        // Re-authenticate user first
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPwd);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Now change the password
                    user.updatePassword(newPwd)
                            .addOnSuccessListener(aVoid1 -> {
                                showLoading(false);
                                Toast.makeText(this, R.string.password_changed_success,
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, getString(R.string.password_change_failed, e.getMessage()),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, R.string.password_current_incorrect, Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_account_title)
                .setMessage(R.string.dialog_delete_account_message)
                .setPositiveButton(R.string.dialog_delete_button, (dialog, which) -> showPasswordConfirmationForDelete())
                .setNegativeButton(R.string.dialog_cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showPasswordConfirmationForDelete() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Check if Google sign-in user
        boolean isGoogleUser = user.getProviderData().size() > 1 &&
                user.getProviderData().get(1).getProviderId().equals("google.com");

        if (isGoogleUser) {
            // For Google users, just confirm with a dialog
            EditText etConfirm = new EditText(this);
            etConfirm.setHint(R.string.dialog_delete_type_delete_hint);
            etConfirm.setPadding(50, 40, 50, 10);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_delete_confirm_title)
                    .setMessage(R.string.dialog_delete_type_delete_message)
                    .setView(etConfirm)
                    .setPositiveButton(R.string.dialog_confirm_button, (dialog, which) -> {
                        if (etConfirm.getText().toString().equals("DELETE")) {
                            deleteUserData(user.getUid());
                        } else {
                            Toast.makeText(this, R.string.delete_confirmation_incorrect, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        } else {
            // For email users, require password
            EditText etPassword = new EditText(this);
            etPassword.setHint(R.string.dialog_enter_password_hint);
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.setPadding(50, 40, 50, 10);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_confirm_password_title)
                    .setMessage(R.string.dialog_confirm_password_message)
                    .setView(etPassword)
                    .setPositiveButton(R.string.dialog_delete_account_button, (dialog, which) -> {
                        String password = etPassword.getText().toString().trim();
                        if (!password.isEmpty()) {
                            deleteAccount(password);
                        } else {
                            Toast.makeText(this, R.string.password_required, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        }
    }

    private void deleteAccount(String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        showLoading(true);

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> deleteUserData(user.getUid()))
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, R.string.password_incorrect, Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteUserData(String userId) {
        // Delete user's reported items
        db.collection("lost_items")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                    }
                    deleteUserChats(userId);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, getString(R.string.delete_error_items, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteUserChats(String userId) {
        // Delete chats where user is the claimer
        db.collection("chats")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        deleteMessagesAndChat(doc.getId());
                    }
                    deleteReporterChats(userId);
                });
    }

    private void deleteReporterChats(String userId) {
        // Delete chats where user is the reporter
        db.collection("chats")
                .whereEqualTo("reporterId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        deleteMessagesAndChat(doc.getId());
                    }
                    deleteUserProfile(userId);
                });
    }

    private void deleteMessagesAndChat(String chatId) {
        // Delete all messages in the chat
        db.collection("chats").document(chatId).collection("messages")
                .get()
                .addOnSuccessListener(messages -> {
                    for (QueryDocumentSnapshot msg : messages) {
                        msg.getReference().delete();
                    }
                    // Then delete the chat document
                    db.collection("chats").document(chatId).delete();
                });
    }

    private void deleteUserProfile(String userId) {
        // Delete user profile from Firestore
        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> deleteFirebaseUser())
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, getString(R.string.delete_error_profile, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteFirebaseUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.delete()
                    .addOnSuccessListener(aVoid -> {
                        showLoading(false);
                        Toast.makeText(this, R.string.account_deleted_success, Toast.LENGTH_SHORT).show();

                        // Clear preferences
                        prefs.edit().clear().apply();

                        // Navigate to login
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, getString(R.string.delete_error_account, e.getMessage()),
                                Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnChangePassword.setEnabled(!show);
        btnLogout.setEnabled(!show);
        btnDeleteAccount.setEnabled(!show);
        btnBack.setEnabled(!show);
    }
}
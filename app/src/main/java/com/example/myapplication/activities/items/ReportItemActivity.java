package com.example.myapplication.activities.items;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReportItemActivity extends AppCompatActivity {

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI elements
    private Spinner spinner;
    private EditText etDescription, etLocation;
    private Button btnReport, btnBack;
    private ProgressBar progressBar; // PHASE 1: Loading indicator
    private TextView tvDescCharCount, tvLocCharCount; // PHASE 3: Character counters

    // PHASE 3: Rate limiting
    private long lastReportTime = 0;
    private static final long REPORT_COOLDOWN = 60000; // 1 minute in milliseconds

    // PHASE 3: Character limits
    private static final int DESC_MAX_LENGTH = 200;
    private static final int LOC_MAX_LENGTH = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.items_activity_report_item);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Link UI elements
        spinner = findViewById(R.id.spinnerType);
        etDescription = findViewById(R.id.etDetails);
        etLocation = findViewById(R.id.etlocation);
        btnReport = findViewById(R.id.btnSubmitReport);
        btnBack = findViewById(R.id.btnBackRPI);
        progressBar = findViewById(R.id.progressBarReport);
        tvDescCharCount = findViewById(R.id.tvDescCharCount);
        tvLocCharCount = findViewById(R.id.tvLocCharCount);

        // Setup spinner (dropdown) for item type selection
        setupSpinner();

        // PHASE 3: Setup character counters
        setupCharacterCounters();

        // SUBMIT REPORT BUTTON
        btnReport.setOnClickListener(v -> {
            // PHASE 3: Check rate limit
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastReportTime < REPORT_COOLDOWN) {
                long remainingTime = (REPORT_COOLDOWN - (currentTime - lastReportTime)) / 1000;
                Toast.makeText(this, "Please wait " + remainingTime + " seconds before reporting another item",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Get values from form fields
            String title = spinner.getSelectedItem().toString().trim();
            String description = etDescription.getText().toString().trim();
            String location = etLocation.getText().toString().trim();

            // PHASE 1: Validate all fields are filled
            if (!validateInputs(title, description, location)) {
                return;
            }

            // Show loading indicator
            showLoading(true);

            // Create map to store item data
            Map<String, Object> lostItem = new HashMap<>();
            lostItem.put("title", title);
            lostItem.put("description", description);
            lostItem.put("location", location);
            lostItem.put("dateReported", FieldValue.serverTimestamp());
            lostItem.put("userId", mAuth.getCurrentUser().getUid());
            lostItem.put("status", "active"); // PHASE 2: Item status management

            // Save to Firestore database
            db.collection("lost_items")
                    .add(lostItem)
                    .addOnSuccessListener(documentReference -> {
                        showLoading(false);
                        // Update last report time for rate limiting
                        lastReportTime = System.currentTimeMillis();
                        Toast.makeText(this, "Item reported successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // Close this screen and return to dashboard
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // BACK BUTTON - Return to dashboard
        btnBack.setOnClickListener(v -> finish());
    }

    // Setup spinner with custom styling
    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                this,
                R.layout.component_spinner_collapsed,
                getResources().getStringArray(R.array.object_types)
        ) {
            @Override
            public boolean isEnabled(int position) {
                // Disable first item (placeholder text "Select object type")
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                // Custom styling for dropdown items
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;

                // Gray out the placeholder item
                if (position == 0) {
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.WHITE);
                }
                return view;
            }
        };

        // Set layout for opened dropdown
        adapter.setDropDownViewResource(R.layout.component_spinner_open);
        spinner.setAdapter(adapter);
    }

    // PHASE 3: Setup character counters for text fields
    private void setupCharacterCounters() {
        // Description character counter
        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                tvDescCharCount.setText(length + "/" + DESC_MAX_LENGTH);

                // Change color if approaching limit
                if (length > DESC_MAX_LENGTH * 0.9) {
                    tvDescCharCount.setTextColor(Color.RED);
                } else if (length > DESC_MAX_LENGTH * 0.7) {
                    tvDescCharCount.setTextColor(Color.YELLOW);
                } else {
                    tvDescCharCount.setTextColor(Color.WHITE);
                }

                // Prevent going over limit
                if (length > DESC_MAX_LENGTH) {
                    etDescription.setText(s.subSequence(0, DESC_MAX_LENGTH));
                    etDescription.setSelection(DESC_MAX_LENGTH);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Location character counter
        etLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                tvLocCharCount.setText(length + "/" + LOC_MAX_LENGTH);

                // Change color if approaching limit
                if (length > LOC_MAX_LENGTH * 0.9) {
                    tvLocCharCount.setTextColor(Color.RED);
                } else if (length > LOC_MAX_LENGTH * 0.7) {
                    tvLocCharCount.setTextColor(Color.YELLOW);
                } else {
                    tvLocCharCount.setTextColor(Color.WHITE);
                }

                // Prevent going over limit
                if (length > LOC_MAX_LENGTH) {
                    etLocation.setText(s.subSequence(0, LOC_MAX_LENGTH));
                    etLocation.setSelection(LOC_MAX_LENGTH);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // PHASE 1: Validate form inputs
    private boolean validateInputs(String title, String description, String location) {
        // Check if item type is selected
        if (title.isEmpty() || title.equals("Select object type")) {
            Toast.makeText(this, "Please select an item type", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if description is filled
        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check description minimum length
        if (description.length() < 10) {
            etDescription.setError("Description too short");
            Toast.makeText(this, "Please provide more details (at least 10 characters)",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if location is filled
        if (location.isEmpty()) {
            etLocation.setError("Location is required");
            Toast.makeText(this, "Please enter where you found the item", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check location minimum length
        if (location.length() < 5) {
            etLocation.setError("Location too short");
            Toast.makeText(this, "Please provide more specific location details",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // PHASE 1: Show/hide loading indicator
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnReport.setEnabled(!show);
        spinner.setEnabled(!show);
        etDescription.setEnabled(!show);
        etLocation.setEnabled(!show);
    }
}
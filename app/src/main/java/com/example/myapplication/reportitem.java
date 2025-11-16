package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class reportitem extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reportanitem);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        Spinner spinner = findViewById(R.id.spinnerType);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                this,
                R.layout.spinnercollapsed,
                getResources().getStringArray(R.array.object_types)
        ) {

            @Override
            public boolean isEnabled(int position) {
                // Disable the first item
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;

                if (position == 0) {
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.WHITE);
                }

                return view;
            }
        };

        adapter.setDropDownViewResource(R.layout.spinneropen);
        spinner.setAdapter(adapter);

        @SuppressLint("WrongViewCast")
        Spinner spinner1 = findViewById(R.id.spinnerType);
        EditText etDescription = findViewById(R.id.etDetails);
        EditText etLocation = findViewById(R.id.etlocation);
        Button btnReport = findViewById(R.id.btnSubmitReport);

        btnReport.setOnClickListener(v -> {
            String title = spinner1.getSelectedItem().toString().trim();
            String description = etDescription.getText().toString().trim();
            String location = etLocation.getText().toString().trim();

            if(title.isEmpty() || description.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> lostItem = new HashMap<>();
            lostItem.put("title", title);
            lostItem.put("description", description);
            lostItem.put("location", location);
            lostItem.put("dateReported", FieldValue.serverTimestamp());
            lostItem.put("userId", mAuth.getCurrentUser().getUid());

            db.collection("lost_items")
                    .add(lostItem)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Item reported successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // close activity or clear fields
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });



        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        Button btnBack = findViewById(R.id.btnBackRPI);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(reportitem.this, Activity2.class);
            startActivity(intent);
        });
    }
}

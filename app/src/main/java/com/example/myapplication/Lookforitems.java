package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Lookforitems extends AppCompatActivity {

    // Data and adapter
    private ItemAdapter itemAdapter;
    private List<Item> itemList;

    // Firebase
    private FirebaseFirestore db;

    // UI elements
    private GridView gridView;
    private EditText etSearch;
    private Button btnBack;
    private ProgressBar progressBar; // PHASE 1: Loading indicator
    private TextView tvEmptyState; // PHASE 1: Empty state message
    private SwipeRefreshLayout swipeRefresh; // PHASE 3: Pull-to-refresh

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lookforitems);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Link UI elements
        gridView = findViewById(R.id.gridView);
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBackLFI);
        progressBar = findViewById(R.id.progressBarLFI);
        tvEmptyState = findViewById(R.id.tvEmptyStateLFI);
        swipeRefresh = findViewById(R.id.swipeRefreshLFI);

        // Initialize item list and adapter
        itemList = new ArrayList<>();
        itemAdapter = new ItemAdapter(this, itemList);
        gridView.setAdapter(itemAdapter);

        // PHASE 3: Setup pull-to-refresh
        swipeRefresh.setOnRefreshListener(() -> {
            fetchItemsFromFirestore();
        });

        // Fetch items from Firestore
        fetchItemsFromFirestore();

        // GRID ITEM CLICK - Open detail view
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            Item clickedItem = (Item) itemAdapter.getItem(position);

            Intent intent = new Intent(Lookforitems.this, DisplayLostItem.class);
            intent.putExtra("title", clickedItem.getName());
            intent.putExtra("description", clickedItem.getDescription());
            intent.putExtra("location", clickedItem.getLocation());
            intent.putExtra("userID", clickedItem.getUserID());
            intent.putExtra("itemID", clickedItem.getItemID());
            startActivity(intent);
        });

        // SEARCH FUNCTIONALITY - Real-time filtering
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (itemAdapter != null) {
                    itemAdapter.getFilter().filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // BACK BUTTON
        btnBack.setOnClickListener(v -> finish());
    }

    // Fetch all items from Firestore
    private void fetchItemsFromFirestore() {
        // PHASE 1: Show loading indicator
        showLoading(true);

        db.collection("lost_items")
                .orderBy("dateReported")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Hide loading
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);

                    List<Item> fetchedItems = new ArrayList<>();

                    // Loop through all documents
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // Get fields from document
                        String title = doc.getString("title");
                        String desc = doc.getString("description");
                        String loc = doc.getString("location");
                        String userid = doc.getString("userId");
                        String itemID = doc.getId();
                        String status = doc.getString("status"); // PHASE 2: Item status

                        // PHASE 2: Only show active items (not resolved)
                        if (status == null || !status.equals("resolved")) {
                            // Only add if all required fields exist
                            if (title != null && desc != null && loc != null) {
                                fetchedItems.add(new Item(title, desc, loc, userid, itemID));
                            }
                        }
                    }

                    // PHASE 1: Show empty state if no items
                    if (fetchedItems.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                        // Update adapter with fetched items
                        itemAdapter.updateItems(fetchedItems);
                    }
                })
                .addOnFailureListener(e -> {
                    // Hide loading
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error fetching items: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // PHASE 1: Show/hide loading indicator
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        gridView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // PHASE 1: Show/hide empty state message
    private void showEmptyState(boolean show) {
        if (show) {
            tvEmptyState.setVisibility(View.VISIBLE);
            gridView.setVisibility(View.GONE);
            tvEmptyState.setText("No items found yet.\nBe the first to report one!");
        } else {
            tvEmptyState.setVisibility(View.GONE);
            gridView.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
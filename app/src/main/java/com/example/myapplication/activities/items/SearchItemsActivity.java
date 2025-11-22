package com.example.myapplication.activities.items;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.models.Item;
import com.example.myapplication.adapters.ItemAdapter;
import com.example.myapplication.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchItemsActivity extends AppCompatActivity {

    // Data and adapter
    private ItemAdapter itemAdapter;
    private List<Item> itemList;

    // Firebase
    private FirebaseFirestore db;

    // UI elements
    private RecyclerView recyclerView;
    private EditText etSearch;
    private Button btnBack;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.items_activity_search_item);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Link UI elements
        recyclerView = findViewById(R.id.recyclerViewItems);
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBackLFI);
        progressBar = findViewById(R.id.progressBarLFI);
        tvEmptyState = findViewById(R.id.tvEmptyStateLFI);
        swipeRefresh = findViewById(R.id.swipeRefreshLFI);

        // Initialize item list and adapter
        itemList = new ArrayList<>();

        // Setup RecyclerView with GridLayoutManager (2 columns)
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        itemAdapter = new ItemAdapter(this, itemList, item -> {
            // Handle item click
            Intent intent = new Intent(SearchItemsActivity.this, ItemDetailActivity.class);
            intent.putExtra("title", item.getName());
            intent.putExtra("description", item.getDescription());
            intent.putExtra("location", item.getLocation());
            intent.putExtra("userID", item.getUserID());
            intent.putExtra("itemID", item.getItemID());
            startActivity(intent);
        });

        recyclerView.setAdapter(itemAdapter);

        // Setup pull-to-refresh
        swipeRefresh.setOnRefreshListener(this::fetchItemsFromFirestore);

        // Fetch items from Firestore
        fetchItemsFromFirestore();

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
        showLoading(true);

        db.collection("lost_items")
                .orderBy("dateReported")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);

                    List<Item> fetchedItems = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        String desc = doc.getString("description");
                        String loc = doc.getString("location");
                        String userid = doc.getString("userId");
                        String itemID = doc.getId();
                        String status = doc.getString("status");


                        if (title != null && desc != null && loc != null) {
                            fetchedItems.add(new Item(title, desc, loc, userid, itemID));
                        }

                    }

                    // Show empty state if no items
                    if (fetchedItems.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                        itemAdapter.updateItems(fetchedItems);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error fetching items: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Show/hide loading indicator
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // Show/hide empty state message
    private void showEmptyState(boolean show) {
        if (show) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setText("No items found yet.\nBe the first to report one!");
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
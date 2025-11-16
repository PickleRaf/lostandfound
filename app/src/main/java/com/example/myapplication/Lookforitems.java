package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Lookforitems extends AppCompatActivity {

    private ItemAdapter itemAdapter;
    private List<Item> itemList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lookforitems);

        db = FirebaseFirestore.getInstance();

        GridView gridView = findViewById(R.id.gridView);
        EditText etSearch = findViewById(R.id.etSearch);
        Button btnBack = findViewById(R.id.btnBackLFI);

        itemList = new ArrayList<>();
        itemAdapter = new ItemAdapter(this, itemList);
        gridView.setAdapter(itemAdapter);

        // Fetch items from Firestore
        fetchItemsFromFirestore();

        // Make grid items clickable
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            Item clickedItem = (Item) itemAdapter.getItem(position);

            Intent intent = new Intent(Lookforitems.this, DisplayLostItem.class);
            intent.putExtra("title", clickedItem.getName());
            intent.putExtra("description", clickedItem.getDescription());
            intent.putExtra("location", clickedItem.getLocation());
            intent.putExtra("userID", clickedItem.getUserID());
            intent.putExtra("itemID",clickedItem.getItemID());
            startActivity(intent);
        });

        // Search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (itemAdapter != null) {
                    itemAdapter.getFilter().filter(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Back button
        btnBack.setOnClickListener(v -> finish());
    }

    private void fetchItemsFromFirestore() {
        db.collection("lost_items")
                .orderBy("dateReported")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Item> fetchedItems = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        String desc = doc.getString("description");
                        String loc = doc.getString("location");
                        String userid = doc.getString("userId");
                        String itemID = doc.getId();
                        if (title != null && desc != null && loc != null) {
                            fetchedItems.add(new Item(title, desc, loc,userid,itemID));
                        }
                    }
                    itemAdapter.updateItems(fetchedItems); // update adapter
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error fetching items: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

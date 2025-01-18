package com.example.uoclabs;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.AbsoluteSizeSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class viewItems extends AppCompatActivity {

    private Spinner itemDropdown;
    private TextView totalQuantityTextView, brandTextView, locationTextView;
    private ImageView itemImageView;
    private ProgressBar progressBar;
    private DatabaseReference itemsReference, locationReference;
    private List<String> itemList = new ArrayList<>();
    private ArrayAdapter<String> itemsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_items);

        // Initialize views
        itemDropdown = findViewById(R.id.itemDropdown);
        totalQuantityTextView = findViewById(R.id.totalQuantityTextView);
        brandTextView = findViewById(R.id.brandTextView);
        locationTextView = findViewById(R.id.locationTextView);
        itemImageView = findViewById(R.id.itemImageView);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase Database references
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        itemsReference = database.getReference("items");
        locationReference = database.getReference("location");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("UOC Labs");

        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black)); // Set title text color if needed
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(viewItems.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional: Call finish() if you don't want to keep the current activity in the back stack
            }
        });

        // Load item names for dropdown
        loadItemNames();

        // Set listener for item selection in dropdown
        itemDropdown.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedItemName = itemList.get(position);
                loadItemDetails(selectedItemName);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    // Method to load item names from Firebase for the dropdown
    private void loadItemNames() {
        progressBar.setVisibility(View.VISIBLE);
        itemsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                itemList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String itemName = snapshot.child("itemName").getValue(String.class);
                    if (itemName != null) {
                        itemList.add(itemName);
                    }
                }
                itemsAdapter = new ArrayAdapter<>(viewItems.this, R.layout.spinner_item, itemList);
                itemsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                itemDropdown.setAdapter(itemsAdapter);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(viewItems.this, "Failed to load item names.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // Method to load item details (image, brand, quantity, locations) based on the selected item
    private void loadItemDetails(String itemName) {
        progressBar.setVisibility(View.VISIBLE);
        itemsReference.child(itemName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot itemSnapshot) {
                if (itemSnapshot.exists()) {
                    // Display total quantity and brand name
                    Integer totalQuantity = itemSnapshot.child("quantity").getValue(Integer.class);
                    String brandName = itemSnapshot.child("brandName").getValue(String.class);
                    String imageUrl = itemSnapshot.child("imageUrl").getValue(String.class); // Assuming you save image URL as "imageUrl"

                    // Set the totalQuantity with green color and size 30dp
                    String quantityText = "Total Quantity : \n" + (totalQuantity != null ? totalQuantity : "N/A");
                    SpannableString spannableQuantity = new SpannableString(quantityText);
                    int quantityStart = quantityText.indexOf("\n") + 1;
                    spannableQuantity.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.holo_green_dark)),
                            quantityStart, quantityText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannableQuantity.setSpan(new AbsoluteSizeSpan(30, true),
                            quantityStart, quantityText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    totalQuantityTextView.setText(spannableQuantity);

                    // Set the brandName with cyan color and size 30dp
                    String brandText = "Brand : \n" + (brandName != null ? brandName : "N/A");
                    SpannableString spannableBrand = new SpannableString(brandText);



                    int brandStart = brandText.indexOf("\n") + 1;
                    spannableBrand.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.holo_blue_light)),
                            brandStart, brandText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannableBrand.setSpan(new AbsoluteSizeSpan(30, true),
                            brandStart, brandText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    brandTextView.setText(spannableBrand);

                    // Load and display the item image using Picasso or Glide
                    if (imageUrl != null) {
                        Picasso.get().load(imageUrl).into(itemImageView);
                    } else {
                        itemImageView.setImageResource(R.drawable.addimage); // Assuming you have a default image
                    }

                    // Fetch and display location quantities
                    loadLocationQuantities(itemName);

                } else {
                    Toast.makeText(viewItems.this, "Item details not found.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(viewItems.this, "Failed to load item details.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // Method to load and display quantities of the selected item across different locations
    private void loadLocationQuantities(String itemName) {
        locationReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot locationSnapshot) {
                StringBuilder locationDetails = new StringBuilder();

                for (DataSnapshot location : locationSnapshot.getChildren()) {
                    for (DataSnapshot item : location.child("items").getChildren()) {
                        String locItemName = item.child("itemName").getValue(String.class);
                        Integer locQuantity = item.child("quantity").getValue(Integer.class);

                        if (locItemName != null && locItemName.equals(itemName) && locQuantity != null) {
                            String locName = location.getKey();
                            locationDetails.append(locName).append("  :  ").append(locQuantity).append("\n");
                        }
                    }
                }

                locationTextView.setText(locationDetails.toString().trim().isEmpty() ? "No locations found." : locationDetails.toString().trim());
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(viewItems.this, "Failed to load location quantities.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed(); // Call the super method to handle the back button press

        // Navigate to the dashboard
        Intent intent = new Intent(viewItems.this, MainActivity.class);
        startActivity(intent);
        finish(); // Optional: Call finish() if you don't want to keep the current activity in the back stack
    }
}

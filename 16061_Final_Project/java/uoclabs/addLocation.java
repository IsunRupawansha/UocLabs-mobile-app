package com.example.uoclabs;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class addLocation extends AppCompatActivity {

    private AutoCompleteTextView peopleName, locationName, locationItem;
    private EditText itemQuan;
    private Button btnAddLocation, btnAddPeople, btnAddItem;
    private DatabaseReference peopleReference, itemsReference, locationReference;
    private ArrayAdapter<String> peopleAdapter, locationAdapter, itemsAdapter;
    private List<String> peopleList = new ArrayList<>();
    private List<String> itemsList = new ArrayList<>();
    private List<String> locationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_location);

        // Initialize Firebase Database references
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        peopleReference = database.getReference("people");
        itemsReference = database.getReference("items");
        locationReference = database.getReference("location");

        // Initialize views
        locationName = findViewById(R.id.locationName);
        peopleName = findViewById(R.id.peopleName);
        locationItem = findViewById(R.id.locationItem);
        itemQuan = findViewById(R.id.itemQuan);
        btnAddLocation = findViewById(R.id.btnAddLocation);
        btnAddPeople = findViewById(R.id.btnAddPeople);
        btnAddItem = findViewById(R.id.btnAddItem);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("UOC Labs");

        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black)); // Set title text color if needed
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(addLocation.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional: Call finish() if you don't want to keep the current activity in the back stack
            }
        });

        // Load data for suggestions
        loadPeopleNames();
        loadItemNames();
        loadLocationNames();

        // Set the adapter for peopleName, locationName, and locationItem AutoCompleteTextView
        peopleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, peopleList);
        peopleName.setAdapter(peopleAdapter);
        itemsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, itemsList);
        locationItem.setAdapter(itemsAdapter);
        locationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, locationList);
        locationName.setAdapter(locationAdapter);

        // Set item click listener for locationName AutoCompleteTextView
        locationName.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLocation = locationList.get(position);
            // Clear peopleName field when a new location is selected
            peopleName.setText("");
        });

        // Set click listener for the add location button
        btnAddLocation.setOnClickListener(v -> {
            String locName = locationName.getText().toString().trim();
            if (!locName.isEmpty()) {
                saveLocationName(locName);
            } else {
                Toast.makeText(this, "Please enter a location name", Toast.LENGTH_SHORT).show();
            }
        });

        // Set click listener for the add people button
        btnAddPeople.setOnClickListener(v -> {
            String locName = locationName.getText().toString().trim();
            String peopleNameStr = peopleName.getText().toString().trim();
            if (!locName.isEmpty() && !peopleNameStr.isEmpty()) {
                addPersonToLocation(locName, peopleNameStr);
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        // Set click listener for the add item button
        btnAddItem.setOnClickListener(v -> validateAndSaveLocation());
    }

    // Method to save location name to Firebase
    private void saveLocationName(String locName) {
        locationReference.child(locName).setValue(new HashMap<>())
                .addOnSuccessListener(aVoid -> Toast.makeText(addLocation.this, "Location added.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(addLocation.this, "Failed to add location: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    // Method to add a person to the location in Firebase
    private void addPersonToLocation(String locName, String personName) {
        // Check if the personName exists in the people branch
        peopleReference.child(personName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Person exists, proceed with adding to location
                    DatabaseReference locRef = locationReference.child(locName).child("people").push();
                    locRef.setValue(personName)
                            .addOnSuccessListener(aVoid -> Toast.makeText(addLocation.this, "Person added to location.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(addLocation.this, "Failed to add person: " + e.getMessage(), Toast.LENGTH_LONG).show());
                } else {
                    // Person does not exist, show an error message
                    Toast.makeText(addLocation.this, "Not a valid name. Please select a name from the suggestions.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(addLocation.this, "Failed to validate person name: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    // Method to validate item data and save to Firebase
    private void validateAndSaveLocation() {
        String selectedItemName = locationItem.getText().toString().trim();
        String enteredQuantityStr = itemQuan.getText().toString().trim();
        String locName = locationName.getText().toString().trim();

        if (!selectedItemName.isEmpty() && !enteredQuantityStr.isEmpty()) {
            try {
                int enteredQuantity = Integer.parseInt(enteredQuantityStr);

                // Step 1: Retrieve the total available quantity from the 'items' branch
                itemsReference.child(selectedItemName).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot itemSnapshot) {
                        if (itemSnapshot.exists()) {
                            Integer totalItemQuantity = itemSnapshot.child("quantity").getValue(Integer.class);

                            // Step 2: Calculate the already allocated quantity across all locations
                            locationReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot locationSnapshot) {
                                    int allocatedQuantity = 0;

                                    for (DataSnapshot location : locationSnapshot.getChildren()) {
                                        for (DataSnapshot item : location.child("items").getChildren()) {
                                            String itemName = item.child("itemName").getValue(String.class);
                                            Integer locQuantity = item.child("quantity").getValue(Integer.class);
                                            if (itemName != null && locQuantity != null && itemName.equals(selectedItemName)) {
                                                allocatedQuantity += locQuantity;
                                            }
                                        }
                                    }

                                    // Step 3: Check if adding the new quantity exceeds the total available quantity
                                    if (totalItemQuantity != null && allocatedQuantity + enteredQuantity <= totalItemQuantity) {
                                        // Step 4: Save location data if valid
                                        addItemToLocation(locName, selectedItemName, enteredQuantity);
                                    } else {
                                        Toast.makeText(addLocation.this, "Quantity exceeds the available item quantity.", Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Toast.makeText(addLocation.this, "Failed to check allocated quantity: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });

                        } else {
                            Toast.makeText(addLocation.this, "Item does not exist. Please select a valid item.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(addLocation.this, "Failed to check item quantity: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to add an item to the location in Firebase
    private void addItemToLocation(String locName, String itemName, int quantity) {
        DatabaseReference locRef = locationReference.child(locName).child("items");

        // Create a map to store item data
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("itemName", itemName);
        itemData.put("quantity", quantity);

        // Directly update the 'items' node for the specified location
        locRef.child(itemName).setValue(itemData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(addLocation.this, "Item added to location.", Toast.LENGTH_SHORT).show();
                    // Clear the item name and quantity fields
                    locationItem.setText("");
                    itemQuan.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(addLocation.this, "Failed to add item: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // Method to load people names from Firebase for suggestions
    private void loadPeopleNames() {
        peopleReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                peopleList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String name = snapshot.getKey(); // Changed to get key as the person name
                    if (name != null) {
                        peopleList.add(name);
                    }
                }
                peopleAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(addLocation.this, "Failed to load people names.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to load item names from Firebase for suggestions
    private void loadItemNames() {
        itemsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                itemsList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String itemName = snapshot.child("itemName").getValue(String.class);
                    if (itemName != null) {
                        itemsList.add(itemName);
                    }
                }
                itemsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(addLocation.this, "Failed to load item names.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to load location names from Firebase for suggestions
    private void loadLocationNames() {
        locationReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                locationList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String locName = snapshot.getKey();
                    if (locName != null) {
                        locationList.add(locName);
                    }
                }
                locationAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(addLocation.this, "Failed to load location names.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onBackPressed() {
        super.onBackPressed(); // Call the super method to handle the back button press

        // Navigate to the dashboard
        Intent intent = new Intent(addLocation.this, MainActivity.class);
        startActivity(intent);
        finish(); // Optional: Call finish() if you don't want to keep the current activity in the back stack
    }
}

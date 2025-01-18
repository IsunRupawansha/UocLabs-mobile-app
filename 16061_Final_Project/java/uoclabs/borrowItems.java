package com.example.uoclabs;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
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

public class borrowItems extends AppCompatActivity {

    private Spinner nameDropdown, fromDropdown, itemDropdown, borrowToDropdown;
    private TextView availableQuantityTextView;
    private EditText borrowingQuantityEditText;
    private Button saveBorrowButton;

    private DatabaseReference peopleReference, locationReference;
    private List<String> peopleList = new ArrayList<>();
    private List<String> locationList = new ArrayList<>();
    private List<String> itemList = new ArrayList<>();
    private ArrayAdapter<String> itemAdapter;

    private int availableQuantity = 0;
    private String selectedItem = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_borrow_items);

        // Initialize views
        nameDropdown = findViewById(R.id.nameDropdown);
        fromDropdown = findViewById(R.id.fromDropdown);
        itemDropdown = findViewById(R.id.itemDropdown);
        borrowToDropdown = findViewById(R.id.borrowToDropdown);
        availableQuantityTextView = findViewById(R.id.availablequantity);
        borrowingQuantityEditText = findViewById(R.id.borrowingquantity);
        saveBorrowButton = findViewById(R.id.saveborrow);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("UOC Labs");

        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black)); // Set title text color if needed
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(borrowItems.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional: Call finish() if you don't want to keep the current activity in the back stack
            }
        });

        // Initialize Firebase references
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        peopleReference = database.getReference("people");
        locationReference = database.getReference("location");

        // Load people, locations, and set listeners
        loadPeopleNames();
        loadLocationNames(fromDropdown);
        loadLocationNames(borrowToDropdown);

        // Listener for when a location is selected from the "fromDropdown"
        fromDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (locationList.isEmpty()) {
                    return; // Exit if locationList is empty to prevent crash
                }
                String selectedLocation = locationList.get(position);
                loadItemsForLocation(selectedLocation);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Listener for when an item is selected from the "itemDropdown"
        itemDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (itemList.isEmpty()) {
                    return; // Exit if itemList is empty to prevent crash
                }
                selectedItem = itemList.get(position);
                loadAvailableQuantity(fromDropdown.getSelectedItem().toString(), selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Save button listener
        saveBorrowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBorrow();
            }
        });
    }

    private void loadPeopleNames() {
        peopleReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                peopleList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    HashMap<String, String> personMap = (HashMap<String, String>) snapshot.getValue();
                    if (personMap != null && personMap.containsKey("name")) {
                        peopleList.add(personMap.get("name"));
                    }
                }
                ArrayAdapter<String> peopleAdapter = new ArrayAdapter<>(borrowItems.this, android.R.layout.simple_spinner_item, peopleList);
                peopleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                nameDropdown.setAdapter(peopleAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(borrowItems.this, "Failed to load people.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLocationNames(Spinner dropdown) {
        locationReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                locationList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String locationName = snapshot.getKey();
                    if (locationName != null) {
                        locationList.add(locationName);
                    }
                }
                ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(borrowItems.this, android.R.layout.simple_spinner_item, locationList);
                locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                dropdown.setAdapter(locationAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(borrowItems.this, "Failed to load locations.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadItemsForLocation(String locationName) {
        locationReference.child(locationName).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                itemList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> itemMap = (Map<String, Object>) snapshot.getValue();
                    if (itemMap != null && itemMap.containsKey("itemName")) {
                        itemList.add((String) itemMap.get("itemName"));
                    }
                }
                itemAdapter = new ArrayAdapter<>(borrowItems.this, android.R.layout.simple_spinner_item, itemList);
                itemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                itemDropdown.setAdapter(itemAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(borrowItems.this, "Failed to load items.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAvailableQuantity(String locationName, String itemName) {
        locationReference.child(locationName).child("items").orderByChild("itemName").equalTo(itemName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                availableQuantity = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> itemMap = (Map<String, Object>) snapshot.getValue();
                    if (itemMap != null && itemMap.containsKey("quantity")) {
                        availableQuantity = ((Long) itemMap.get("quantity")).intValue(); // Convert from Long to int
                    }
                }
                availableQuantityTextView.setText("Available Quantity: " + availableQuantity);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(borrowItems.this, "Failed to load quantity.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveBorrow() {
        String selectedLocationFrom = fromDropdown.getSelectedItem().toString();
        String selectedLocationTo = borrowToDropdown.getSelectedItem().toString();
        String borrowingQuantityStr = borrowingQuantityEditText.getText().toString();
        int borrowingQuantity;

        try {
            borrowingQuantity = Integer.parseInt(borrowingQuantityStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prevent borrowing from the same location to the same location
        if (selectedLocationFrom.equals(selectedLocationTo)) {
            Toast.makeText(this, "Cannot borrow items to the same location.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (borrowingQuantity > availableQuantity) {
            Toast.makeText(this, "Borrowing quantity exceeds available quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update quantity in "from" location
        updateQuantity(selectedLocationFrom, selectedItem, -borrowingQuantity);

        // Update or create quantity in "to" location
        updateOrCreateQuantity(selectedLocationTo, selectedItem, borrowingQuantity);
    }

    private void updateQuantity(String locationName, String itemName, int quantityChange) {
        locationReference.child(locationName).child("items").orderByChild("itemName").equalTo(itemName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    int currentQuantity = ((Long) snapshot.child("quantity").getValue()).intValue();
                    int newQuantity = currentQuantity + quantityChange;
                    if (newQuantity < 0) {
                        newQuantity = 0; // Prevent negative quantities
                    }
                    snapshot.getRef().child("quantity").setValue(newQuantity);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(borrowItems.this, "Failed to update quantity.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateOrCreateQuantity(String locationName, String itemName, int quantityChange) {
        locationReference.child(locationName).child("items").orderByChild("itemName").equalTo(itemName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Update existing item quantity
                    updateQuantity(locationName, itemName, quantityChange);
                } else {
                    // Create new item entry with the given quantity
                    DatabaseReference newItemRef = locationReference.child(locationName).child("items").push();
                    newItemRef.child("itemName").setValue(itemName);
                    newItemRef.child("quantity").setValue(quantityChange);
                }
                Toast.makeText(borrowItems.this, "Item borrowed successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(borrowItems.this, "Failed to update or create item.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onBackPressed() {
        super.onBackPressed(); // Call the super method to handle the back button press

        // Navigate to the dashboard
        Intent intent = new Intent(borrowItems.this, MainActivity.class);
        startActivity(intent);
        finish(); // Optional: Call finish() if you don't want to keep the current activity in the back stack
    }
}


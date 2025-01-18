package com.example.uoclabs;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class Settings extends AppCompatActivity {

    private AutoCompleteTextView dltItem, dltName, dltLocation;
    private Button btnDeleteItems, btnDeletePeople, btnDeleteLocation;
    private DatabaseReference itemsRef, peopleRef, locationRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dltItem = findViewById(R.id.dltItem);
        dltName = findViewById(R.id.dltName);
        dltLocation = findViewById(R.id.dltLocation);
        btnDeleteItems = findViewById(R.id.btnDeleteItems);
        btnDeletePeople = findViewById(R.id.btnDeletePeople);
        btnDeleteLocation = findViewById(R.id.btnDeleteLocation);

        itemsRef = FirebaseDatabase.getInstance().getReference("items");
        peopleRef = FirebaseDatabase.getInstance().getReference("people");
        locationRef = FirebaseDatabase.getInstance().getReference("location");

        // Load suggestions from the database for all fields
        loadSuggestions(itemsRef, dltItem);
        loadSuggestions(peopleRef, dltName);
        loadSuggestions(locationRef, dltLocation);

        btnDeleteItems.setOnClickListener(view -> deleteItem());
        btnDeletePeople.setOnClickListener(view -> deleteName());
        btnDeleteLocation.setOnClickListener(view -> deleteLocation());
    }

    private void loadSuggestions(DatabaseReference reference, AutoCompleteTextView autoCompleteTextView) {
        reference.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> suggestions = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    suggestions.add(dataSnapshot.getKey()); // Assuming keys are the names of items/people/locations
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(Settings.this, android.R.layout.simple_dropdown_item_1line, suggestions);
                autoCompleteTextView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(Settings.this, "Failed to load suggestions.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteItem() {
        String itemName = dltItem.getText().toString().trim();
        if (!itemName.isEmpty()) {
            // Step 1: Remove the item from the "items" branch
            itemsRef.child(itemName).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Step 2: Remove the item from all locations
                    locationRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
                                DatabaseReference itemRefInLocation = locationSnapshot.getRef().child("items").child(itemName);
                                itemRefInLocation.removeValue();  // Remove the item from each location
                            }
                            Toast.makeText(Settings.this, "Item deleted from all locations successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                            Toast.makeText(Settings.this, "Failed to delete item from locations", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(Settings.this, "Failed to delete item from items branch", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(Settings.this, "Item name cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }


    private void deleteName() {
        String personName = dltName.getText().toString().trim();
        if (!personName.isEmpty()) {
            // Step 1: Remove the person from the "people" branch
            peopleRef.child(personName).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Step 2: Remove the person from all locations
                    locationRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
                                for (DataSnapshot peopleSnapshot : locationSnapshot.child("people").getChildren()) {
                                    String peopleInLocation = peopleSnapshot.getValue(String.class);
                                    if (personName.equals(peopleInLocation)) {
                                        // Remove the person from the specific location
                                        peopleSnapshot.getRef().removeValue();
                                    }
                                }
                            }
                            Toast.makeText(Settings.this, "Person deleted from all locations successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                            Toast.makeText(Settings.this, "Failed to delete person from locations", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(Settings.this, "Failed to delete person from people branch", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(Settings.this, "Person name cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }



    private void deleteLocation() {
        String locationName = dltLocation.getText().toString().trim();
        if (!locationName.isEmpty()) {
            locationRef.child(locationName).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(Settings.this, "Location deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Settings.this, "Failed to delete location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}

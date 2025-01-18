package com.example.uoclabs;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class viewLab extends AppCompatActivity {

    private Spinner locationDropdown;
    private TextView peopleTextView, itemTextView;
    private DatabaseReference locationReference;
    private List<String> locationList = new ArrayList<>();
    private ArrayAdapter<String> locationAdapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_lab);

        // Initialize views
        locationDropdown = findViewById(R.id.itemDropdown);
        peopleTextView = findViewById(R.id.PeopleTextView);
        itemTextView = findViewById(R.id.itemTextView);
        progressBar = findViewById(R.id.progressBar);

        peopleTextView.setTextSize(18);
        itemTextView.setTextSize(15);

        // Initialize Firebase Database reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        locationReference = database.getReference("location");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("UOC Labs");

        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black)); // Set title text color if needed
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(viewLab.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional: Call finish() if you don't want to keep the current activity in the back stack
            }
        });

        // Load locations for dropdown
        loadLocationNames();

        // Set listener for location selection in dropdown
        locationDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLocationName = locationList.get(position);
                loadLocationDetails(selectedLocationName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    // Method to load location names from Firebase for the dropdown
    private void loadLocationNames() {
        progressBar.setVisibility(View.VISIBLE);
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
                locationAdapter = new ArrayAdapter<>(viewLab.this, android.R.layout.simple_spinner_item, locationList);
                locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                locationDropdown.setAdapter(locationAdapter);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(viewLab.this, "Failed to load locations.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to load location details (people, items) based on the selected location
    private void loadLocationDetails(String locationName) {
        progressBar.setVisibility(View.VISIBLE);
        locationReference.child(locationName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot locationSnapshot) {
                if (locationSnapshot.exists()) {
                    // Display people
                    StringBuilder peopleDetails = new StringBuilder();
                    final List<String> peopleList = new ArrayList<>();
                    for (DataSnapshot peopleSnapshot : locationSnapshot.child("people").getChildren()) {
                        String personName = peopleSnapshot.getValue(String.class);
                        if (personName != null) {
                            peopleList.add(personName);
                            peopleDetails.append(personName).append("\n");
                        }
                    }
                    peopleTextView.setText(peopleDetails.toString().trim().isEmpty() ? "No people found." : peopleDetails.toString().trim());

                    // Set click listener for each person
                    peopleTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!peopleList.isEmpty()) {
                                // Create an AlertDialog to show a list of people
                                AlertDialog.Builder builder = new AlertDialog.Builder(viewLab.this);
                                builder.setTitle("Select a Person");
                                builder.setItems(peopleList.toArray(new String[0]), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // The 'which' parameter gives the index of the selected item
                                        String selectedPerson = peopleList.get(which);
                                        Intent intent = new Intent(viewLab.this, PersonDetailsActivity.class);
                                        intent.putExtra("personName", selectedPerson);
                                        startActivity(intent);
                                    }
                                });
                                builder.show();
                            }
                        }
                    });

                    // Display items and their quantities
                    StringBuilder itemDetails = new StringBuilder();
                    for (DataSnapshot itemSnapshot : locationSnapshot.child("items").getChildren()) {
                        String itemName = itemSnapshot.child("itemName").getValue(String.class);
                        Integer itemQuantity = itemSnapshot.child("quantity").getValue(Integer.class);
                        if (itemName != null && itemQuantity != null) {
                            itemDetails.append(itemName).append(": ").append(itemQuantity).append("\n");
                        }
                    }
                    itemTextView.setText(itemDetails.toString().trim().isEmpty() ? "No items found." : itemDetails.toString().trim());
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(viewLab.this, "Location details not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(viewLab.this, "Failed to load location details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed(); // Call the super method to handle the back button press

        // Navigate to the dashboard
        Intent intent = new Intent(viewLab.this, MainActivity.class);
        startActivity(intent);
        finish(); // Optional: Call finish() if you don't want to keep the current activity in the back stack
    }
}

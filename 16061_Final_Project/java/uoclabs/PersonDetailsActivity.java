// PersonDetailsActivity.java
package com.example.uoclabs;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PersonDetailsActivity extends AppCompatActivity {

    private TextView personNameTextView, personDetailsTextView;
    private DatabaseReference peopleReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_details);

        // Initialize views
        personNameTextView = findViewById(R.id.personNameTextView);
        personDetailsTextView = findViewById(R.id.personDetailsTextView);

        // Initialize Firebase Database reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        peopleReference = database.getReference("people");

        // Get the passed person's name
        String personName = getIntent().getStringExtra("personName");

        // Display the person's name
        personNameTextView.setText(personName);

        // Load the person's details from Firebase
        loadPersonDetails(personName);
    }

    // Method to load person details from Firebase
    private void loadPersonDetails(String personName) {
        peopleReference.child(personName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Assuming you have fields like "age", "role", etc. under each person
                    StringBuilder details = new StringBuilder();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String key = snapshot.getKey();
                        String value = snapshot.getValue(String.class);
                        details.append(key).append(": ").append(value).append("\n");
                    }
                    personDetailsTextView.setText(details.toString().trim());
                } else {
                    Toast.makeText(PersonDetailsActivity.this, "No details found for " + personName, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(PersonDetailsActivity.this, "Failed to load person details.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

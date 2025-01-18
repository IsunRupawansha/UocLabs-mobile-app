package com.example.uoclabs;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class addPeople extends AppCompatActivity {

    private EditText userName,userEmail,userMobileNo;
    private Button btnAddPeople;
    private DatabaseReference databaseReference;
    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_people);

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("people");

        // Initialize views
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        userMobileNo = findViewById(R.id.userMobileNo);
        spinner = findViewById(R.id.spinner);
        btnAddPeople = findViewById(R.id.btnAddPeople);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("UOC Labs");

        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black)); // Set title text color if needed
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(addPeople.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional: Call finish() if you don't want to keep the current activity in the back stack
            }
        });

        // Set click listener for the submit button
        btnAddPeople.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataToFirebase();
            }
        });


        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.dropdown_items, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        // Set an item selected listener for the spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get selected item
                String selectedItem = parent.getItemAtPosition(position).toString();
                // Display a toast message with the selected item
                Toast.makeText(parent.getContext(), "Selected: " + selectedItem, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        try {
            Field popup = Spinner.class.getDeclaredField("mPopup");
            popup.setAccessible(true);
            android.widget.ListPopupWindow popupWindow = (android.widget.ListPopupWindow) popup.get(spinner);
            popupWindow.setHeight(300); // Set the desired height in pixels
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendDataToFirebase() {
        String name = userName.getText().toString().trim();
        String email = userEmail.getText().toString().trim();
        String mobileStr = userMobileNo.getText().toString().trim();
        String designation = spinner.getSelectedItem().toString();

        if (!name.isEmpty() && !email.isEmpty() && !mobileStr.isEmpty()) {
            if (isValidEmail(email) && isInteger(mobileStr)) {
                int mobile = Integer.parseInt(mobileStr);

                // Create a unique ID for each item
                String itemId = databaseReference.push().getKey();

                // Create a map of key-value pairs
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("name", name);
                itemData.put("email", email);
                itemData.put("mobile", mobileStr);
                itemData.put("designation", designation);

                // Save the map to Firebase
                databaseReference.child(name).setValue(itemData, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            // Data could not be saved
                            Toast.makeText(addPeople.this, "Failed to save data: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                        } else {
                            // Data saved successfully
                            Toast.makeText(addPeople.this, "Data saved successfully", Toast.LENGTH_SHORT).show();
                            // Clear the EditText fields
                            clearFields();
                        }
                    }
                });
            } else {
                // Show a message if the email or mobile number is invalid
                Toast.makeText(addPeople.this, "Invalid email or mobile number", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Show a message if any field is empty
            Toast.makeText(addPeople.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidEmail(String email) {
        return email.endsWith("@sci.cmb.ac.lk") || email.endsWith("@phy.cmb.ac.lk");
    }

    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void clearFields() {
        userName.setText("");
        userEmail.setText("");
        userMobileNo.setText("");
        spinner.setSelection(0);
    }
    public void onBackPressed() {
        super.onBackPressed(); // Call the super method to handle the back button press

        // Navigate to the dashboard
        Intent intent = new Intent(addPeople.this, MainActivity.class);
        startActivity(intent);
        finish(); // Optional: Call finish() if you don't want to keep the current activity in the back stack
    }
}

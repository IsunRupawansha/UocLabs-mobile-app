package com.example.uoclabs;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.bumptech.glide.Glide;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class addItems extends AppCompatActivity {

    private EditText itemBrand, itemQuantity;
    private AutoCompleteTextView itemName;
    private Button btnAddItems, btnSelectImage ,btnRemoveImage;
    private ImageView itemImageView;
    private ProgressBar uploadProgressBar;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private ArrayAdapter<String> adapter;
    private List<String> itemNamesList = new ArrayList<>();
    private Uri imageUri;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_items);

        // Initialize Firebase Database and Storage
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("items");
        storageReference = FirebaseStorage.getInstance().getReference("item_images");

        // Initialize views
        itemName = findViewById(R.id.itemName);
        itemBrand = findViewById(R.id.itemBrand);
        itemQuantity = findViewById(R.id.itemQuantity);
        btnAddItems = findViewById(R.id.btnAddItems);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        itemImageView = findViewById(R.id.itemImage);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("UOC Labs");

        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black)); // Set title text color if needed
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(addItems.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional: Call finish() if you don't want to keep the current activity in the back stack
            }
        });

        // Initialize adapter for AutoCompleteTextView
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, itemNamesList);
        itemName.setAdapter(adapter);


        // Set click listener for the submit button
        btnAddItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataToFirebase();
            }
        });

        // Set click listener for the select image button
        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        // Set click listener for the remove image button
        btnRemoveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeImage();
            }
        });

        // Load item names from Firebase for suggestions
        loadItemNames();

        // Set item click listener for suggestions
        itemName.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = adapter.getItem(position);
            itemName.setText(selectedName);
            fillItemDetails(selectedName);
        });

        // Set focus change listener for the itemName EditText
        itemName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkItemExists();
                }
            }
        });
    }

    // Method to load item names from Firebase for suggestions
    private void loadItemNames() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                itemNamesList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String itemName = snapshot.getKey();
                    itemNamesList.add(itemName);
                }
                adapter.notifyDataSetChanged(); // Notify the adapter that data has changed
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(addItems.this, "Failed to load item names: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Method to send data to Firebase
    private void sendDataToFirebase() {
        String nameitem = itemName.getText().toString().trim();
        String brandName = itemBrand.getText().toString().trim();
        String quantity = itemQuantity.getText().toString().trim();

        if (!nameitem.isEmpty() && !brandName.isEmpty() && !quantity.isEmpty() && imageUri != null) {
            try {
                int quan = Integer.parseInt(quantity);

                // Upload image to Firebase Storage
                uploadProgressBar.setVisibility(View.VISIBLE);
                final StorageReference fileReference = storageReference.child(nameitem + ".jpg");
                fileReference.putFile(imageUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        // Get the download URL for the image
                                        String imageUrl = uri.toString();

                                        // Create a unique ID for each item
                                        String itemId = databaseReference.push().getKey();

                                        // Create a map of key-value pairs
                                        Map<String, Object> itemData = new HashMap<>();
                                        itemData.put("itemName", nameitem);
                                        itemData.put("brandName", brandName);
                                        itemData.put("quantity", quan);
                                        itemData.put("imageUrl", imageUrl);

                                        // Save the map to Firebase
                                        databaseReference.child(nameitem).setValue(itemData, new DatabaseReference.CompletionListener() {
                                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                                uploadProgressBar.setVisibility(View.GONE);
                                                if (databaseError != null) {
                                                    // Data could not be saved
                                                    Toast.makeText(addItems.this, "Failed to save data: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                                                } else {
                                                    // Data saved successfully
                                                    Toast.makeText(addItems.this, "Data saved successfully", Toast.LENGTH_SHORT).show();
                                                }
                                                clearFields();
                                            }
                                        });
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                uploadProgressBar.setVisibility(View.GONE);
                                Toast.makeText(addItems.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            } catch (NumberFormatException e) {
                // Quantity is not a valid integer
                Toast.makeText(addItems.this, "Quantity must be an integer", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Show a message if any field is empty
            Toast.makeText(addItems.this, "Please fill out all fields and select an image", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to open the file chooser for selecting an image
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Method to remove the selected image
    private void removeImage() {
        imageUri = null; // Reset the imageUri
        itemImageView.setImageResource(R.drawable.addimage); // Reset image view to a placeholder
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                itemImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void clearFields() {
        itemName.setText("");
        itemBrand.setText("");
        itemQuantity.setText("");
        itemImageView.setImageResource(R.drawable.addimage); // Reset image view to a placeholder
    }

    // Method to check if the item already exists in the database
    private void checkItemExists() {
        String nameitem = itemName.getText().toString().trim();

        if (!nameitem.isEmpty()) {
            // Query Firebase to check if the item exists
            databaseReference.child(nameitem).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // If item exists, retrieve data and auto-fill fields
                        Map<String, Object> itemData = (Map<String, Object>) dataSnapshot.getValue();
                        itemBrand.setText((String) itemData.get("brandName"));
                        itemQuantity.setText(String.valueOf(itemData.get("quantity")));
                        // Load the image from Firebase Storage using the image URL
                        String imageUrl = (String) itemData.get("imageUrl");
                        Glide.with(addItems.this).load(imageUrl).into(itemImageView);
                    } else {
                        // If item does not exist, clear fields
                        itemBrand.setText("");
                        itemQuantity.setText("");
                        itemImageView.setImageResource(R.drawable.addimage);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(addItems.this, "Failed to check item: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    // Method to fill item details if item already exists
    private void fillItemDetails(String selectedName) {
        databaseReference.child(selectedName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Object> itemData = (Map<String, Object>) dataSnapshot.getValue();
                    itemBrand.setText((String) itemData.get("brandName"));
                    itemQuantity.setText(String.valueOf(itemData.get("quantity")));
                    String imageUrl = (String) itemData.get("imageUrl");
                    Glide.with(addItems.this).load(imageUrl).into(itemImageView);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(addItems.this, "Failed to load item details: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    public void onBackPressed() {
        super.onBackPressed(); // Call the super method to handle the back button press

        // Navigate to the dashboard
        Intent intent = new Intent(addItems.this, MainActivity.class);
        startActivity(intent);
        finish(); // Optional: Call finish() if you don't want to keep the current activity in the back stack
    }
}


package com.example.uoclabs;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class splash extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 2000; // Duration for splash screen in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // Handler to start the MainActivity after a delay
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                // Start MainActivity
                Intent mainIntent = new Intent(splash.this, MainActivity.class);
                startActivity(mainIntent);
                // Finish the splash activity
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }





}
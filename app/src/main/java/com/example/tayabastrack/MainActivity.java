package com.example.tayabastrack;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Make sure this matches your XML file name

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                // Create an Intent that will start the next activity
                Intent mainIntent = new Intent(MainActivity.this, Login.class); // Replace NextActivity.class with your actual next activity
                startActivity(mainIntent);
                finish(); // Close the splash activity so the user won't come back to it
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}
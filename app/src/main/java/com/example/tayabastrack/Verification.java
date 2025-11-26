package com.example.tayabastrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Verification extends AppCompatActivity {

    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);
        android.view.Window window = getWindow();
        window.setStatusBarColor(android.graphics.Color.parseColor("#ffffff"));
        androidx.core.view.WindowInsetsControllerCompat controller =
                new androidx.core.view.WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);

        // Initialize views
        backButton = findViewById(R.id.backButton);

        // Handle Back button - go to Login
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(Verification.this, Login.class);
            startActivity(intent);
            finish();
        });
    }
}
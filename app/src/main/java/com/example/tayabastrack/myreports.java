package com.example.tayabastrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class myreports extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_myreports);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ Find the tab TextViews
        TextView ongoingTab = findViewById(R.id.ongoing);
        TextView completedTab = findViewById(R.id.completed);

        // ✅ Click listener for ON-GOING
        ongoingTab.setOnClickListener(v -> {
            Intent intent = new Intent(myreports.this, ongoing.class);
            startActivity(intent);
        });

        // ✅ Click listener for COMPLETED
        completedTab.setOnClickListener(v -> {
            Intent intent = new Intent(myreports.this, completed.class);
            startActivity(intent);
        });

        // ✅ Back Button (ImageButton)
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(myreports.this, dashboard.class);
            startActivity(intent);
            finish(); // optional, so this activity won't stay in the back stack
        });
    }
}

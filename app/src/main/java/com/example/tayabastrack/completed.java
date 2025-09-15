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

public class completed extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // ✅ Correct layout
        setContentView(R.layout.activity_completed);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ Find the tab TextViews
        TextView pendingTab = findViewById(R.id.pending);
        TextView ongoingTab = findViewById(R.id.ongoing);

        // ✅ Click listener for PENDING
        pendingTab.setOnClickListener(v -> {
            Intent intent = new Intent(completed.this, myreports.class);
            startActivity(intent);
        });

        // ✅ Click listener for ON-GOING
        ongoingTab.setOnClickListener(v -> {
            Intent intent = new Intent(completed.this, ongoing.class);
            startActivity(intent);
        });

        // ✅ Back Button (ImageButton)
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(completed.this, dashboard.class);
            startActivity(intent);
            finish(); // optional, so this activity won't remain in the back stack
        });
    }
}

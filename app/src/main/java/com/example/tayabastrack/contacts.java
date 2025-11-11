package com.example.tayabastrack;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class contacts extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contacts);

        // Set status bar color to match layout background and use dark icons for contrast
        android.view.Window window = getWindow();
        window.setStatusBarColor(android.graphics.Color.parseColor("#ffffff"));
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);



        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            // Apply system bars padding (status bar, navigation bar)
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    Math.max(systemBars.bottom, ime.bottom)
            );

            return WindowInsetsCompat.CONSUMED;
        });

        // Bottom Navigation Buttons
        ImageButton btnNavHome = findViewById(R.id.nav_home);
        ImageButton btnNavContacts = findViewById(R.id.nav_contacts);
        ImageButton btnNavSubmit = findViewById(R.id.nav_submit);
        ImageButton btnNavHistory = findViewById(R.id.nav_history);
        ImageButton btnNavProfile = findViewById(R.id.nav_profile);

        // Set click listeners for navigation
        btnNavHome.setOnClickListener(v -> {
            startActivity(new Intent(this, dashboard.class));
            finish();
        });

        btnNavContacts.setOnClickListener(v -> {
            // Already on contacts page, optionally show a toast or do nothing
            Toast.makeText(this, "Already on Contacts", Toast.LENGTH_SHORT).show();
        });

        btnNavSubmit.setOnClickListener(v -> {
            startActivity(new Intent(this, submitreport.class));
            finish();
        });

        btnNavHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, myreports.class));
            finish();
        });

        btnNavProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, myaccount.class));
            finish();
        });
    }

    // Back button click handler
    public void onBackClick(View view) {
        finish();
    }

    // Contact card click handlers
    public void onContact1Click(View view) {
        copyToClipboard("CityEngineeringOffice.tayabas@yahoo.com", "Email");
    }

    public void onContact2Click(View view) {
        copyToClipboard("(042 911-97 46)", "Telephone Number");
    }

    public void onContact3Click(View view) {
        copyToClipboard("09065429497", "Phone Number");
    }

    public void onContact4Click(View view) {
        copyToClipboard("09236317022", "Phone Number");
    }

    public void onContact5Click(View view) {
        copyToClipboard("09632728778", "Phone Number");
    }

    // Helper method to copy text to clipboard
    private void copyToClipboard(String text, String label) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, label + " copied!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(contacts.this, dashboard.class));
        finish();
    }
}
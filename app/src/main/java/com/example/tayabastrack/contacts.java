package com.example.tayabastrack;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class contacts extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contacts);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Back button click handler
    public void onBackClick(View view) {
        finish();
    }

    // Contact card click handlers
    public void onContact1Click(View view) {
        copyNumber("09065429497");
    }

    public void onContact2Click(View view) {
        copyNumber("09065429497");
    }

    public void onContact3Click(View view) {
        copyNumber("09065429497");
    }

    public void onContact4Click(View view) {
        copyNumber("09065429497");
    }

    public void onContact5Click(View view) {
        copyNumber("09065429497");
    }

    public void onContact6Click(View view) {
        copyNumber("09065429497");
    }

    public void onContact7Click(View view) {
        copyNumber("09065429497");
    }

    public void onContact8Click(View view) {
        copyNumber("09065429497");
    }

    // Helper method to copy number to clipboard
    private void copyNumber(String number) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Phone Number", number);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Number copied!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(contacts.this, dashboard.class));
        finish();
    }
}
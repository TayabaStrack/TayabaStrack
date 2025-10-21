package com.example.tayabastrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class dashboard extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Request notification permission
        NotificationHelper.requestNotificationPermission(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize buttons
        ImageButton btnSubmitReport = findViewById(R.id.submit_report);
        ImageButton btnMyReports    = findViewById(R.id.my_reports);
        ImageButton btnMyAccount    = findViewById(R.id.my_account);
        ImageButton btnUserManual   = findViewById(R.id.user_manual);
        ImageButton btnLogout       = findViewById(R.id.logout_button);
        ImageButton btnContacts     = findViewById(R.id.contacts);

        // Set click listeners for navigation
        btnSubmitReport.setOnClickListener(v -> startActivity(new Intent(this, submitreport.class)));
        btnMyReports.setOnClickListener(v -> startActivity(new Intent(this, myreports.class)));
        btnMyAccount.setOnClickListener(v -> startActivity(new Intent(this, myaccount.class)));
        btnContacts.setOnClickListener(v -> startActivity(new Intent(this, contacts.class)));
        btnUserManual.setOnClickListener(v -> startActivity(new Intent(this, usermanual.class)));

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("Do you want to log out?")
                .setPositiveButton("Yes", (d, w) -> {
                    // Sign out from Firebase
                    mAuth.signOut();

                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                    // Navigate to Login and clear activity stack
                    Intent intent = new Intent(this, Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            // Set background to #004AAD and text/buttons to white
            dialog.getWindow().setBackgroundDrawableResource(R.color.blue_004aad);

            int white = ContextCompat.getColor(this, android.R.color.white);

            TextView message = dialog.findViewById(android.R.id.message);
            if (message != null) message.setTextColor(white);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(white);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(white);
        });

        dialog.show();
    }

    @Override
    public void onBackPressed() {
        // Show exit app dialog instead of going back to previous screen
        new AlertDialog.Builder(this)
                .setMessage("Do you want to exit the app?")
                .setPositiveButton("Yes", (d, w) -> {
                    finishAffinity(); // Close all activities and exit app
                })
                .setNegativeButton("No", (d, w) -> d.dismiss())
                .create()
                .show();
    }
}
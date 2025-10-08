package com.example.tayabastrack;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class myaccount extends AppCompatActivity {

    private TextView fullNameText, positionText;
    private EditText etFirstName, etMiddleName, etSurname, etPosition, etPhoneNumber, etBarangay;
    private Button btnLogout;
    private ImageButton backButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myaccount);

        // Handle system window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            loadUserData();
        } else {
            // User not logged in, redirect to login
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(myaccount.this, Login.class));
            finish();
            return;
        }

        // Setup event listeners
        setupEventListeners();
    }

    private void initializeViews() {
        fullNameText = findViewById(R.id.fullNameText);
        positionText = findViewById(R.id.positionText);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etSurname = findViewById(R.id.etSurname);
        etPosition = findViewById(R.id.etPosition);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etBarangay = findViewById(R.id.etBarangay);
        btnLogout = findViewById(R.id.btnLogout);
        backButton = findViewById(R.id.backButton);
    }

    private void loadUserData() {
        // Show loading state
        fullNameText.setText("Loading...");
        positionText.setText("");

        // Fetch user data from Firestore
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get user data
                        String firstName = documentSnapshot.getString("firstName");
                        String middleName = documentSnapshot.getString("middleName");
                        String surname = documentSnapshot.getString("surname");
                        String position = documentSnapshot.getString("position");
                        String phoneNumber = documentSnapshot.getString("phoneNumber");
                        String barangay = documentSnapshot.getString("barangay");
                        String fullName = documentSnapshot.getString("fullName");

                        // Update EditTexts
                        if (firstName != null) etFirstName.setText(firstName);
                        if (middleName != null) etMiddleName.setText(middleName);
                        if (surname != null) etSurname.setText(surname);
                        if (position != null) etPosition.setText(position);
                        if (phoneNumber != null) etPhoneNumber.setText(phoneNumber);
                        if (barangay != null) etBarangay.setText(barangay);

                        // Update header display
                        if (fullName != null && !fullName.isEmpty()) {
                            fullNameText.setText(fullName);
                        } else {
                            // Build full name from components
                            String displayName = buildFullName(firstName, middleName, surname);
                            fullNameText.setText(displayName.isEmpty() ? "No name available" : displayName);
                        }

                        if (position != null && !position.isEmpty()) {
                            positionText.setText(position);
                        } else {
                            positionText.setText("No position set");
                        }

                        Log.d("MyAccount", "User data loaded successfully");
                    } else {
                        fullNameText.setText("User data not found");
                        positionText.setText("");
                        Log.w("MyAccount", "User document does not exist");
                    }
                })
                .addOnFailureListener(e -> {
                    fullNameText.setText("Error loading data");
                    positionText.setText("");
                    Toast.makeText(this, "Failed to load user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e("MyAccount", "Error loading user data", e);
                });
    }

    private String buildFullName(String firstName, String middleName, String surname) {
        StringBuilder name = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            name.append(firstName);
        }
        if (middleName != null && !middleName.isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(middleName);
        }
        if (surname != null && !surname.isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(surname);
        }
        return name.toString();
    }

    private void setupEventListeners() {
        // Back button
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(myaccount.this, dashboard.class));
            finish();
        });

        // Logout button
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        // Sign out from Firebase
        mAuth.signOut();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Redirect to login
        Intent intent = new Intent(myaccount.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(myaccount.this, dashboard.class));
        finish();
    }
}
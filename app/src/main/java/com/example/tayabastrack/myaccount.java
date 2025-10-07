package com.example.tayabastrack;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class myaccount extends AppCompatActivity {

    private TextView fullNameText, positionText;
    private Button btnEditProfile, btnDeleteAccount, btnLogout;
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
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
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
                        String fullName = documentSnapshot.getString("fullName");
                        String position = documentSnapshot.getString("position");

                        // Update UI
                        if (fullName != null && !fullName.isEmpty()) {
                            fullNameText.setText(fullName);
                        } else {
                            fullNameText.setText("No name available");
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

    private void setupEventListeners() {
        // Back button
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(myaccount.this, dashboard.class));
            finish();
        });

        // Edit Profile button
        btnEditProfile.setOnClickListener(v -> {
            // TODO: Navigate to Edit Profile activity
            Toast.makeText(this, "Edit Profile - Coming Soon", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(myaccount.this, EditProfileActivity.class);
            // startActivity(intent);
        });

        // Delete Account button
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());

        // Logout button
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently deleted.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setMessage("Deleting account...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Delete user data from Firestore first
        db.collection("users")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("MyAccount", "User document deleted from Firestore");

                    // Delete all user reports
                    db.collection("users")
                            .document(userId)
                            .collection("reports")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                    document.getReference().delete();
                                }
                                Log.d("MyAccount", "User reports deleted");

                                // Finally, delete the Firebase Auth account
                                currentUser.delete()
                                        .addOnCompleteListener(task -> {
                                            progressDialog.dismiss();
                                            if (task.isSuccessful()) {
                                                Toast.makeText(this, "Account deleted successfully",
                                                        Toast.LENGTH_SHORT).show();
                                                // Redirect to login
                                                startActivity(new Intent(myaccount.this, Login.class));
                                                finish();
                                            } else {
                                                Toast.makeText(this, "Failed to delete account: " +
                                                                task.getException().getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                                Log.e("MyAccount", "Error deleting auth account", task.getException());
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "Failed to delete user reports: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                Log.e("MyAccount", "Error deleting reports", e);
                            });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to delete user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e("MyAccount", "Error deleting user document", e);
                });
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
package com.example.tayabastrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class NewPassword extends AppCompatActivity {

    private TextInputEditText newPasswordInput, confirmPasswordInput;
    private Button resetPasswordButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userEmail;

    private static final int MIN_PASSWORD_LENGTH = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);
        android.view.Window window = getWindow();
        window.setStatusBarColor(android.graphics.Color.parseColor("#ffffff"));
        androidx.core.view.WindowInsetsControllerCompat controller =
                new androidx.core.view.WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get email from intent
        Intent intent = getIntent();
        userEmail = intent.getStringExtra("email");

        if (userEmail == null) {
            Toast.makeText(this, "Error: No email provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize views
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);

        // Handle Reset Password button
        resetPasswordButton.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String newPassword = newPasswordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(newPassword)) {
            newPasswordInput.setError("Password is required");
            newPasswordInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError("Please confirm your password");
            confirmPasswordInput.requestFocus();
            return;
        }

        // Validate password length (minimum 6 characters)
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            newPasswordInput.setError("Password must be at least 6 characters");
            newPasswordInput.requestFocus();
            return;
        }

        // Check if passwords match
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            confirmPasswordInput.requestFocus();
            return;
        }

        // Disable button
        resetPasswordButton.setEnabled(false);
        resetPasswordButton.setText("Resetting...");

        // Update password
        updatePassword(newPassword);
    }

    private void updatePassword(String newPassword) {
        // Get user document from Firestore using email
        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String userId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        // Update password in Firestore
                        db.collection("users").document(userId)
                                .update("password", newPassword)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(NewPassword.this,
                                            "Password reset successfully!",
                                            Toast.LENGTH_LONG).show();

                                    // Navigate back to login
                                    Intent intent = new Intent(NewPassword.this, Login.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(NewPassword.this,
                                            "Error updating password: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    resetPasswordButton.setEnabled(true);
                                    resetPasswordButton.setText("Reset Password");
                                    Log.e("NewPassword", "Error updating password", e);
                                });
                    } else {
                        Toast.makeText(NewPassword.this,
                                "User not found",
                                Toast.LENGTH_SHORT).show();
                        resetPasswordButton.setEnabled(true);
                        resetPasswordButton.setText("Reset Password");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NewPassword.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    resetPasswordButton.setEnabled(true);
                    resetPasswordButton.setText("Reset Password");
                    Log.e("NewPassword", "Error finding user", e);
                });
    }
}
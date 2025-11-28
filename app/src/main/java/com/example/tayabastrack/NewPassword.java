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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class NewPassword extends AppCompatActivity {

    private TextInputEditText newPasswordInput, confirmPasswordInput;
    private Button resetPasswordButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userEmail;
    private String userId;

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final String TAG = "NewPassword";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);

        android.view.Window window = getWindow();
        window.setStatusBarColor(android.graphics.Color.parseColor("#ffffff"));
        androidx.core.view.WindowInsetsControllerCompat controller =
                new androidx.core.view.WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        userEmail = intent.getStringExtra("email");

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Error: No email provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);

        resetPasswordButton.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String newPassword = newPasswordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

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

        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            newPasswordInput.setError("Password must be at least 6 characters");
            newPasswordInput.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return;
        }

        resetPasswordButton.setEnabled(false);
        resetPasswordButton.setText("Resetting Password...");

        getUserAndUpdatePassword(newPassword);
    }

    private void getUserAndUpdatePassword(String newPassword) {
        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        userId = document.getId();
                        String oldPassword = document.getString("password");

                        Log.d(TAG, "User found: " + userId);

                        // Sign in with old password to authenticate
                        signInAndUpdate(oldPassword, newPassword);

                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        resetButton();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error getting user", e);
                    resetButton();
                });
    }

    private void signInAndUpdate(String oldPassword, String newPassword) {
        String authPassword = (oldPassword != null && !oldPassword.isEmpty()) ? oldPassword : "TempPass123456";

        mAuth.signInWithEmailAndPassword(userEmail, authPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Signed in successfully");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // Update Firebase Auth password
                            updateAuthPassword(user, newPassword);
                        } else {
                            resetButton();
                        }

                    } else {
                        Log.e(TAG, "Sign-in failed, creating account", task.getException());
                        // If sign-in fails, create account with new password
                        createAccountWithNewPassword(newPassword);
                    }
                });
    }

    private void createAccountWithNewPassword(String newPassword) {
        mAuth.createUserWithEmailAndPassword(userEmail, newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Account created with new password");

                        // Update Firestore
                        updateFirestorePassword(newPassword);

                    } else {
                        Log.e(TAG, "Failed to create account", task.getException());
                        Toast.makeText(this,
                                "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        resetButton();
                    }
                });
    }

    private void updateAuthPassword(FirebaseUser user, String newPassword) {
        user.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth password updated");

                        // Update Firestore
                        updateFirestorePassword(newPassword);

                    } else {
                        Log.e(TAG, "Failed to update auth password", task.getException());
                        Toast.makeText(this,
                                "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        resetButton();
                    }
                });
    }

    private void updateFirestorePassword(String newPassword) {
        Log.d(TAG, "Updating Firestore password");

        db.collection("users").document(userId)
                .update("password", newPassword)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Password updated successfully!");

                    // Mark reset code as used
                    db.collection("password_reset_codes").document(userEmail)
                            .update("used", true)
                            .addOnSuccessListener(v -> Log.d(TAG, "Reset code marked as used"))
                            .addOnFailureListener(e -> Log.e(TAG, "Error marking code", e));

                    // Sign out
                    if (mAuth.getCurrentUser() != null) {
                        mAuth.signOut();
                    }

                    Toast.makeText(this,
                            "Password reset successfully! Please login with your new password.",
                            Toast.LENGTH_LONG).show();

                    // Go to login
                    Intent intent = new Intent(NewPassword.this, Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update Firestore password", e);
                    Toast.makeText(this,
                            "Error updating password: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void resetButton() {
        resetPasswordButton.setEnabled(true);
        resetPasswordButton.setText("Reset Password");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
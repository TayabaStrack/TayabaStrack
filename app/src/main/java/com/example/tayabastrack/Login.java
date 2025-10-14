package com.example.tayabastrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText;
    private Button signInButton;
    private TextView forgotPasswordText, registerButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        signInButton = findViewById(R.id.signInButton);
        forgotPasswordText = findViewById(R.id.forgotPassword);
        registerButton = findViewById(R.id.registerButton);

        // Handle Login button
        signInButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(Login.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        // Handle Forgot Password
        forgotPasswordText.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, forgotpassword.class);
            startActivity(intent);
        });

        // Handle Register
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, register.class);
            startActivity(intent);
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_SHORT).show();

                        // Get the current user ID
                        String userId = mAuth.getCurrentUser().getUid();

                        // Check user status from Firestore
                        checkUserStatus(userId);
                    } else {
                        Toast.makeText(Login.this, "Login Failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserStatus(String userId) {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");

                        if ("pending".equals(status)) {
                            // User is pending - go to verification screen
                            Toast.makeText(Login.this, "Your account is pending verification. Please check your email.",
                                    Toast.LENGTH_LONG).show();

                            String email = documentSnapshot.getString("email");
                            Intent intent = new Intent(Login.this, Verification.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            finish();
                        } else if ("approved".equals(status) || "active".equals(status)) {
                            // User is approved or active - go to dashboard
                            Intent intent = new Intent(Login.this, dashboard.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Any other status
                            Toast.makeText(Login.this, "Account status: " + status, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(Login.this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Login.this, "Error checking user status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
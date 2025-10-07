package com.example.tayabastrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText;
    private Button signInButton;
    private TextView forgotPasswordText, registerButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

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

                        // Go to Dashboard
                        Intent intent = new Intent(Login.this, dashboard.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(Login.this, "Login Failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

package com.example.tayabastrack;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class Login extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText;
    private Button signInButton;
    private TextView forgotPasswordText, registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signInButton = findViewById(R.id.signInButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        registerButton = findViewById(R.id.registerButton);

        // Underline Register text
        registerButton.setPaintFlags(registerButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Handle Login button
        signInButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(Login.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            } else {
                // TODO: Replace with real authentication
                if (email.equals("andeng@gmail.com") && password.equals("123456")) {
                    Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_SHORT).show();

                    // ✅ Go to Dashboard
                    Intent intent = new Intent(Login.this, dashboard.class);
                    startActivity(intent);
                    finish(); // prevent going back to login
                } else {
                    Toast.makeText(Login.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                }
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
}

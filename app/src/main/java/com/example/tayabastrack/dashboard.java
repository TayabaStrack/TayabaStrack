package com.example.tayabastrack;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class dashboard extends AppCompatActivity {

    private static final String TAG = "Dashboard";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private LinearLayout reportsContainer;
    private TextView greetingText;
    private TextView roleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Request notification permission
        NotificationHelper.requestNotificationPermission(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get reference to UI elements
        reportsContainer = findViewById(R.id.reports_container);
        greetingText = findViewById(R.id.greeting_text);
        roleText = findViewById(R.id.role_text);

        // Load user info
        loadUserInfo();

        // Load completed reports
        loadCompletedReports();

        // Initialize logout button from header
        ImageButton btnLogout = findViewById(R.id.logout_button);
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        // Initialize Bottom Navigation ImageButtons
        ImageButton btnNavHome = findViewById(R.id.nav_home);
        ImageButton btnNavContacts = findViewById(R.id.nav_contacts);
        ImageButton btnNavSubmit = findViewById(R.id.nav_submit);
        ImageButton btnNavHistory = findViewById(R.id.nav_history);
        ImageButton btnNavProfile = findViewById(R.id.nav_profile);

        // Set click listeners for navigation
        btnNavHome.setOnClickListener(v -> {
            // Already on home, do nothing
        });

        btnNavContacts.setOnClickListener(v -> {
            startActivity(new Intent(this, contacts.class));
        });

        btnNavSubmit.setOnClickListener(v -> {
            startActivity(new Intent(this, submitreport.class));
        });

        btnNavHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, myreports.class));
        });

        btnNavProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, myaccount.class));
        });
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.d(TAG, "No user logged in");
            greetingText.setText("Hi, Guest!");
            roleText.setText("Not logged in");
            return;
        }

        Log.d(TAG, "Current user UID: " + currentUser.getUid());
        Log.d(TAG, "Current user email: " + currentUser.getEmail());

        // Fetch user data from Firestore
        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "Firestore query successful");
                    Log.d(TAG, "Document exists: " + documentSnapshot.exists());

                    if (documentSnapshot.exists()) {
                        // Log all data in the document
                        Log.d(TAG, "Document data: " + documentSnapshot.getData());

                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("surname");
                        String position = documentSnapshot.getString("position");

                        Log.d(TAG, "firstName: " + firstName);
                        Log.d(TAG, "surname: " + lastName);
                        Log.d(TAG, "position: " + position);

                        // Set greeting text
                        if (firstName != null && lastName != null) {
                            String greeting = "Hi, " + firstName + " " + lastName + "!";
                            greetingText.setText(greeting);
                            Log.d(TAG, "Set greeting to: " + greeting);
                        } else if (firstName != null) {
                            String greeting = "Hi, " + firstName + "!";
                            greetingText.setText(greeting);
                            Log.d(TAG, "Set greeting to: " + greeting);
                        } else {
                            greetingText.setText("Hi, User!");
                            Log.d(TAG, "firstName or lastName is null, using default");
                        }

                        // Set position text
                        if (position != null && !position.isEmpty()) {
                            roleText.setText(position);
                            Log.d(TAG, "Set position to: " + position);
                        } else {
                            roleText.setText("Position not set");
                            Log.d(TAG, "position is null or empty, using default");
                        }
                    } else {
                        Log.w(TAG, "Document does not exist for UID: " + currentUser.getUid());
                        greetingText.setText("Hi, User!");
                        roleText.setText("Position not set");
                        Toast.makeText(this, "User profile not found. Please complete your profile.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user info", e);
                    Toast.makeText(this, "Failed to load user info: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    greetingText.setText("Hi, User!");
                    roleText.setText("Position not set");
                });
    }

    private void loadCompletedReports() {
        Log.d(TAG, "Loading completed reports...");

        // Fetch latest completed reports from ALL USERS in Firestore
        db.collection("reports")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Reports query successful. Total documents: " + queryDocumentSnapshots.size());
                    reportsContainer.removeAllViews();

                    // Filter completed reports in memory
                    java.util.List<CardView> completedCards = new java.util.ArrayList<>();

                    for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        Object completedAt = document.get("completedAt");
                        Object completionDate = document.get("completionDate");

                        Log.d(TAG, "Report ID: " + document.getId() + ", Status: " + status);

                        // Only include if status is repaired/completed AND has a completion date
                        if (status != null && (completedAt != null || completionDate != null) &&
                                (status.equalsIgnoreCase("repaired") || status.equalsIgnoreCase("completed"))) {
                            completedCards.add(createReportCard(document.getData()));
                            if (completedCards.size() >= 2) break; // Only need 2
                        }
                    }

                    Log.d(TAG, "Completed reports found: " + completedCards.size());

                    if (completedCards.isEmpty()) {
                        // Show empty state message
                        TextView emptyMessage = new TextView(this);
                        emptyMessage.setText("No completed reports yet");
                        emptyMessage.setTextSize(14);
                        emptyMessage.setTextColor(0xFF757575);
                        emptyMessage.setPadding(16, 32, 16, 32);
                        emptyMessage.setGravity(android.view.Gravity.CENTER);
                        reportsContainer.addView(emptyMessage);
                        return;
                    }

                    for (CardView card : completedCards) {
                        reportsContainer.addView(card);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reports", e);
                    Toast.makeText(this, "Failed to load reports: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private CardView createReportCard(java.util.Map<String, Object> reportData) {
        CardView cardView = new CardView(this);
        cardView.setRadius(dpToPx(16));
        cardView.setCardElevation(dpToPx(2));
        cardView.setUseCompatPadding(true);
        cardView.setCardBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(12));
        cardView.setLayoutParams(cardParams);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        cardView.addView(cardContent);

        // Image Section (if available)
        if (reportData.get("imageUrl") != null && !reportData.get("imageUrl").toString().isEmpty()) {
            ImageView reportImage = new ImageView(this);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(200)
            );
            imageParams.setMargins(0, 0, 0, dpToPx(12));
            reportImage.setLayoutParams(imageParams);
            reportImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            reportImage.setBackgroundColor(0xFFF0F0F0);

            // Load image from URL
            loadImageFromUrl(reportData.get("imageUrl").toString(), reportImage);
            cardContent.addView(reportImage);
        }

        // Description Section
        TextView descValue = new TextView(this);
        descValue.setText(reportData.get("description") != null ?
                reportData.get("description").toString() : "No description");
        descValue.setTextSize(18);
        descValue.setTypeface(null, android.graphics.Typeface.BOLD);
        descValue.setTextColor(0xFF000000);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, 0, 0, dpToPx(8));
        descValue.setLayoutParams(descParams);
        cardContent.addView(descValue);

        // Barangay Section
        TextView barangayValue = new TextView(this);
        String barangay = reportData.get("userBarangay") != null ?
                reportData.get("userBarangay").toString() : "No barangay";
        barangayValue.setText(barangay);
        barangayValue.setTextSize(16);
        barangayValue.setTextColor(0xFF666666);
        LinearLayout.LayoutParams barangayParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        barangayParams.setMargins(0, 0, 0, dpToPx(8));
        barangayValue.setLayoutParams(barangayParams);
        cardContent.addView(barangayValue);

        // Repaired Date Section
        TextView dateValue = new TextView(this);
        String dateText = formatCompletedAt(reportData);
        dateValue.setText(dateText);
        dateValue.setTextSize(14);
        dateValue.setTextColor(0xFF999999);
        cardContent.addView(dateValue);

        return cardView;
    }

    // Helper method to format the completion date (checks both completedAt and completionDate)
    private String formatCompletedAt(java.util.Map<String, Object> reportData) {
        try {
            // Try completedAt first
            Object completedAtObj = reportData.get("completedAt");

            if (completedAtObj instanceof com.google.firebase.Timestamp) {
                com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) completedAtObj;
                java.util.Date date = timestamp.toDate();

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                        "MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
                return sdf.format(date);
            } else if (completedAtObj instanceof String) {
                return completedAtObj.toString();
            }

            // If completedAt doesn't exist, try completionDate
            Object completionDateObj = reportData.get("completionDate");

            if (completionDateObj instanceof com.google.firebase.Timestamp) {
                com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) completionDateObj;
                java.util.Date date = timestamp.toDate();

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                        "MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
                return sdf.format(date);
            } else if (completionDateObj instanceof String) {
                return completionDateObj.toString();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error formatting completion date", e);
        }

        return "Date not available";
    }

    // Helper method to load image from URL asynchronously
    private void loadImageFromUrl(String imageUrl, ImageView imageView) {
        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                // Update UI on main thread
                runOnUiThread(() -> {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading image from URL: " + imageUrl, e);
                runOnUiThread(() -> {
                    // Show placeholder or error image
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                });
            }
        }).start();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called - reloading data");
        // Reload user info and reports when returning to dashboard
        loadUserInfo();
        loadCompletedReports();
    }

    private void showLogoutDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("Do you want to log out?")
                .setPositiveButton("Yes", (d, w) -> {
                    // Sign out from Firebase
                    mAuth.signOut();
                    Log.d(TAG, "User logged out");

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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("Do you want to exit the app?")
                .setPositiveButton("Yes", (d, w) -> {
                    finishAffinity(); // Close all activities and exit app
                })
                .setNegativeButton("No", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            // Apply same styling as logout dialog
            dialog.getWindow().setBackgroundDrawableResource(R.color.blue_004aad);

            int white = ContextCompat.getColor(this, android.R.color.white);

            TextView message = dialog.findViewById(android.R.id.message);
            if (message != null) message.setTextColor(white);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(white);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(white);
        });

        dialog.show();
    }
}
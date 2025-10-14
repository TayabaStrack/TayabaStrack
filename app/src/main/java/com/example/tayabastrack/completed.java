package com.example.tayabastrack;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.Timestamp;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class completed extends AppCompatActivity {

    private LinearLayout contentLayout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_completed);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize views
        contentLayout = findViewById(R.id.contentFrame);

        // Find the tab TextViews
        TextView pendingTab = findViewById(R.id.tabPending);
        TextView ongoingTab = findViewById(R.id.ongoing);
        TextView declinedTab = findViewById(R.id.declined);

        // Load completed reports
        loadCompletedReports();

        // Click listener for PENDING
        pendingTab.setOnClickListener(v -> {
            Intent intent = new Intent(completed.this, myreports.class);
            startActivity(intent);
            finish();
        });

        // Click listener for ON-GOING
        ongoingTab.setOnClickListener(v -> {
            Intent intent = new Intent(completed.this, ongoing.class);
            startActivity(intent);
            finish();
        });

        // Click listener for DECLINED
        declinedTab.setOnClickListener(v -> {
            Intent intent = new Intent(completed.this, declined.class);
            startActivity(intent);
            finish();
        });

        // Back Button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(completed.this, dashboard.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadCompletedReports() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Fetch completed reports from Firestore
        // Query from root reports collection where userId matches and status is "completed"
        db.collection("reports")
                .whereEqualTo("userId", userId)
                .whereIn("status", java.util.Arrays.asList("repaired", "Repaired","completed","Completed"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    contentLayout.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        // Show empty state message
                        TextView emptyMessage = new TextView(this);
                        emptyMessage.setText("No completed reports");
                        emptyMessage.setTextSize(16);
                        emptyMessage.setPadding(16, 16, 16, 16);
                        emptyMessage.setGravity(android.view.Gravity.CENTER);
                        contentLayout.addView(emptyMessage);
                        return;
                    }

                    // Create report cards for each completed report
                    queryDocumentSnapshots.getDocuments().forEach(document -> {
                        CardView reportCard = createReportCard(document.getData());
                        contentLayout.addView(reportCard);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load reports: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private CardView createReportCard(java.util.Map<String, Object> reportData) {
        CardView cardView = new CardView(this);
        cardView.setRadius(12);
        cardView.setCardElevation(8);
        cardView.setMaxCardElevation(12);
        cardView.setUseCompatPadding(true);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(8, 8, 8, 16);
        cardView.setLayoutParams(cardParams);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setPadding(16, 16, 16, 16);
        cardContent.setBackgroundColor(0xFFFFFFFF);
        cardView.addView(cardContent);

        // Left side - All text information
        LinearLayout leftContainer = new LinearLayout(this);
        leftContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.65f
        );
        leftContainer.setLayoutParams(leftParams);
        cardContent.addView(leftContainer);

        // Description Section
        addInfoSection(leftContainer, "Description:",
                reportData.get("description") != null ? reportData.get("description").toString() : "No description",
                true);

        // Barangay Section
        addInfoSection(leftContainer, "Barangay:",
                reportData.get("barangay") != null ? reportData.get("barangay").toString() : "No barangay",
                false);

        // Inspection Section
        String inspectionDate = formatTimestamp(reportData.get("inspectionDate"));
        addInfoSection(leftContainer, "Inspection:", inspectionDate, false);

        // To Repair In Section
        addInfoSection(leftContainer, "To Repair In:",
                reportData.get("toRepairIn") != null ? reportData.get("toRepairIn").toString() : "N/A",
                false);

        // Repaired Section
        String completionDate = formatTimestamp(reportData.get("completionDate"));
        addInfoSection(leftContainer, "Repaired:", completionDate, false);

        // Right side - Image Section
        LinearLayout imageContainer = new LinearLayout(this);
        imageContainer.setOrientation(LinearLayout.VERTICAL);
        imageContainer.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams imageContainerParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0.35f
        );
        imageContainer.setLayoutParams(imageContainerParams);
        cardContent.addView(imageContainer);

        ImageView reportImage = new ImageView(this);
        reportImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        reportImage.setBackgroundColor(0xFFC0C0C0);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(300, 300);
        reportImage.setLayoutParams(imageParams);

        // Load image from Firebase Storage URL
        if (reportData.containsKey("imageUrl") && reportData.get("imageUrl") != null) {
            String imageUrl = reportData.get("imageUrl").toString();
            loadImageFromUrl(imageUrl, reportImage);
        } else {
            // Try to load from old Blob format (for backward compatibility)
            if (reportData.containsKey("incidentImage")) {
                try {
                    com.google.firebase.firestore.Blob imageBlob =
                            (com.google.firebase.firestore.Blob) reportData.get("incidentImage");
                    byte[] imageBytes = imageBlob.toBytes();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    if (bitmap != null) {
                        reportImage.setImageBitmap(bitmap);
                    }
                } catch (Exception e) {
                    android.util.Log.e("Completed", "Failed to load image from Blob", e);
                }
            }
        }

        imageContainer.addView(reportImage);

        return cardView;
    }

    private void addInfoSection(LinearLayout container, String label, String value, boolean isFirst) {
        // Label
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(15);
        labelView.setTypeface(null, android.graphics.Typeface.BOLD);
        labelView.setTextColor(0xFF004AAD);

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (!isFirst) {
            labelParams.setMargins(0, 12, 0, 0);
        }
        labelView.setLayoutParams(labelParams);
        container.addView(labelView);

        // Value
        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(22);
        valueView.setTypeface(null, android.graphics.Typeface.BOLD);
        valueView.setTextColor(0xFF333333);

        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        valueParams.setMargins(0, 4, 0, 0);
        valueView.setLayoutParams(valueParams);
        container.addView(valueView);
    }

    private String formatTimestamp(Object timestampObj) {
        if (timestampObj == null) {
            return "N/A";
        }

        try {
            Date date;

            // Handle Firebase Timestamp
            if (timestampObj instanceof Timestamp) {
                Timestamp timestamp = (Timestamp) timestampObj;
                date = timestamp.toDate();
            }
            // Handle com.google.firebase.Timestamp (alternative import)
            else if (timestampObj instanceof com.google.firebase.Timestamp) {
                com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) timestampObj;
                date = timestamp.toDate();
            }
            // Handle Date object
            else if (timestampObj instanceof Date) {
                date = (Date) timestampObj;
            }
            // Handle String (already formatted)
            else if (timestampObj instanceof String) {
                return timestampObj.toString();
            }
            // Handle Long (milliseconds)
            else if (timestampObj instanceof Long) {
                date = new Date((Long) timestampObj);
            }
            else {
                return "N/A";
            }

            // Format: January 1, 2025
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
            return dateFormat.format(date);

        } catch (Exception e) {
            android.util.Log.e("Completed", "Error formatting timestamp", e);
            return "N/A";
        }
    }

    private void loadImageFromUrl(String imageUrl, ImageView imageView) {
        // Load image in background thread
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
                android.util.Log.e("Completed", "Failed to load image from URL: " + imageUrl, e);
                runOnUiThread(() -> {
                    // Keep the gray placeholder background
                });
            }
        }).start();
    }
}
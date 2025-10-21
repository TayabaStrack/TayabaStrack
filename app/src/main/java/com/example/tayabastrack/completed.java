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

        // Inspection Section (Date Range)
        if ((reportData.containsKey("startDate") && reportData.get("startDate") != null) ||
                (reportData.containsKey("endDate") && reportData.get("endDate") != null)) {

            TextView inspectionLabel = new TextView(this);
            inspectionLabel.setText("Inspection:");
            inspectionLabel.setTextSize(15);
            inspectionLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            inspectionLabel.setTextColor(0xFF004AAD);
            LinearLayout.LayoutParams inspectionLabelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            inspectionLabelParams.setMargins(0, 12, 0, 4);
            inspectionLabel.setLayoutParams(inspectionLabelParams);
            leftContainer.addView(inspectionLabel);

            TextView inspectionText = new TextView(this);
            String startDateString = formatDateOnly(reportData.get("startDate"));
            String endDateString = formatDateOnly(reportData.get("endDate"));

            String dateRangeText = startDateString + " - " + endDateString;

            inspectionText.setText(dateRangeText);
            inspectionText.setTextSize(14);
            inspectionText.setTypeface(null, android.graphics.Typeface.BOLD);
            inspectionText.setTextColor(0xFF333333);
            LinearLayout.LayoutParams inspectionParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            inspectionParams.setMargins(0, 0, 0, 12);
            inspectionText.setLayoutParams(inspectionParams);
            leftContainer.addView(inspectionText);
        }

        // Repaired Date
        if (reportData.containsKey("completionDate") && reportData.get("completionDate") != null) {
            TextView repairedLabel = new TextView(this);
            repairedLabel.setText("Repaired Date:");
            repairedLabel.setTextSize(15);
            repairedLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            repairedLabel.setTextColor(0xFF004AAD);
            LinearLayout.LayoutParams repairedLabelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            repairedLabelParams.setMargins(0, 12, 0, 4);
            repairedLabel.setLayoutParams(repairedLabelParams);
            leftContainer.addView(repairedLabel);

            TextView repairedText = new TextView(this);
            Object completionDateObj = reportData.get("completionDate");

            String completionDateString = formatDateOnly(completionDateObj);

            repairedText.setText(completionDateString);
            repairedText.setTextSize(14);
            repairedText.setTypeface(null, android.graphics.Typeface.BOLD);
            repairedText.setTextColor(0xFF333333);
            LinearLayout.LayoutParams repairedParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            repairedParams.setMargins(0, 0, 0, 12);
            repairedText.setLayoutParams(repairedParams);
            leftContainer.addView(repairedText);
        }

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

    private String formatDateOnly(Object dateObj) {
        if (dateObj == null) {
            return "N/A";
        }

        try {
            Date date = null;
            String dateStr = dateObj.toString();

            android.util.Log.d("Completed", "Date object type: " + dateObj.getClass().getName() + ", Value: " + dateStr);

            // Handle Firebase Timestamp
            if (dateObj instanceof com.google.firebase.Timestamp) {
                com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) dateObj;
                date = timestamp.toDate();
            }
            // Handle java.util.Date object
            else if (dateObj instanceof java.util.Date) {
                date = (java.util.Date) dateObj;
            }
            // Handle Long (milliseconds)
            else if (dateObj instanceof Long) {
                date = new Date((Long) dateObj);
            }
            // Handle String that might be a timestamp
            else if (dateObj instanceof String) {
                try {
                    // Try parsing as Long first
                    long timeInMillis = Long.parseLong(dateStr);
                    date = new Date(timeInMillis);
                } catch (NumberFormatException nfe) {
                    // If not a number, return as is
                    return dateStr;
                }
            }
            // Fallback: try to convert to long
            else {
                try {
                    long timeInMillis = Long.parseLong(dateStr);
                    date = new Date(timeInMillis);
                } catch (NumberFormatException nfe) {
                    return "N/A";
                }
            }

            if (date != null) {
                // Format: Jan 1, 2025 (date only, no time, short month)
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);
                String formatted = dateFormat.format(date);
                android.util.Log.d("Completed", "Formatted date: " + formatted);
                return formatted;
            }

            return "N/A";

        } catch (Exception e) {
            android.util.Log.e("Completed", "Error formatting date: " + dateObj.toString(), e);
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
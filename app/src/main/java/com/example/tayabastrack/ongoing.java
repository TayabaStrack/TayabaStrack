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
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.InputStream;
import com.google.firebase.Timestamp;
import java.net.HttpURLConnection;
import java.net.URL;

public class ongoing extends AppCompatActivity {

    private LinearLayout contentLayout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ongoing);

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
        TextView completedTab = findViewById(R.id.completed);
        TextView declinedTab = findViewById(R.id.declined);

        // Load ongoing reports
        loadOngoingReports();

        // Click listener for PENDING (go back to myreports)
        pendingTab.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, myreports.class);
            startActivity(intent);
            finish();
        });

        // Click listener for COMPLETED
        completedTab.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, completed.class);
            startActivity(intent);
            finish();
        });

        // Click listener for DECLINED
        declinedTab.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, declined.class);
            startActivity(intent);
            finish();
        });

        // Back Button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, dashboard.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadOngoingReports() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Fetch ongoing reports from Firestore
        // Query from root reports collection where userId matches and status is "ongoing"
        db.collection("reports")
                .whereEqualTo("userId", userId)
                .whereIn("status", java.util.Arrays.asList("ongoing", "Ongoing"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    contentLayout.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView emptyMessage = new TextView(this);
                        emptyMessage.setText("No ongoing reports");
                        emptyMessage.setTextSize(16);
                        emptyMessage.setPadding(16, 16, 16, 16);
                        emptyMessage.setGravity(android.view.Gravity.CENTER);
                        contentLayout.addView(emptyMessage);
                        return;
                    }

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
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(16, 16, 16, 16);
        cardContent.setBackgroundColor(0xFFFFFFFF);
        cardView.addView(cardContent);

        // Top section - Description and Barangay with Image
        LinearLayout topSection = new LinearLayout(this);
        topSection.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        topParams.setMargins(0, 0, 0, 16);
        topSection.setLayoutParams(topParams);
        cardContent.addView(topSection);

        // Left side - Description and Barangay Container
        LinearLayout leftContainer = new LinearLayout(this);
        leftContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.65f
        );
        leftContainer.setLayoutParams(leftParams);
        topSection.addView(leftContainer);

        // Description Label
        TextView descriptionLabel = new TextView(this);
        descriptionLabel.setText("Description:");
        descriptionLabel.setTextSize(15);
        descriptionLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        descriptionLabel.setTextColor(0xFF004AAD);
        leftContainer.addView(descriptionLabel);

        // Description Text
        TextView descriptionText = new TextView(this);
        String description = reportData.get("description") != null ?
                reportData.get("description").toString() : "No description";
        descriptionText.setText(description);
        descriptionText.setTextSize(22);
        descriptionText.setTypeface(null, android.graphics.Typeface.BOLD);
        descriptionText.setTextColor(0xFF333333);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, 4, 0, 12);
        descriptionText.setLayoutParams(descParams);
        leftContainer.addView(descriptionText);

        // Barangay Label
        TextView barangayLabel = new TextView(this);
        barangayLabel.setText("Barangay:");
        barangayLabel.setTextSize(15);
        barangayLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        barangayLabel.setTextColor(0xFF004AAD);
        leftContainer.addView(barangayLabel);

        // Barangay Text
        TextView barangayText = new TextView(this);
        String barangay = reportData.get("barangay") != null ?
                reportData.get("barangay").toString() : "No barangay";
        barangayText.setText(barangay);
        barangayText.setTextSize(22);
        barangayText.setTypeface(null, android.graphics.Typeface.BOLD);
        barangayText.setTextColor(0xFF333333);
        LinearLayout.LayoutParams barangayParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        barangayParams.setMargins(0, 4, 0, 0);
        barangayText.setLayoutParams(barangayParams);
        leftContainer.addView(barangayText);

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
        topSection.addView(imageContainer);

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
                    android.util.Log.e("Ongoing", "Failed to load image from Blob", e);
                }
            }
        }

        imageContainer.addView(reportImage);

        // Inspection Date Section
        if (reportData.containsKey("inspectionDate") && reportData.get("inspectionDate") != null) {
            TextView inspectionLabel = new TextView(this);
            inspectionLabel.setText("Inspection Date:");
            inspectionLabel.setTextSize(15);
            inspectionLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            inspectionLabel.setTextColor(0xFF004AAD);
            LinearLayout.LayoutParams inspectionLabelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            inspectionLabelParams.setMargins(0, 16, 0, 4);
            inspectionLabel.setLayoutParams(inspectionLabelParams);
            cardContent.addView(inspectionLabel);

            TextView inspectionText = new TextView(this);
            Object dateObj = reportData.get("inspectionDate");

            // Handle both Timestamp objects and String values
            String dateString;
            if (dateObj instanceof com.google.firebase.Timestamp) {
                com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) dateObj;
                dateString = new java.text.SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a", java.util.Locale.US)
                        .format(timestamp.toDate());
            } else {
                dateString = dateObj.toString();
            }

            inspectionText.setText(dateString);
            inspectionText.setTextSize(14);
            inspectionText.setTypeface(null, android.graphics.Typeface.BOLD);
            inspectionText.setTextColor(0xFF333333);
            LinearLayout.LayoutParams inspectionParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            inspectionParams.setMargins(0, 0, 0, 12);
            inspectionText.setLayoutParams(inspectionParams);
            cardContent.addView(inspectionText);
        }

        // To Repair In Section (Date Range: startDate - endDate)
        if ((reportData.containsKey("startDate") && reportData.get("startDate") != null) ||
                (reportData.containsKey("endDate") && reportData.get("endDate") != null)) {

            TextView repairLabel = new TextView(this);
            repairLabel.setText("To Repair In:");
            repairLabel.setTextSize(15);
            repairLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            repairLabel.setTextColor(0xFF004AAD);
            LinearLayout.LayoutParams repairLabelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            repairLabelParams.setMargins(0, 0, 0, 4);
            repairLabel.setLayoutParams(repairLabelParams);
            cardContent.addView(repairLabel);

            TextView repairText = new TextView(this);

            // Format start date
            String startDateString = "";
            if (reportData.containsKey("startDate") && reportData.get("startDate") != null) {
                Object startDateObj = reportData.get("startDate");
                if (startDateObj instanceof com.google.firebase.Timestamp) {
                    com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) startDateObj;
                    startDateString = new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
                            .format(timestamp.toDate());
                } else {
                    startDateString = startDateObj.toString();
                }
            }

            // Format end date
            String endDateString = "";
            if (reportData.containsKey("endDate") && reportData.get("endDate") != null) {
                Object endDateObj = reportData.get("endDate");
                if (endDateObj instanceof com.google.firebase.Timestamp) {
                    com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) endDateObj;
                    endDateString = new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
                            .format(timestamp.toDate());
                } else {
                    endDateString = endDateObj.toString();
                }
            }

            // Combine dates with hyphen
            String dateRange = "";
            if (!startDateString.isEmpty() && !endDateString.isEmpty()) {
                dateRange = startDateString + " - " + endDateString;
            } else if (!startDateString.isEmpty()) {
                dateRange = startDateString;
            } else if (!endDateString.isEmpty()) {
                dateRange = endDateString;
            }

            repairText.setText(dateRange);
            repairText.setTextSize(14);
            repairText.setTypeface(null, android.graphics.Typeface.BOLD);
            repairText.setTextColor(0xFF333333);
            LinearLayout.LayoutParams repairParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            repairParams.setMargins(0, 0, 0, 0);
            repairText.setLayoutParams(repairParams);
            cardContent.addView(repairText);
        }

        return cardView;
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
                android.util.Log.e("Ongoing", "Failed to load image from URL: " + imageUrl, e);
                runOnUiThread(() -> {
                    // Keep the gray placeholder background
                });
            }
        }).start();
    }
}
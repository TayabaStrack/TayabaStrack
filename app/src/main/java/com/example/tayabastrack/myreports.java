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
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class myreports extends AppCompatActivity {

    private LinearLayout contentLayout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_myreports);

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
        TextView ongoingTab = findViewById(R.id.ongoing);
        TextView completedTab = findViewById(R.id.completed);
        TextView declinedTab = findViewById(R.id.declined);

        // Load pending reports (default tab)
        loadPendingReports();

        // ✅ Click listener for ON-GOING
        ongoingTab.setOnClickListener(v -> {
            Intent intent = new Intent(myreports.this, ongoing.class);
            startActivity(intent);
            finish();
        });

        // ✅ Click listener for COMPLETED
        completedTab.setOnClickListener(v -> {
            Intent intent = new Intent(myreports.this, completed.class);
            startActivity(intent);
            finish();
        });

        // ✅ Click listener for DECLINED
        declinedTab.setOnClickListener(v -> {
            Intent intent = new Intent(myreports.this, declined.class);
            startActivity(intent);
            finish();
        });

        // ✅ Back Button (ImageButton)
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(myreports.this, dashboard.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadPendingReports() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Fetch pending reports from Firestore
        // Query from root reports collection where userId matches and status is "pending"
        db.collection("reports")
                .whereEqualTo("userId", userId)
                .whereIn("status", java.util.Arrays.asList("pending", "Pending"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    contentLayout.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        // Show empty state message
                        TextView emptyMessage = new TextView(this);
                        emptyMessage.setText("No pending reports");
                        emptyMessage.setTextSize(16);
                        emptyMessage.setPadding(16, 16, 16, 16);
                        emptyMessage.setGravity(android.view.Gravity.CENTER);
                        contentLayout.addView(emptyMessage);
                        return;
                    }

                    // Create report cards for each report
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
        cardContent.setBackgroundColor(0xFFFFFFFF); // White background
        cardView.addView(cardContent);

        // Left side - Description and Barangay Container
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
        TextView descriptionLabel = new TextView(this);
        descriptionLabel.setText("Description:");
        descriptionLabel.setTextSize(15);
        descriptionLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        descriptionLabel.setTextColor(0xFF004AAD);
        leftContainer.addView(descriptionLabel);

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
        descParams.setMargins(0, 8, 0, 16);
        descriptionText.setLayoutParams(descParams);
        leftContainer.addView(descriptionText);

        // Barangay Section
        TextView barangayLabel = new TextView(this);
        barangayLabel.setText("Barangay:");
        barangayLabel.setTextSize(15);
        barangayLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        barangayLabel.setTextColor(0xFF004AAD);
        leftContainer.addView(barangayLabel);

        TextView barangayText = new TextView(this);
        String barangay = reportData.get("barangay") != null ?
                reportData.get("barangay").toString() : "No barangay";
        barangayText.setText(barangay);
        barangayText.setTypeface(null, android.graphics.Typeface.BOLD);
        barangayText.setTextSize(22);
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
                    // Image failed to load, show placeholder background
                    android.util.Log.e("MyReports", "Failed to load image from Blob", e);
                }
            }
        }

        imageContainer.addView(reportImage);

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
                android.util.Log.e("MyReports", "Failed to load image from URL: " + imageUrl, e);
                runOnUiThread(() -> {
                    // Keep the gray placeholder background
                });
            }
        }).start();
    }
}
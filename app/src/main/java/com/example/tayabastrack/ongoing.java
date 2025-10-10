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
import com.google.firebase.firestore.Blob;

public class ongoing extends AppCompatActivity {

    private LinearLayout contentLayout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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
        db.collection("users")
                .document(userId)
                .collection("reports")
                .whereEqualTo("status", "ongoing")
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
        descriptionText.setTextSize(16);
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
        barangayText.setTextSize(16);
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
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(150, 150);
        reportImage.setLayoutParams(imageParams);

        // Load image if available
        if (reportData.containsKey("incidentImage")) {
            try {
                Blob imageBlob = (Blob) reportData.get("incidentImage");
                byte[] imageBytes = imageBlob.toBytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null) {
                    reportImage.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                // Image failed to load, show placeholder background
            }
        }

        imageContainer.addView(reportImage);

        // Inspection Status Section (if available)
        if (reportData.containsKey("inspectionStatus") && reportData.get("inspectionStatus") != null) {
            TextView inspectionLabel = new TextView(this);
            inspectionLabel.setText("Inspection Status:");
            inspectionLabel.setTextSize(14);
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
            inspectionText.setText(reportData.get("inspectionStatus").toString());
            inspectionText.setTextSize(14);
            inspectionText.setTextColor(0xFF333333);
            LinearLayout.LayoutParams inspectionParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            inspectionParams.setMargins(0, 0, 0, 12);
            inspectionText.setLayoutParams(inspectionParams);
            cardContent.addView(inspectionText);
        }

        // To Repair In Section (if available)
        if (reportData.containsKey("toRepairIn") && reportData.get("toRepairIn") != null) {
            TextView repairLabel = new TextView(this);
            repairLabel.setText("To Repair In:");
            repairLabel.setTextSize(14);
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
            repairText.setText(reportData.get("toRepairIn").toString());
            repairText.setTextSize(14);
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
}
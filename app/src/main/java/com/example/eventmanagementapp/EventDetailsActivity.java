package com.example.eventmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class EventDetailsActivity extends AppCompatActivity {

    private String eventId;
    private FirebaseFirestore db;

    private TextView tvDetailsBannerEmoji, tvDetailsCategoryBadge;
    private TextView tvDetailsTitle, tvDetailsDateTime, tvDetailsLocation;
    private TextView tvDetailsPrice, tvDetailsSeats, tvDetailsDescription;
    private TextView tvDetailsOrganizerName, tvDetailsOrganizerEmail;
    private Button btnBookTicketNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        eventId = getIntent().getStringExtra("eventId");
        db = FirebaseFirestore.getInstance();

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        loadEventDetails();
    }

    private void bindViews() {
        tvDetailsBannerEmoji = findViewById(R.id.tvDetailsBannerEmoji);
        tvDetailsCategoryBadge = findViewById(R.id.tvDetailsCategoryBadge);
        tvDetailsTitle = findViewById(R.id.tvDetailsTitle);
        tvDetailsDateTime = findViewById(R.id.tvDetailsDateTime);
        tvDetailsLocation = findViewById(R.id.tvDetailsLocation);
        tvDetailsPrice = findViewById(R.id.tvDetailsPrice);
        tvDetailsSeats = findViewById(R.id.tvDetailsSeats);
        tvDetailsDescription = findViewById(R.id.tvDetailsDescription);
        tvDetailsOrganizerName = findViewById(R.id.tvDetailsOrganizerName);
        tvDetailsOrganizerEmail = findViewById(R.id.tvDetailsOrganizerEmail);
        btnBookTicketNow = findViewById(R.id.btnBookTicketNow);

        findViewById(R.id.tvDetailsBack).setOnClickListener(v -> finish());

        btnBookTicketNow.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookTicketActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
    }

    private void loadEventDetails() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!document.exists()) {
                        Toast.makeText(this, "Event no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String title = document.getString("title");
                    String category = document.getString("category");
                    String description = document.getString("description");
                    String date = document.getString("date");
                    String startTime = document.getString("startTime");
                    String endTime = document.getString("endTime");
                    String location = document.getString("location");
                    String organizerName = document.getString("organizerName");
                    String organizerEmail = document.getString("organizerEmail");

                    Long priceLong = document.getLong("price");
                    Long totalLong = document.getLong("totalTickets");
                    Long soldLong = document.getLong("ticketsSold");

                    long price = priceLong != null ? priceLong : 0;
                    long total = totalLong != null ? totalLong : 0;
                    long sold = soldLong != null ? soldLong : 0;
                    long available = Math.max(0, total - sold);

                    tvDetailsTitle.setText(title != null ? title : "Event Details");
                    tvDetailsCategoryBadge.setText(category != null ? category : "General");
                    tvDetailsBannerEmoji.setText(getEmojiForCategory(category));

                    String timeStr = (startTime != null ? startTime : "")
                            + (endTime != null && !endTime.isEmpty() ? " - " + endTime : "");

                    tvDetailsDateTime.setText("📅  " + (date != null ? date : "")
                            + (timeStr.isEmpty() ? "" : "  •  " + timeStr));

                    tvDetailsLocation.setText("📍  " + (location != null ? location : "Campus"));
                    tvDetailsPrice.setText(price == 0 ? "Free" : "₹" + price);
                    tvDetailsSeats.setText(available + " Seats Available");
                    tvDetailsDescription.setText(description != null ? description : "No description provided.");

                    tvDetailsOrganizerName.setText(organizerName != null && !organizerName.isEmpty()
                            ? organizerName : "Event Organizer");

                    tvDetailsOrganizerEmail.setText(organizerEmail != null && !organizerEmail.isEmpty()
                            ? organizerEmail : "");

                    if (available <= 0) {
                        btnBookTicketNow.setEnabled(false);
                        btnBookTicketNow.setText("Sold Out");
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Could not load details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getEmojiForCategory(String category) {
        if (category == null) return "🎉";
        switch (category.toLowerCase(Locale.US)) {
            case "technology":
                return "💻";
            case "cultural":
                return "🎭";
            case "workshop":
                return "🛠️";
            case "sports":
                return "🏆";
            case "academic":
                return "📚";
            default:
                return "🎉";
        }
    }
}

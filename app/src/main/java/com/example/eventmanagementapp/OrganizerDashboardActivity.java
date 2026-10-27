package com.example.eventmanagementapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrganizerDashboardActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvStatMyEvents, tvStatTotalTickets, tvStatUpcoming, tvStatCompleted;
    private TextView tvNoOrganizerEvents;
    private LinearLayout llRecentEventsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_dashboard);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                int statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
                int navHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom;

                View topContainer = findViewById(R.id.llTopContainer);
                if (topContainer != null) {
                    topContainer.setPadding(
                            topContainer.getPaddingLeft(),
                            statusBarHeight + dpToPx(16),
                            topContainer.getPaddingRight(),
                            topContainer.getPaddingBottom()
                    );
                }

                View bottomNav = findViewById(R.id.llBottomNav);
                if (bottomNav != null) {
                    bottomNav.setPadding(
                            bottomNav.getPaddingLeft(),
                            bottomNav.getPaddingTop(),
                            bottomNav.getPaddingRight(),
                            Math.max(dpToPx(12), navHeight + dpToPx(8))
                    );
                }
                return insets;
            });
        }

        tvStatMyEvents = findViewById(R.id.tvStatMyEvents);
        tvStatTotalTickets = findViewById(R.id.tvStatTotalTickets);
        tvStatUpcoming = findViewById(R.id.tvStatUpcoming);
        tvStatCompleted = findViewById(R.id.tvStatCompleted);
        tvNoOrganizerEvents = findViewById(R.id.tvNoOrganizerEvents);
        llRecentEventsContainer = findViewById(R.id.llRecentEventsContainer);

        loadOrganizerProfile();

        Button action = findViewById(R.id.btnDashboardAction);
        action.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class))
        );

        View navCreate = findViewById(R.id.navOrganizerCreate);
        if (navCreate != null) {
            navCreate.setOnClickListener(v ->
                    startActivity(new Intent(this, CreateEventActivity.class))
            );
        }

        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser() != null) {
            loadOrganizerEvents();
        }
    }

    private void loadOrganizerProfile() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (document.exists()) {
                        String name = document.getString("name");
                        String organizerId = document.getString("organizerId");

                        ((TextView) findViewById(R.id.tvDashboardTitle))
                                .setText("Welcome,\n"
                                        + (name == null || name.isEmpty() ? "Organizer" : name) + "!");

                        ((TextView) findViewById(R.id.tvDashboardEmail))
                                .setText("Organizer ID: "
                                        + (organizerId == null || organizerId.isEmpty()
                                        ? "Verified" : organizerId));
                    }
                });
    }

    private void loadOrganizerEvents() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("events")
                .whereEqualTo("organizerUid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    llRecentEventsContainer.removeAllViews();

                    int myEventsCount = 0;
                    int totalTicketsSold = 0;
                    int liveInFeedCount = 0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        myEventsCount++;

                        Long soldLong = doc.getLong("ticketsSold");
                        int sold = soldLong != null ? soldLong.intValue() : 0;
                        totalTicketsSold += sold;

                        Boolean showInFeed = doc.getBoolean("showInFeed");
                        if (Boolean.TRUE.equals(showInFeed)) {
                            liveInFeedCount++;
                        }

                        addRecentEventCard(doc);
                    }

                    tvStatMyEvents.setText(String.valueOf(myEventsCount));
                    tvStatTotalTickets.setText(String.valueOf(totalTicketsSold));
                    tvStatUpcoming.setText(String.valueOf(myEventsCount));
                    tvStatCompleted.setText(String.valueOf(liveInFeedCount));

                    tvNoOrganizerEvents.setVisibility(
                            myEventsCount == 0 ? View.VISIBLE : View.GONE
                    );
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this,
                                "Failed to load events: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addRecentEventCard(DocumentSnapshot doc) {
        String eventId = doc.getId();
        String title = doc.getString("title");
        String date = doc.getString("date");
        String location = doc.getString("location");
        Long totalLong = doc.getLong("totalTickets");
        Long soldLong = doc.getLong("ticketsSold");
        Long priceLong = doc.getLong("price");

        int total = totalLong != null ? totalLong.intValue() : 0;
        int sold = soldLong != null ? soldLong.intValue() : 0;
        long price = priceLong != null ? priceLong : 0;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundColor(Color.parseColor("#1A2036"));
        int pad = dpToPx(14);
        card.setPadding(pad, pad, pad, pad);
        card.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dpToPx(12);
        card.setLayoutParams(cardParams);

        // Left colored badge representing event banner thumbnail
        TextView badge = new TextView(this);
        badge.setText("🎉");
        badge.setTextSize(22f);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundColor(Color.parseColor("#3B1F6E"));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                dpToPx(56), dpToPx(56)
        );
        badgeParams.rightMargin = dpToPx(14);
        badge.setLayoutParams(badgeParams);
        card.addView(badge);

        // Middle details column
        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        infoCol.setLayoutParams(infoParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title != null ? title : "Untitled Event");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        infoCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText((date != null ? date : "") + " • " + (location != null ? location : ""));
        tvSub.setTextColor(Color.parseColor("#A7AEC4"));
        tvSub.setTextSize(12f);
        infoCol.addView(tvSub);

        TextView tvStats = new TextView(this);
        tvStats.setText(total + " tickets • " + sold + " sold   |   ₹" + price);
        tvStats.setTextColor(Color.parseColor("#A78BFA"));
        tvStats.setTextSize(13f);
        tvStats.setTypeface(null, Typeface.BOLD);
        infoCol.addView(tvStats);

        card.addView(infoCol);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventDetailsActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        llRecentEventsContainer.addView(card);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void logout() {
        auth.signOut();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }
}
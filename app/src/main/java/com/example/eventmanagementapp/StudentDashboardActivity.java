package com.example.eventmanagementapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentDashboardActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvStudentGreeting, tvStudentSectionTitle, tvStudentEmptyState;
    private EditText etSearchEvents;
    private HorizontalScrollView hsvCategories;
    private TextView chipAll, chipTech, chipCultural, chipWorkshop;
    private TextView navStudentHome, navStudentTickets;
    private LinearLayout llStudentEventsContainer;

    private final List<DocumentSnapshot> allEvents = new ArrayList<>();
    private String selectedCategory = "All";
    private boolean showingMyTickets = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_student_dashboard);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int navHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

                View topContainer = findViewById(R.id.llStudentTopContainer);
                if (topContainer != null) {
                    topContainer.setPadding(
                            topContainer.getPaddingLeft(),
                            statusBarHeight + dpToPx(16),
                            topContainer.getPaddingRight(),
                            topContainer.getPaddingBottom()
                    );
                }

                View bottomNav = findViewById(R.id.llStudentBottomNav);
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

        ScrollView scrollView = findViewById(R.id.studentScrollView);
        if (scrollView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
                int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeHeight);
                return insets;
            });
        }

        tvStudentGreeting = findViewById(R.id.tvStudentGreeting);
        tvStudentSectionTitle = findViewById(R.id.tvStudentSectionTitle);
        tvStudentEmptyState = findViewById(R.id.tvStudentEmptyState);
        etSearchEvents = findViewById(R.id.etSearchEvents);
        hsvCategories = findViewById(R.id.hsvCategories);
        chipAll = findViewById(R.id.chipAll);
        chipTech = findViewById(R.id.chipTech);
        chipCultural = findViewById(R.id.chipCultural);
        chipWorkshop = findViewById(R.id.chipWorkshop);
        navStudentHome = findViewById(R.id.navStudentHome);
        navStudentTickets = findViewById(R.id.navStudentTickets);
        llStudentEventsContainer = findViewById(R.id.llStudentEventsContainer);

        loadStudentProfile();
        setupSearchAndFilters();
        setupBottomNav();

        findViewById(R.id.btnStudentLogout).setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser() != null) {
            if (showingMyTickets) {
                loadMyBookedTickets();
            } else {
                loadAllVisibleEvents();
            }
        }
    }

    private void loadStudentProfile() {
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
                        if (name != null && !name.trim().isEmpty()) {
                            tvStudentGreeting.setText("Hi, " + name);
                        }
                    }
                });
    }

    private void setupSearchAndFilters() {
        etSearchEvents.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!showingMyTickets) {
                    renderFilteredEvents();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipAll.setOnClickListener(v -> selectCategory("All", chipAll));
        chipTech.setOnClickListener(v -> selectCategory("Technology", chipTech));
        chipCultural.setOnClickListener(v -> selectCategory("Cultural", chipCultural));
        chipWorkshop.setOnClickListener(v -> selectCategory("Workshop", chipWorkshop));
    }

    private void selectCategory(String category, TextView activeChip) {
        selectedCategory = category;

        TextView[] chips = {chipAll, chipTech, chipCultural, chipWorkshop};
        for (TextView chip : chips) {
            if (chip == activeChip) {
                chip.setBackgroundColor(Color.parseColor("#7C3AED"));
                chip.setTextColor(Color.WHITE);
            } else {
                chip.setBackgroundColor(Color.parseColor("#1A2036"));
                chip.setTextColor(Color.parseColor("#A7AEC4"));
            }
        }

        renderFilteredEvents();
    }

    private void setupBottomNav() {
        navStudentHome.setOnClickListener(v -> {
            showingMyTickets = false;
            navStudentHome.setTextColor(Color.parseColor("#A78BFA"));
            navStudentHome.setTypeface(null, Typeface.BOLD);
            navStudentTickets.setTextColor(Color.parseColor("#A7AEC4"));
            navStudentTickets.setTypeface(null, Typeface.NORMAL);

            etSearchEvents.setVisibility(View.VISIBLE);
            hsvCategories.setVisibility(View.VISIBLE);
            tvStudentSectionTitle.setText("Upcoming Events");
            loadAllVisibleEvents();
        });

        navStudentTickets.setOnClickListener(v -> {
            showingMyTickets = true;
            navStudentTickets.setTextColor(Color.parseColor("#A78BFA"));
            navStudentTickets.setTypeface(null, Typeface.BOLD);
            navStudentHome.setTextColor(Color.parseColor("#A7AEC4"));
            navStudentHome.setTypeface(null, Typeface.NORMAL);

            etSearchEvents.setVisibility(View.GONE);
            hsvCategories.setVisibility(View.GONE);
            tvStudentSectionTitle.setText("My Booked Tickets");
            loadMyBookedTickets();
        });
    }

    private void loadAllVisibleEvents() {
        db.collection("events")
                .whereEqualTo("showInFeed", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    allEvents.clear();
                    allEvents.addAll(querySnapshot.getDocuments());
                    renderFilteredEvents();
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this,
                                "Could not load events: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderFilteredEvents() {
        llStudentEventsContainer.removeAllViews();
        String query = etSearchEvents.getText().toString().trim().toLowerCase(Locale.US);

        int matchCount = 0;
        for (DocumentSnapshot doc : allEvents) {
            String title = doc.getString("title") != null ? doc.getString("title") : "";
            String location = doc.getString("location") != null ? doc.getString("location") : "";
            String category = doc.getString("category") != null ? doc.getString("category") : "";

            boolean matchesCategory = "All".equalsIgnoreCase(selectedCategory)
                    || category.equalsIgnoreCase(selectedCategory);

            boolean matchesQuery = query.isEmpty()
                    || title.toLowerCase(Locale.US).contains(query)
                    || location.toLowerCase(Locale.US).contains(query);

            if (matchesCategory && matchesQuery) {
                matchCount++;
                addStudentEventCard(doc);
            }
        }

        if (matchCount == 0) {
            tvStudentEmptyState.setText("No events match your filter.");
            tvStudentEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvStudentEmptyState.setVisibility(View.GONE);
        }
    }

    private void addStudentEventCard(DocumentSnapshot doc) {
        String eventId = doc.getId();
        String title = doc.getString("title");
        String category = doc.getString("category");
        String date = doc.getString("date");
        String location = doc.getString("location");
        Long priceLong = doc.getLong("price");
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
        cardParams.bottomMargin = dpToPx(14);
        card.setLayoutParams(cardParams);

        // Left Event Thumbnail Box
        TextView thumb = new TextView(this);
        thumb.setText(getEmojiForCategory(category));
        thumb.setTextSize(26f);
        thumb.setGravity(Gravity.CENTER);
        thumb.setBackgroundColor(Color.parseColor("#3B1F6E"));
        LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(
                dpToPx(78), dpToPx(78)
        );
        thumbParams.rightMargin = dpToPx(14);
        thumb.setLayoutParams(thumbParams);
        card.addView(thumb);

        // Right Details Column
        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        infoCol.setLayoutParams(infoParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title != null ? title : "Event");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        infoCol.addView(tvTitle);

        TextView tvDate = new TextView(this);
        tvDate.setText("📅  " + (date != null ? date : "TBA"));
        tvDate.setTextColor(Color.parseColor("#A7AEC4"));
        tvDate.setTextSize(12f);
        tvDate.setPadding(0, dpToPx(4), 0, 0);
        infoCol.addView(tvDate);

        TextView tvLoc = new TextView(this);
        tvLoc.setText("📍  " + (location != null ? location : "Campus"));
        tvLoc.setTextColor(Color.parseColor("#A7AEC4"));
        tvLoc.setTextSize(12f);
        tvLoc.setPadding(0, dpToPx(2), 0, dpToPx(6));
        infoCol.addView(tvLoc);

        TextView tvPrice = new TextView(this);
        tvPrice.setText(price == 0 ? "Free" : "₹" + price);
        tvPrice.setTextColor(Color.WHITE);
        tvPrice.setTextSize(16f);
        tvPrice.setTypeface(null, Typeface.BOLD);
        infoCol.addView(tvPrice);

        card.addView(infoCol);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventDetailsActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        llStudentEventsContainer.addView(card);
    }

    private void loadMyBookedTickets() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("bookings")
                .whereEqualTo("studentUid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    llStudentEventsContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        tvStudentEmptyState.setText("You haven't booked any tickets yet.");
                        tvStudentEmptyState.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvStudentEmptyState.setVisibility(View.GONE);
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        addBookedTicketCard(doc);
                    }
                });
    }

    private void addBookedTicketCard(DocumentSnapshot doc) {
        String title = doc.getString("eventTitle");
        String date = doc.getString("eventDate");
        String time = doc.getString("eventTime");
        String location = doc.getString("eventLocation");
        String ticketCode = doc.getString("ticketCode");
        Long qtyLong = doc.getLong("quantity");
        Long totalLong = doc.getLong("totalAmount");

        int qty = qtyLong != null ? qtyLong.intValue() : 1;
        long total = totalLong != null ? totalLong : 0;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#1A2036"));
        int pad = dpToPx(16);
        card.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dpToPx(12);
        card.setLayoutParams(params);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title != null ? title : "Event Ticket");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        card.addView(tvTitle);

        TextView tvInfo = new TextView(this);
        tvInfo.setText("📅 " + (date != null ? date : "")
                + (time != null ? " • " + time : "")
                + "\n📍 " + (location != null ? location : ""));
        tvInfo.setTextColor(Color.parseColor("#A7AEC4"));
        tvInfo.setTextSize(13f);
        tvInfo.setPadding(0, dpToPx(6), 0, dpToPx(8));
        card.addView(tvInfo);

        TextView tvFooter = new TextView(this);
        tvFooter.setText(qty + (qty == 1 ? " Ticket" : " Tickets")
                + " • ₹" + total + "   |   Code: " + (ticketCode != null ? ticketCode : "CONFIRMED"));
        tvFooter.setTextColor(Color.parseColor("#10B981"));
        tvFooter.setTextSize(13f);
        tvFooter.setTypeface(null, Typeface.BOLD);
        card.addView(tvFooter);

        llStudentEventsContainer.addView(card);
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
            default:
                return "🎉";
        }
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
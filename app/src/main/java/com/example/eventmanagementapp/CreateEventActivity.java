package com.example.eventmanagementapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    private LinearLayout llCreateHeader, step1Container, step2Container, step3Container, stepSuccessContainer;
    private TextView tvStep1Indicator, tvStep2Indicator, tvStep3Indicator;

    private EditText etEventTitle, etEventDescription;
    private Spinner spCategory, spTicketType;
    private EditText etEventDate, etStartTime, etEndTime, etLocation, etAddress;
    private EditText etPrice, etTotalTickets, etBookingDeadline;
    private Switch swEnableQr, swShowInFeed;

    private TextView tvSuccessEventTitle, tvSuccessEventDateTime, tvSuccessEventLocation, tvSuccessEventPrice;
    private Button btnSubmitEvent;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String organizerName = "Event Organizer";
    private String organizerEmail = "";

    private static final String[] MONTHS = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        ScrollView scrollView = findViewById(R.id.createEventScrollView);
        if (scrollView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
                int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeHeight);
                return insets;
            });
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        organizerEmail = auth.getCurrentUser().getEmail() != null
                ? auth.getCurrentUser().getEmail() : "";

        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("name") != null) {
                        organizerName = doc.getString("name");
                    }
                });

        bindViews();
        setupSpinners();
        setupPickers();
        setupStepNavigation();
    }

    private void bindViews() {
        llCreateHeader = findViewById(R.id.llCreateHeader);
        step1Container = findViewById(R.id.step1Container);
        step2Container = findViewById(R.id.step2Container);
        step3Container = findViewById(R.id.step3Container);
        stepSuccessContainer = findViewById(R.id.stepSuccessContainer);

        tvStep1Indicator = findViewById(R.id.tvStep1Indicator);
        tvStep2Indicator = findViewById(R.id.tvStep2Indicator);
        tvStep3Indicator = findViewById(R.id.tvStep3Indicator);

        etEventTitle = findViewById(R.id.etEventTitle);
        spCategory = findViewById(R.id.spCategory);
        etEventDescription = findViewById(R.id.etEventDescription);

        etEventDate = findViewById(R.id.etEventDate);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        etLocation = findViewById(R.id.etLocation);
        etAddress = findViewById(R.id.etAddress);

        spTicketType = findViewById(R.id.spTicketType);
        etPrice = findViewById(R.id.etPrice);
        etTotalTickets = findViewById(R.id.etTotalTickets);
        etBookingDeadline = findViewById(R.id.etBookingDeadline);
        swEnableQr = findViewById(R.id.swEnableQr);
        swShowInFeed = findViewById(R.id.swShowInFeed);

        tvSuccessEventTitle = findViewById(R.id.tvSuccessEventTitle);
        tvSuccessEventDateTime = findViewById(R.id.tvSuccessEventDateTime);
        tvSuccessEventLocation = findViewById(R.id.tvSuccessEventLocation);
        tvSuccessEventPrice = findViewById(R.id.tvSuccessEventPrice);
        btnSubmitEvent = findViewById(R.id.btnSubmitEvent);

        findViewById(R.id.tvBackTop).setOnClickListener(v -> finish());
        findViewById(R.id.btnGoToMyEvents).setOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        String[] categories = {"Technology", "Cultural", "Workshop", "Sports", "Academic"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, categories
        );
        spCategory.setAdapter(catAdapter);

        String[] ticketTypes = {"General", "VIP", "Early Bird", "Free Entry"};
        ArrayAdapter<String> ticketAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, ticketTypes
        );
        spTicketType.setAdapter(ticketAdapter);
    }

    private void setupPickers() {
        etEventDate.setOnClickListener(v -> showDatePicker(etEventDate));
        etBookingDeadline.setOnClickListener(v -> showDatePicker(etBookingDeadline));
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));
    }

    private void showDatePicker(EditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String formatted = String.format(Locale.US, "%02d %s %d",
                    dayOfMonth, MONTHS[month], year);
            target.setText(formatted);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(EditText target) {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String amPm = hourOfDay >= 12 ? "PM" : "AM";
            int hour12 = hourOfDay % 12;
            if (hour12 == 0) hour12 = 12;
            String formatted = String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm);
            target.setText(formatted);
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }

    private void setupStepNavigation() {
        findViewById(R.id.btnStep1Next).setOnClickListener(v -> {
            if (etEventTitle.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please enter an Event Title", Toast.LENGTH_SHORT).show();
                return;
            }
            if (etEventDescription.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please enter a Description", Toast.LENGTH_SHORT).show();
                return;
            }
            showStep(2);
        });

        findViewById(R.id.btnStep2Back).setOnClickListener(v -> showStep(1));

        findViewById(R.id.btnStep2Next).setOnClickListener(v -> {
            if (etEventDate.getText().toString().trim().isEmpty()
                    || etStartTime.getText().toString().trim().isEmpty()
                    || etLocation.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please fill in Date, Start Time, and Location", Toast.LENGTH_SHORT).show();
                return;
            }
            showStep(3);
        });

        findViewById(R.id.btnStep3Back).setOnClickListener(v -> showStep(2));

        btnSubmitEvent.setOnClickListener(v -> publishEventToFirestore());
    }

    private void showStep(int step) {
        step1Container.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2Container.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        step3Container.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        int activeBg = Color.parseColor("#7C3AED");
        int inactiveBg = Color.parseColor("#1A2036");

        tvStep1Indicator.setBackgroundColor(step >= 1 ? activeBg : inactiveBg);
        tvStep2Indicator.setBackgroundColor(step >= 2 ? activeBg : inactiveBg);
        tvStep3Indicator.setBackgroundColor(step >= 3 ? activeBg : inactiveBg);

        tvStep1Indicator.setTextColor(Color.WHITE);
        tvStep2Indicator.setTextColor(step >= 2 ? Color.WHITE : Color.parseColor("#A7AEC4"));
        tvStep3Indicator.setTextColor(step >= 3 ? Color.WHITE : Color.parseColor("#A7AEC4"));
    }

    private void publishEventToFirestore() {
        String priceStr = etPrice.getText().toString().trim();
        String totalTicketsStr = etTotalTickets.getText().toString().trim();

        if (priceStr.isEmpty() || totalTicketsStr.isEmpty()) {
            Toast.makeText(this, "Please enter Ticket Price and Total Tickets", Toast.LENGTH_SHORT).show();
            return;
        }

        long price;
        long totalTickets;
        try {
            price = Long.parseLong(priceStr);
            totalTickets = Long.parseLong(totalTicketsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number for price or total tickets", Toast.LENGTH_SHORT).show();
            return;
        }

        if (totalTickets <= 0) {
            Toast.makeText(this, "Total tickets must be at least 1", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etEventTitle.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        String description = etEventDescription.getText().toString().trim();
        String date = etEventDate.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String ticketType = spTicketType.getSelectedItem().toString();
        String bookingDeadline = etBookingDeadline.getText().toString().trim();
        boolean enableQr = swEnableQr.isChecked();
        boolean showInFeed = swShowInFeed.isChecked();

        btnSubmitEvent.setEnabled(false);
        btnSubmitEvent.setText("Creating...");

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("title", title);
        eventData.put("category", category);
        eventData.put("description", description);
        eventData.put("date", date);
        eventData.put("startTime", startTime);
        eventData.put("endTime", endTime.isEmpty() ? "Onwards" : endTime);
        eventData.put("location", location);
        eventData.put("address", address);
        eventData.put("ticketType", ticketType);
        eventData.put("price", price);
        eventData.put("totalTickets", totalTickets);
        eventData.put("ticketsSold", 0L);
        eventData.put("bookingDeadline", bookingDeadline);
        eventData.put("enableQr", enableQr);
        eventData.put("showInFeed", showInFeed);
        eventData.put("organizerUid", auth.getCurrentUser().getUid());
        eventData.put("organizerName", organizerName);
        eventData.put("organizerEmail", organizerEmail);
        eventData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("events")
                .add(eventData)
                .addOnSuccessListener(docRef -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    llCreateHeader.setVisibility(View.GONE);
                    step3Container.setVisibility(View.GONE);
                    stepSuccessContainer.setVisibility(View.VISIBLE);

                    tvSuccessEventTitle.setText(title);
                    tvSuccessEventDateTime.setText("📅  " + date + "  •  " + startTime
                            + (endTime.isEmpty() ? "" : " - " + endTime));
                    tvSuccessEventLocation.setText("📍  " + location);
                    tvSuccessEventPrice.setText(price == 0 ? "Free" : "₹" + price);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    btnSubmitEvent.setEnabled(true);
                    btnSubmitEvent.setText("Create Event");
                    Toast.makeText(this,
                            "Failed to create event: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}

package com.example.eventmanagementapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class BookTicketActivity extends AppCompatActivity {

    private String eventId;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout llBookingFormContainer, llTicketConfirmationContainer;
    private TextView tvBookEventTitle, tvBookEventDate, tvBookEventLocation, tvBookUnitPrice;
    private TextView tvTicketQty, tvBookTotalAmount;
    private RadioGroup rgPaymentMethod;
    private Button btnProceedToPay;

    private TextView tvConfirmEventTitle, tvConfirmDate, tvConfirmTime, tvConfirmLocation;
    private TextView tvConfirmTicketSummary, tvConfirmTicketCode;
    private ImageView ivQrCode;

    private String eventTitle = "";
    private String eventDate = "";
    private String eventTime = "";
    private String eventLocation = "";
    private long unitPrice = 0;
    private long maxAvailable = 1;
    private int quantity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_ticket);

        eventId = getIntent().getStringExtra("eventId");
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (eventId == null || auth.getCurrentUser() == null) {
            finish();
            return;
        }

        bindViews();
        loadEventInfo();
    }

    private void bindViews() {
        llBookingFormContainer = findViewById(R.id.llBookingFormContainer);
        llTicketConfirmationContainer = findViewById(R.id.llTicketConfirmationContainer);

        tvBookEventTitle = findViewById(R.id.tvBookEventTitle);
        tvBookEventDate = findViewById(R.id.tvBookEventDate);
        tvBookEventLocation = findViewById(R.id.tvBookEventLocation);
        tvBookUnitPrice = findViewById(R.id.tvBookUnitPrice);
        tvTicketQty = findViewById(R.id.tvTicketQty);
        tvBookTotalAmount = findViewById(R.id.tvBookTotalAmount);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        btnProceedToPay = findViewById(R.id.btnProceedToPay);

        tvConfirmEventTitle = findViewById(R.id.tvConfirmEventTitle);
        tvConfirmDate = findViewById(R.id.tvConfirmDate);
        tvConfirmTime = findViewById(R.id.tvConfirmTime);
        tvConfirmLocation = findViewById(R.id.tvConfirmLocation);
        tvConfirmTicketSummary = findViewById(R.id.tvConfirmTicketSummary);
        tvConfirmTicketCode = findViewById(R.id.tvConfirmTicketCode);
        ivQrCode = findViewById(R.id.ivQrCode);

        findViewById(R.id.tvBookBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnTicketMinus).setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateTotals();
            }
        });

        findViewById(R.id.btnTicketPlus).setOnClickListener(v -> {
            if (quantity < maxAvailable && quantity < 10) {
                quantity++;
                updateTotals();
            } else {
                Toast.makeText(this, "Maximum available limit reached", Toast.LENGTH_SHORT).show();
            }
        });

        btnProceedToPay.setOnClickListener(v -> confirmBooking());

        View.OnClickListener goHomeListener = v -> {
            Intent intent = new Intent(this, StudentDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        };

        findViewById(R.id.btnViewMyTicket).setOnClickListener(goHomeListener);
        findViewById(R.id.btnBackToStudentHome).setOnClickListener(goHomeListener);
    }

    private void loadEventInfo() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed() || !doc.exists()) {
                        return;
                    }
                    eventTitle = doc.getString("title") != null ? doc.getString("title") : "Event";
                    eventDate = doc.getString("date") != null ? doc.getString("date") : "";
                    String start = doc.getString("startTime") != null ? doc.getString("startTime") : "";
                    String end = doc.getString("endTime") != null ? doc.getString("endTime") : "";
                    eventTime = start + (end.isEmpty() ? "" : " - " + end);
                    eventLocation = doc.getString("location") != null ? doc.getString("location") : "";

                    Long priceLong = doc.getLong("price");
                    Long totalLong = doc.getLong("totalTickets");
                    Long soldLong = doc.getLong("ticketsSold");

                    unitPrice = priceLong != null ? priceLong : 0;
                    long total = totalLong != null ? totalLong : 0;
                    long sold = soldLong != null ? soldLong : 0;
                    maxAvailable = Math.max(1, total - sold);

                    tvBookEventTitle.setText(eventTitle);
                    tvBookEventDate.setText("📅  " + eventDate);
                    tvBookEventLocation.setText("📍  " + eventLocation);
                    tvBookUnitPrice.setText(unitPrice == 0 ? "Free" : "₹" + unitPrice);

                    updateTotals();
                });
    }

    private void updateTotals() {
        tvTicketQty.setText(String.valueOf(quantity));
        long total = unitPrice * quantity;
        tvBookTotalAmount.setText(total == 0 ? "Free" : "₹" + total);
    }

    private void confirmBooking() {
        btnProceedToPay.setEnabled(false);
        btnProceedToPay.setText("Processing...");

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail() != null
                ? auth.getCurrentUser().getEmail() : "";
        String paymentMethod = rgPaymentMethod.getCheckedRadioButtonId() == R.id.rbUpi
                ? "UPI" : "Card";
        String ticketCode = String.format(Locale.US, "EVT%06d", new Random().nextInt(900000) + 100000);
        long totalAmount = unitPrice * quantity;

        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference bookingRef = db.collection("bookings").document();

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            if (!eventSnap.exists()) {
                throw new FirebaseFirestoreException(
                        "Event no longer exists",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            Long totalLong = eventSnap.getLong("totalTickets");
            Long soldLong = eventSnap.getLong("ticketsSold");
            long total = totalLong != null ? totalLong : 0;
            long sold = soldLong != null ? soldLong : 0;

            if (sold + quantity > total) {
                throw new FirebaseFirestoreException(
                        "Not enough tickets remaining",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            transaction.update(eventRef, "ticketsSold", sold + quantity);

            Map<String, Object> booking = new HashMap<>();
            booking.put("bookingId", bookingRef.getId());
            booking.put("ticketCode", ticketCode);
            booking.put("eventId", eventId);
            booking.put("eventTitle", eventTitle);
            booking.put("eventDate", eventDate);
            booking.put("eventTime", eventTime);
            booking.put("eventLocation", eventLocation);
            booking.put("studentUid", uid);
            booking.put("studentEmail", email);
            booking.put("quantity", quantity);
            booking.put("totalAmount", totalAmount);
            booking.put("paymentMethod", paymentMethod);
            booking.put("bookedAt", FieldValue.serverTimestamp());

            transaction.set(bookingRef, booking);
            return null;
        }).addOnSuccessListener(unused -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            showTicketConfirmation(ticketCode, totalAmount);
        }).addOnFailureListener(e -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            btnProceedToPay.setEnabled(true);
            btnProceedToPay.setText("Proceed to Pay");
            Toast.makeText(this,
                    "Booking failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        });
    }

    private void showTicketConfirmation(String ticketCode, long totalAmount) {
        llBookingFormContainer.setVisibility(View.GONE);
        llTicketConfirmationContainer.setVisibility(View.VISIBLE);

        tvConfirmEventTitle.setText(eventTitle);
        tvConfirmDate.setText("📅  " + eventDate);
        tvConfirmTime.setText("🕒  " + eventTime);
        tvConfirmLocation.setText("📍  " + eventLocation);
        tvConfirmTicketSummary.setText(quantity + (quantity == 1 ? " Ticket" : " Tickets")
                + "  •  " + (totalAmount == 0 ? "Free" : "₹" + totalAmount));
        tvConfirmTicketCode.setText(ticketCode);

        ivQrCode.setImageBitmap(generateQrBitmap(ticketCode, 220));
    }

    /**
     * Generates a clean QR-style matrix bitmap directly from the ticketCode seed
     * so no external ZXing dependency is required in build.gradle.
     */
    private Bitmap generateQrBitmap(String seed, int sizePx) {
        int grid = 21;
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);

        float cell = (float) sizePx / grid;
        Random rnd = new Random(seed.hashCode());

        for (int r = 0; r < grid; r++) {
            for (int c = 0; c < grid; c++) {
                boolean inTopLeft = r < 7 && c < 7;
                boolean inTopRight = r < 7 && c >= grid - 7;
                boolean inBottomLeft = r >= grid - 7 && c < 7;

                boolean drawCell;
                if (inTopLeft || inTopRight || inBottomLeft) {
                    int lr = r < 7 ? r : r - (grid - 7);
                    int lc = c < 7 ? c : c - (grid - 7);
                    boolean border = (lr == 0 || lr == 6 || lc == 0 || lc == 6);
                    boolean center = (lr >= 2 && lr <= 4 && lc >= 2 && lc <= 4);
                    drawCell = border || center;
                } else {
                    drawCell = rnd.nextBoolean();
                }

                if (drawCell) {
                    canvas.drawRect(c * cell, r * cell, (c + 1) * cell, (r + 1) * cell, paint);
                }
            }
        }
        return bmp;
    }
}

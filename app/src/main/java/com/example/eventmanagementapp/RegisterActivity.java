package com.example.eventmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etOrganizerId;
    private RadioGroup rgRole;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                View content = findViewById(R.id.registerScrollView);
                if (content != null) {
                    content.setPadding(
                            content.getPaddingLeft(),
                            statusBarHeight + dpToPx(16),
                            content.getPaddingRight(),
                            imeHeight
                    );
                }
                return insets;
            });
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        etOrganizerId = findViewById(R.id.etOrganizerId);
        rgRole = findViewById(R.id.rgRegisterRole);

        Button btnCreate = findViewById(R.id.btnCreateAccount);
        Button btnBack = findViewById(R.id.btnBackToLogin);

        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbRegisterOrganizer) {
                etOrganizerId.setVisibility(View.VISIBLE);
            } else {
                etOrganizerId.setVisibility(View.GONE);
            }
        });

        btnCreate.setOnClickListener(v -> register());

        btnBack.setOnClickListener(v -> finish());
    }

    private void register() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String organizerId =
                etOrganizerId.getText().toString().trim();

        boolean isOrganizer =
                rgRole.getCheckedRadioButtonId()
                        == R.id.rbRegisterOrganizer;

        String role = isOrganizer ? "organizer" : "student";

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all required fields");
            return;
        }

        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters");
            return;
        }

        if (isOrganizer && organizerId.isEmpty()) {
            showMessage("Enter your Organizer ID");
            return;
        }

        createAccount(name, email, password, role, isOrganizer ? organizerId : "");
    }

    private void createAccount(
            String name,
            String email,
            String password,
            String role,
            String organizerId) {

        // 1. Authenticate first so Firestore reads/writes happen as a signed-in user.
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser == null) {
                        showMessage("Account created. Please sign in.");
                        finish();
                        return;
                    }

                    String uid = firebaseUser.getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("uid", uid);
                    user.put("name", name);
                    user.put("email", email);
                    user.put("role", role);
                    user.put("organizerId", organizerId);
                    user.put("createdAt", FieldValue.serverTimestamp());

                    DocumentReference userRef = db.collection("users").document(uid);

                    if ("organizer".equals(role)) {
                        // 2. Atomically validate Organizer ID, save user profile, and redeem ID.
                        DocumentReference codeRef =
                                db.collection("organizerCodes").document(organizerId);

                        db.runTransaction(transaction -> {
                            DocumentSnapshot codeDoc = transaction.get(codeRef);

                            if (!codeDoc.exists()
                                    || !Boolean.TRUE.equals(codeDoc.getBoolean("active"))) {
                                throw new FirebaseFirestoreException(
                                        "Invalid or inactive Organizer ID",
                                        FirebaseFirestoreException.Code.ABORTED
                                );
                            }

                            String usedByUid = codeDoc.getString("usedByUid");
                            if (usedByUid != null && !usedByUid.trim().isEmpty()) {
                                throw new FirebaseFirestoreException(
                                        "This Organizer ID has already been used",
                                        FirebaseFirestoreException.Code.ABORTED
                                );
                            }

                            transaction.set(userRef, user);
                            transaction.update(codeRef, "usedByUid", uid);
                            return null;
                        }).addOnSuccessListener(unused -> {
                            showMessage("Organizer account created!");
                            navigateToDashboard(role);
                        }).addOnFailureListener(e -> {
                            // Roll back the temporary Auth user so the email can be retried.
                            firebaseUser.delete().addOnCompleteListener(task -> {
                                auth.signOut();
                                showMessage(e.getMessage() != null
                                        ? e.getMessage()
                                        : "Could not validate Organizer ID");
                            });
                        });
                    } else {
                        // Student registration
                        userRef.set(user)
                                .addOnSuccessListener(unused -> {
                                    showMessage("Student account created!");
                                    navigateToDashboard(role);
                                })
                                .addOnFailureListener(e -> {
                                    showMessage("Account created, but profile "
                                            + "saving failed: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e ->
                        showMessage("Registration failed: "
                                + e.getMessage())
                );
    }

    private void navigateToDashboard(String role) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        Class<?> target = "organizer".equals(role)
                ? OrganizerDashboardActivity.class
                : StudentDashboardActivity.class;

        Intent intent = new Intent(this, target);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
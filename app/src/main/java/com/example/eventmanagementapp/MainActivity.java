package com.example.eventmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private RadioGroup rgRole;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                View content = findViewById(R.id.loginScrollView);
                if (content != null) {
                    content.setPadding(
                            content.getPaddingLeft(),
                            statusBarHeight + dpToPx(16),
                            content.getPaddingRight(),
                            content.getPaddingBottom()
                    );
                }
                return insets;
            });
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        rgRole = findViewById(R.id.rgRole);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> login());

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(
                        MainActivity.this,
                        RegisterActivity.class
                ))
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Reuse an existing Firebase session without enforcing the default RadioButton selection.
        if (auth.getCurrentUser() != null) {
            openDashboard(false);
        }
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Enter email and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> openDashboard(true))
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Login failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void openDashboard(boolean verifySelectedRole) {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!document.exists()) {
                        auth.signOut();
                        Toast.makeText(this,
                                "User profile not found. Please register again.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String actualRole = document.getString("role");

                    if (actualRole == null) {
                        auth.signOut();
                        Toast.makeText(this,
                                "Account role is missing",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (verifySelectedRole) {
                        boolean studentSelected =
                                rgRole.getCheckedRadioButtonId()
                                        == R.id.rbStudent;

                        String selectedRole = studentSelected
                                ? "student" : "organizer";

                        if (!actualRole.equals(selectedRole)) {
                            auth.signOut();
                            Toast.makeText(this,
                                    "Please select your registered account type",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    Class<?> target = actualRole.equals("organizer")
                            ? OrganizerDashboardActivity.class
                            : StudentDashboardActivity.class;

                    Intent intent = new Intent(this, target);
                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Could not load profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
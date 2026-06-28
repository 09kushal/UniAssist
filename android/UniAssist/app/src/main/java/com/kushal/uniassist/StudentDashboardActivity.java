package com.kushal.uniassist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnFindTutor, btnMyBookings, btnNotifications, btnEditProfile, btnLogout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        sessionManager = new SessionManager(this);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnFindTutor = findViewById(R.id.btnFindTutor);
        btnMyBookings = findViewById(R.id.btnMyBookings);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);

        String fullName = sessionManager.getFullName();
        if (fullName != null) {
            tvWelcome.setText("Welcome, " + fullName);
        }

        btnFindTutor.setOnClickListener(v -> Toast.makeText(this, "Find Tutor coming soon!", Toast.LENGTH_SHORT).show());
        btnMyBookings.setOnClickListener(v -> Toast.makeText(this, "My Bookings coming soon!", Toast.LENGTH_SHORT).show());
        btnNotifications.setOnClickListener(v -> Toast.makeText(this, "Notifications coming soon!", Toast.LENGTH_SHORT).show());
        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(StudentDashboardActivity.this, EditProfileActivity.class)));

        btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(StudentDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}

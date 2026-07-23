package com.kushal.uniassist;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.kushal.uniassist.models.TutorResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TutorDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvVerificationStatus, tvTotalSessions, tvTutorRating, tvEarnings;
    private ImageView ivTutorAvatar;
    private CardView cardBookings, cardProfile, cardPayouts, cardReports;
    private Button btnLogout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_dashboard);

        // Edge-to-edge & Status Bar Fix
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus);
        tvTotalSessions = findViewById(R.id.tvTotalSessions);
        tvTutorRating = findViewById(R.id.tvTutorRating);
        tvEarnings = findViewById(R.id.tvEarnings);
        ivTutorAvatar = findViewById(R.id.ivTutorAvatar);

        cardBookings = findViewById(R.id.cardBookings);
        cardProfile = findViewById(R.id.cardProfile);
        cardPayouts = findViewById(R.id.cardPayouts);
        cardReports = findViewById(R.id.cardReports);
        btnLogout = findViewById(R.id.btnLogout);

        String fullName = sessionManager.getFullName();
        if (fullName != null && tvWelcome != null) {
            tvWelcome.setText(fullName);
        }

        fetchTutorProfile();

        cardBookings.setOnClickListener(v -> startActivity(new Intent(this, TutorBookingsActivity.class)));
        cardProfile.setOnClickListener(v -> startActivity(new Intent(this, TutorEditProfileActivity.class)));
        cardPayouts.setOnClickListener(v -> startActivity(new Intent(this, PayoutActivity.class)));
        cardReports.setOnClickListener(v -> startActivity(new Intent(this, TutorReportsActivity.class)));

        btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void fetchTutorProfile() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + sessionManager.getAccessToken();

        apiService.getMyTutorProfile(authHeader).enqueue(new Callback<TutorResponse>() {
            @Override
            public void onResponse(Call<TutorResponse> call, Response<TutorResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayTutorData(response.body());
                } else if (response.code() == 401) {
                    redirectToLogin();
                }
            }

            @Override
            public void onFailure(Call<TutorResponse> call, Throwable t) {
                Toast.makeText(TutorDashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayTutorData(TutorResponse tutor) {
        if (tvVerificationStatus != null) {
            if (tutor.isVerified()) {
                tvVerificationStatus.setText("✓ Verified");
                tvVerificationStatus.setBackgroundResource(R.drawable.bg_verified_badge);
            } else {
                tvVerificationStatus.setText("⏳ Pending");
                tvVerificationStatus.setBackgroundResource(R.drawable.bg_pending_badge);
            }
        }

        if (tvTotalSessions != null) tvTotalSessions.setText(String.valueOf(tutor.getTotalSessionsDone()));
        if (tvTutorRating != null) tvTutorRating.setText(tutor.getAverageRating());
        
        try {
            double price = Double.parseDouble(tutor.getPricingPerSession());
            int total = (int)(tutor.getTotalSessionsDone() * price);
            if (tvEarnings != null) tvEarnings.setText("NPR " + total);
        } catch (Exception e) {
            if (tvEarnings != null) tvEarnings.setText("NPR 0");
        }

        if (ivTutorAvatar != null) {
            Glide.with(this)
                    .load(tutor.getProfilePhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_tutor_placeholder)
                    .into(ivTutorAvatar);
        }
    }

    private void redirectToLogin() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

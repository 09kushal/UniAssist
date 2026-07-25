package com.kushal.uniassist;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import com.google.gson.Gson;
import com.kushal.uniassist.models.ApiResponse;
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
        Log.e("TutorDash", "=== DASHBOARD CREATED ===");
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

    @Override
    protected void onResume() {
        super.onResume();
        fetchTutorProfile();
    }

    private void fetchTutorProfile() {
        Log.e("TutorDash", "=== FETCHING PROFILE ===");
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String token = sessionManager.getAccessToken();
        Log.e("TutorDash", "Token null: " + (token == null));
        Log.e("TutorDash", "Token length: " + (token != null ? token.length() : 0));
        
        String authHeader = "Bearer " + token;
        Log.e("TutorDash", "Calling my-profile API...");

        apiService.getMyTutorProfile(authHeader).enqueue(new Callback<ApiResponse<TutorResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TutorResponse>> call, Response<ApiResponse<TutorResponse>> response) {
                Log.e("TutorDash", "=== GOT RESPONSE ===");
                Log.e("TutorDash", "Code: " + response.code());
                Log.e("TutorDash", "Body null: " + (response.body() == null));

                if (response.isSuccessful() && response.body() != null) {
                    // Log raw JSON response
                    String rawJson = new Gson().toJson(response.body());
                    Log.d("TutorDash", "RAW RESPONSE: " + rawJson);

                    TutorResponse profile = response.body().getData();

                    if (profile != null) {
                        Log.d("TutorDash", "is_verified: " + profile.isVerified());
                        Log.d("TutorDash", "is_verified_badge: " + profile.isVerifiedBadge());
                        Log.d("TutorDash", "full_name: " + profile.getFullName());
                        displayTutorData(profile);
                    } else {
                        Log.e("TutorDash", "Profile (data) is NULL!");
                    }
                } else if (response.code() == 401) {
                    redirectToLogin();
                } else {
                    Log.e("TutorDash", "Response failed: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e("TutorDash", "Error: " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        Log.e("TutorDash", e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TutorResponse>> call, Throwable t) {
                Log.e("TutorDash", "=== FAILURE ===");
                Log.e("TutorDash", "Error: " + t.getMessage());
                Toast.makeText(TutorDashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayTutorData(TutorResponse tutor) {
        Log.d("TutorDash", "is_verified_badge: " + tutor.isVerifiedBadge());
        Log.d("TutorDash", "is_verified: " + tutor.isVerified());
        Log.d("TutorDash", "Full response: " + new Gson().toJson(tutor));

        if (tvWelcome != null) {
            tvWelcome.setText("Welcome back,\n" + tutor.getFullName());
        }

        if (tvVerificationStatus != null) {
            if (tutor.isVerifiedBadge() || tutor.isVerified()) {
                tvVerificationStatus.setText("✓ Verified");
                tvVerificationStatus.setBackgroundResource(R.drawable.bg_verified_badge);
            } else {
                tvVerificationStatus.setText("⏳ Pending");
                tvVerificationStatus.setBackgroundResource(R.drawable.bg_pending_badge);
            }
        }

        if (tvTotalSessions != null) tvTotalSessions.setText(String.valueOf(tutor.getTotalSessionsDone()));
        if (tvTutorRating != null) {
            tvTutorRating.setText(String.format(java.util.Locale.US, "%.1f", tutor.getAverageRatingFloat()));
        }
        
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

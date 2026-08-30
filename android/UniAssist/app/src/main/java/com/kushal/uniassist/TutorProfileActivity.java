package com.kushal.uniassist;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.AvailabilitySlot;
import com.kushal.uniassist.models.SkillResponse;
import com.kushal.uniassist.models.SubjectResponse;
import com.kushal.uniassist.models.TutorResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TutorProfileActivity extends AppCompatActivity {

    private ImageView ivProfilePhoto;
    private TextView tvTutorName, tvBio, tvPrice, tvRating, tvSessions, tvDomain, tvNoSubjects, tvNoAvailability, tvNoReviews;
    private ChipGroup chipGroupSubjects;
    private CardView cvVerifiedBadge;
    private RecyclerView rvAvailability, rvReviews;
    private ReviewAdapter reviewAdapter;
    private Button btnBookSession;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private int tutorId;
    private TutorResponse currentTutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_profile);

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
        tutorId = getIntent().getIntExtra("tutor_id", -1);
        Log.d("TutorProfile", "Loading tutor ID: " + tutorId);

        if (tutorId == -1) {
            Toast.makeText(this, "Invalid tutor ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvTutorName = findViewById(R.id.tvTutorName);
        tvBio = findViewById(R.id.tvBio);
        tvPrice = findViewById(R.id.tvPrice);
        tvRating = findViewById(R.id.tvRating);
        tvSessions = findViewById(R.id.tvSessions);
        tvDomain = findViewById(R.id.tvDomain);
        tvNoSubjects = findViewById(R.id.tvNoSubjects);
        tvNoAvailability = findViewById(R.id.tvNoAvailability);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        chipGroupSubjects = findViewById(R.id.chipGroupSubjects);
        cvVerifiedBadge = findViewById(R.id.cvVerifiedBadge);
        rvAvailability = findViewById(R.id.rvAvailability);
        rvReviews = findViewById(R.id.rvReviews);
        btnBookSession = findViewById(R.id.btnBookSession);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        rvAvailability.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(new ArrayList<>());
        rvReviews.setAdapter(reviewAdapter);

        fetchTutorProfile();
        fetchTutorReviews();

        btnBack.setOnClickListener(v -> finish());

        btnBookSession.setOnClickListener(v -> {
            if (currentTutor == null) return;
            Intent intent = new Intent(this, BookingRequestActivity.class);
            intent.putExtra("tutor_id", tutorId);
            intent.putExtra("tutor_name", currentTutor.getFullName());
            intent.putExtra("tutor_price", currentTutor.getPricingPerSession());
            startActivity(intent);
        });
    }

    private void fetchTutorProfile() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + sessionManager.getAccessToken();

        apiService.getTutorProfile(authHeader, tutorId).enqueue(new Callback<ApiResponse<TutorResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TutorResponse>> call, Response<ApiResponse<TutorResponse>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    displayTutor(response.body().getData());
                } else if (response.code() == 401) {
                    redirectToLogin();
                } else {
                    Toast.makeText(TutorProfileActivity.this, "Error fetching profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TutorResponse>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(TutorProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTutorReviews() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getTutorReviews(tutorId, 1).enqueue(new Callback<ApiResponse<com.kushal.uniassist.models.TutorReviewsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.kushal.uniassist.models.TutorReviewsResponse>> call, Response<ApiResponse<com.kushal.uniassist.models.TutorReviewsResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    displayReviews(response.body().getData().getResults());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.kushal.uniassist.models.TutorReviewsResponse>> call, Throwable t) {
                Log.e("TutorProfile", "Failed to fetch reviews", t);
            }
        });
    }

    private void displayReviews(List<com.kushal.uniassist.models.ReviewResponse> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            tvNoReviews.setVisibility(View.VISIBLE);
            rvReviews.setVisibility(View.GONE);
        } else {
            tvNoReviews.setVisibility(View.GONE);
            rvReviews.setVisibility(View.VISIBLE);
            reviewAdapter.updateList(reviews);
        }
    }

    private void displayTutor(TutorResponse tutor) {
        this.currentTutor = tutor;
        Log.d("TutorProfile", "Pricing: " + tutor.getPricingPerSession());
        Log.d("TutorProfile", "Name: " + tutor.getFullName());
        
        tvTutorName.setText(tutor.getFullName());
        tvBio.setText(tutor.getBio() != null ? tutor.getBio() : "No bio available");
        tvPrice.setText("NPR " + tutor.getPricingPerSession());
        tvRating.setText(tutor.getAverageRating() + " ★");
        tvSessions.setText(String.valueOf(tutor.getTotalSessionsDone()));

        String domain = tutor.getDomain();
        if (domain != null) {
            switch (domain) {
                case "academic":
                    tvDomain.setText("Academic");
                    break;
                case "skill":
                    tvDomain.setText("Skill-Based");
                    break;
                case "both":
                    tvDomain.setText("Academic & Skills");
                    break;
            }
        }

        if (tutor.isVerifiedBadge()) {
            cvVerifiedBadge.setVisibility(View.VISIBLE);
        }

        // Subjects & Skills
        chipGroupSubjects.removeAllViews();
        List<SubjectResponse> subjects = tutor.getSubjects();
        List<SkillResponse> skills = tutor.getSkills();

        if ((subjects == null || subjects.isEmpty()) && (skills == null || skills.isEmpty())) {
            tvNoSubjects.setVisibility(View.VISIBLE);
        } else {
            tvNoSubjects.setVisibility(View.GONE);
            if (subjects != null) {
                for (SubjectResponse s : subjects) {
                    Chip chip = new Chip(this);
                    chip.setText(s.getName());
                    chip.setChipBackgroundColorResource(R.color.accent_light);
                    chip.setTextColor(ContextCompat.getColor(this, R.color.primary));
                    chip.setClickable(false);
                    chipGroupSubjects.addView(chip);
                }
            }
            if (skills != null) {
                for (SkillResponse s : skills) {
                    Chip chip = new Chip(this);
                    chip.setText(s.getName());
                    chip.setChipBackgroundColorResource(R.color.accent_light);
                    chip.setTextColor(ContextCompat.getColor(this, R.color.primary));
                    chip.setClickable(false);
                    chipGroupSubjects.addView(chip);
                }
            }
        }

        // Availability
        List<AvailabilitySlot> slots = tutor.getAvailabilitySlots();
        if (slots == null || slots.isEmpty()) {
            tvNoAvailability.setVisibility(View.VISIBLE);
        } else {
            tvNoAvailability.setVisibility(View.GONE);
            AvailabilityAdapter availabilityAdapter = new AvailabilityAdapter(slots, null);
            rvAvailability.setAdapter(availabilityAdapter);
        }

        Glide.with(this)
                .load(tutor.getProfilePhotoUrl())
                .placeholder(R.drawable.ic_tutor_placeholder)
                .circleCrop()
                .into(ivProfilePhoto);
    }

    private void redirectToLogin() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

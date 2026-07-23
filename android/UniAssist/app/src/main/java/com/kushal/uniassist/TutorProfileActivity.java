package com.kushal.uniassist;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
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

    private ImageView ivProfileLarge;
    private TextView tvNameLarge, tvBio, tvSubjectsLarge, tvPriceLarge, tvRatingLarge, tvSessionsLarge;
    private Button btnBook;
    private SessionManager sessionManager;
    private int tutorId;

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

        if (tutorId == -1) {
            Toast.makeText(this, "Invalid tutor ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ivProfileLarge = findViewById(R.id.ivProfileLarge);
        tvNameLarge = findViewById(R.id.tvNameLarge);
        tvBio = findViewById(R.id.tvBio);
        tvSubjectsLarge = findViewById(R.id.tvSubjectsLarge);
        tvPriceLarge = findViewById(R.id.tvPriceLarge);
        tvRatingLarge = findViewById(R.id.tvRatingLarge);
        tvSessionsLarge = findViewById(R.id.tvSessionsLarge);
        btnBook = findViewById(R.id.btnBook);

        fetchTutorProfile();

        btnBook.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookingRequestActivity.class);
            intent.putExtra("tutor_id", tutorId);
            startActivity(intent);
        });
    }

    private void fetchTutorProfile() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + sessionManager.getAccessToken();

        apiService.getTutorProfile(authHeader, tutorId).enqueue(new Callback<TutorResponse>() {
            @Override
            public void onResponse(Call<TutorResponse> call, Response<TutorResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayTutor(response.body());
                } else if (response.code() == 401) {
                    redirectToLogin();
                } else {
                    Toast.makeText(TutorProfileActivity.this, "Error fetching profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TutorResponse> call, Throwable t) {
                Toast.makeText(TutorProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayTutor(TutorResponse tutor) {
        tvNameLarge.setText(tutor.getFullName());
        tvBio.setText(tutor.getBio());
        
        String subjects = "";
        if (tutor.getSubjects() != null && !tutor.getSubjects().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (SubjectResponse s : tutor.getSubjects()) {
                names.add(s.getName());
            }
            subjects = TextUtils.join(", ", names);
        } else if (tutor.getSkills() != null && !tutor.getSkills().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (SkillResponse s : tutor.getSkills()) {
                names.add(s.getName());
            }
            subjects = TextUtils.join(", ", names);
        }
        tvSubjectsLarge.setText(subjects);
        
        tvPriceLarge.setText("NPR " + tutor.getPricingPerSession() + " per hour");
        tvRatingLarge.setText(tutor.getAverageRating() + " ★");
        tvSessionsLarge.setText(String.valueOf(tutor.getTotalSessionsDone()));

        Glide.with(this)
                .load(tutor.getProfilePhotoUrl())
                .placeholder(R.drawable.ic_tutor_placeholder)
                .into(ivProfileLarge);
    }

    private void redirectToLogin() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

package com.kushal.uniassist;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kushal.uniassist.models.StudentProfileResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etGradeOrUniversity, etSubjectsOfInterest;
    private Button btnSaveProfile;
    private ProgressBar progressBar;
    private ImageView ivBack, ivProfile;
    private RelativeLayout rlChangePhoto, rlChangePassword, rlLogout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

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

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etGradeOrUniversity = findViewById(R.id.etGradeUniversity);
        etSubjectsOfInterest = findViewById(R.id.etSubjectsOfInterest);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.ivBack);
        ivProfile = findViewById(R.id.ivProfile);
        rlChangePhoto = findViewById(R.id.rlChangePhoto);
        rlChangePassword = findViewById(R.id.rlChangePassword);
        rlLogout = findViewById(R.id.rlLogout);

        if (ivBack != null) ivBack.setOnClickListener(v -> finish());
        
        preFillData();

        btnSaveProfile.setOnClickListener(v -> saveProfile());
        
        rlChangePhoto.setOnClickListener(v -> Toast.makeText(this, "Photo upload coming soon", Toast.LENGTH_SHORT).show());
        rlChangePassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        rlLogout.setOnClickListener(v -> logout());
    }

    private void preFillData() {
        if (sessionManager != null) {
            etFullName.setText(sessionManager.getFullName());
            etEmail.setText(sessionManager.getEmail());
            // Grade and subjects might need to be fetched from a "get profile" API if not in session
        }
    }

    private void logout() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void saveProfile() {
        String grade = etGradeOrUniversity.getText().toString().trim();
        String subjects = etSubjectsOfInterest.getText().toString().trim();

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnSaveProfile.setEnabled(false);

        String token = sessionManager.getAccessToken();
        String authHeader = "Bearer " + token;

        RequestBody gradeBody = RequestBody.create(grade, MediaType.parse("text/plain"));
        RequestBody subjectsBody = RequestBody.create(subjects, MediaType.parse("text/plain"));

        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.updateStudentProfile(authHeader, gradeBody, subjectsBody)
                .enqueue(new Callback<StudentProfileResponse>() {
                    @Override
                    public void onResponse(Call<StudentProfileResponse> call, Response<StudentProfileResponse> response) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        btnSaveProfile.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(EditProfileActivity.this,
                                    "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String errorMsg = "Update failed.";
                            if (response.errorBody() != null) {
                                try {
                                    errorMsg = response.errorBody().string();
                                } catch (Exception ignored) {}
                            }
                            Toast.makeText(EditProfileActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<StudentProfileResponse> call, Throwable t) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        btnSaveProfile.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}

package com.kushal.uniassist;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kushal.uniassist.models.StudentProfileResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etGradeOrUniversity, etSubjectsOfInterest;
    private Button btnSaveProfile;
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new SessionManager(this);

        etGradeOrUniversity = findViewById(R.id.etGradeOrUniversity);
        etSubjectsOfInterest = findViewById(R.id.etSubjectsOfInterest);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressBar);

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String grade = etGradeOrUniversity.getText().toString().trim();
        String subjects = etSubjectsOfInterest.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);
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
                        progressBar.setVisibility(View.GONE);
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
                        progressBar.setVisibility(View.GONE);
                        btnSaveProfile.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}

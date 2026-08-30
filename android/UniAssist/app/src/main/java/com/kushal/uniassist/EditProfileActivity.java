package com.kushal.uniassist;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.StudentProfileRequest;
import com.kushal.uniassist.models.StudentProfileResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_IMAGE_PERMISSION = 101;

    private EditText etFullName, etEmail, etGradeOrUniversity, etSubjectsOfInterest;
    private Button btnSaveProfile;
    private ProgressBar progressBar;
    private ImageView ivBack, ivProfilePhoto;
    private TextView tvAvatarLetter;
    private FrameLayout flCameraOverlay;
    private RelativeLayout rlChangePassword, rlLogout;
    private SessionManager sessionManager;
    private Uri selectedImageUri;

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
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvAvatarLetter = findViewById(R.id.tvAvatarLetter);
        flCameraOverlay = findViewById(R.id.flCameraOverlay);
        rlChangePassword = findViewById(R.id.rlChangePassword);
        rlLogout = findViewById(R.id.rlLogout);

        if (ivBack != null) ivBack.setOnClickListener(v -> finish());
        
        setupAvatar();
        loadProfile();

        btnSaveProfile.setOnClickListener(v -> saveProfile());
        
        flCameraOverlay.setOnClickListener(v -> checkPermissionsAndPickImage());
        rlChangePassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        rlLogout.setOnClickListener(v -> logout());
    }

    private void setupAvatar() {
        String name = sessionManager.getFullName();
        if (name != null && !name.isEmpty()) {
            tvAvatarLetter.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            tvAvatarLetter.setText("S");
        }
    }

    private void loadProfile() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        String authHeader = "Bearer " + sessionManager.getAccessToken();
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getStudentProfile(authHeader).enqueue(new Callback<ApiResponse<StudentProfileResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StudentProfileResponse>> call, Response<ApiResponse<StudentProfileResponse>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    StudentProfileResponse student = response.body().getData();
                    if (student != null) {
                        Log.d("EditProfile", "Grade: " + student.getGradeOrUniversity());
                        Log.d("EditProfile", "Subjects: " + student.getSubjectsOfInterest());

                        etFullName.setText(student.getFullName());
                        etEmail.setText(sessionManager.getEmail());
                        etGradeOrUniversity.setText(student.getGradeOrUniversity());
                        etSubjectsOfInterest.setText(student.getSubjectsOfInterest());

                        if (student.getProfilePhotoUrl() != null && !student.getProfilePhotoUrl().isEmpty()) {
                            tvAvatarLetter.setVisibility(View.GONE);
                            Glide.with(EditProfileActivity.this)
                                    .load(student.getProfilePhotoUrl())
                                    .circleCrop()
                                    .into(ivProfilePhoto);
                        }
                    }
                } else {
                    preFillFromSession();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StudentProfileResponse>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                preFillFromSession();
            }
        });
    }

    private void preFillFromSession() {
        if (sessionManager != null) {
            etFullName.setText(sessionManager.getFullName());
            etEmail.setText(sessionManager.getEmail());
        }
    }

    private void logout() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void checkPermissionsAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_IMAGE_PERMISSION);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_IMAGE_PERMISSION);
                return;
            }
        }
        openImagePicker();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission needed to select photo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            Log.d("EditProfile", "Image selected: " + selectedImageUri);
            Glide.with(this).load(selectedImageUri).circleCrop().into(ivProfilePhoto);
            tvAvatarLetter.setVisibility(View.GONE);
            Toast.makeText(this, "Photo selected. Will be uploaded on save.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfile() {
        String grade = etGradeOrUniversity.getText().toString().trim();
        String subjects = etSubjectsOfInterest.getText().toString().trim();

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnSaveProfile.setEnabled(false);

        String token = sessionManager.getAccessToken();
        String authHeader = "Bearer " + token;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        if (selectedImageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, bos);
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), bos.toByteArray());
                MultipartBody.Part photoPart = MultipartBody.Part.createFormData("profile_photo", "photo.jpg", requestFile);

                RequestBody gradeBody = RequestBody.create(grade, MediaType.parse("text/plain"));
                RequestBody subjectsBody = RequestBody.create(subjects, MediaType.parse("text/plain"));

                apiService.updateStudentProfile(authHeader, gradeBody, subjectsBody, photoPart)
                        .enqueue(new Callback<ApiResponse<StudentProfileResponse>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<StudentProfileResponse>> call, Response<ApiResponse<StudentProfileResponse>> response) {
                                handleSaveResponse(response);
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<StudentProfileResponse>> call, Throwable t) {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                btnSaveProfile.setEnabled(true);
                                Toast.makeText(EditProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (IOException e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnSaveProfile.setEnabled(true);
                e.printStackTrace();
            }
        } else {
            StudentProfileRequest request = new StudentProfileRequest(grade, subjects);
            apiService.updateStudentProfile(authHeader, request)
                    .enqueue(new Callback<ApiResponse<StudentProfileResponse>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<StudentProfileResponse>> call, Response<ApiResponse<StudentProfileResponse>> response) {
                            handleSaveResponse(response);
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<StudentProfileResponse>> call, Throwable t) {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            btnSaveProfile.setEnabled(true);
                            Toast.makeText(EditProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void handleSaveResponse(Response<ApiResponse<StudentProfileResponse>> response) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        btnSaveProfile.setEnabled(true);

        if (response.isSuccessful() && response.body() != null) {
            Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            String errorMsg = "Update failed.";
            try {
                if (response.errorBody() != null) {
                    errorMsg = response.errorBody().string();
                }
            } catch (Exception ignored) {}
            Toast.makeText(EditProfileActivity.this, errorMsg, Toast.LENGTH_LONG).show();
        }
    }
}

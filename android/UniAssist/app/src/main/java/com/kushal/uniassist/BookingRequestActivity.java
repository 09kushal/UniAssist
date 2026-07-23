package com.kushal.uniassist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.BookingRequest;
import com.kushal.uniassist.models.BookingResponse;
import com.kushal.uniassist.models.TutorResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingRequestActivity extends AppCompatActivity {

    private EditText etSubjectOrSkill, etMessage;
    private TextView tvTutorName, tvPriceLabel, tvSelectedDate, tvStartTime, tvEndTime;
    private ImageView ivTutorPhoto, ivBack;
    private LinearLayout llSelectDate, llSelectStartTime, llSelectEndTime;
    private Button btnSendRequest;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private int tutorId;

    private String selectedDate = "";
    private String selectedStartTime = "09:00:00";
    private String selectedEndTime = "11:00:00";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_request);

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
            Toast.makeText(this, "Invalid tutor", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ivBack = findViewById(R.id.ivBack);
        ivTutorPhoto = findViewById(R.id.ivTutorPhoto);
        tvTutorName = findViewById(R.id.tvTutorName);
        tvPriceLabel = findViewById(R.id.tvPriceLabel);
        
        etSubjectOrSkill = findViewById(R.id.etSubjectOrSkill);
        etMessage = findViewById(R.id.etMessage);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);
        
        llSelectDate = findViewById(R.id.llSelectDate);
        llSelectStartTime = findViewById(R.id.llSelectStartTime);
        llSelectEndTime = findViewById(R.id.llSelectEndTime);
        btnSendRequest = findViewById(R.id.btnSendRequest);
        progressBar = findViewById(R.id.progressBar);

        if (ivBack != null) ivBack.setOnClickListener(v -> finish());
        if (llSelectDate != null) llSelectDate.setOnClickListener(v -> showDatePicker());
        if (llSelectStartTime != null) llSelectStartTime.setOnClickListener(v -> showTimePicker(true));
        if (llSelectEndTime != null) llSelectEndTime.setOnClickListener(v -> showTimePicker(false));
        if (btnSendRequest != null) btnSendRequest.setOnClickListener(v -> sendBookingRequest());

        fetchTutorMinimal();
    }

    private void fetchTutorMinimal() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + sessionManager.getAccessToken();

        apiService.getTutorProfile(authHeader, tutorId).enqueue(new Callback<TutorResponse>() {
            @Override
            public void onResponse(Call<TutorResponse> call, Response<TutorResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TutorResponse tutor = response.body();
                    if (tvTutorName != null) tvTutorName.setText(tutor.getFullName());
                    if (tvPriceLabel != null) tvPriceLabel.setText("NPR " + tutor.getPricingPerSession() + "/session");
                    if (ivTutorPhoto != null) {
                        Glide.with(BookingRequestActivity.this)
                                .load(tutor.getProfilePhotoUrl())
                                .circleCrop()
                                .placeholder(R.drawable.ic_tutor_placeholder)
                                .into(ivTutorPhoto);
                    }
                }
            }

            @Override
            public void onFailure(Call<TutorResponse> call, Throwable t) {}
        });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year1, month1 + 1, dayOfMonth);
            if (tvSelectedDate != null) tvSelectedDate.setText(selectedDate);
        }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showTimePicker(boolean isStart) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d:00", hourOfDay, minute1);
            String displayTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
            if (isStart) {
                selectedStartTime = time;
                if (tvStartTime != null) tvStartTime.setText(displayTime);
            } else {
                selectedEndTime = time;
                if (tvEndTime != null) tvEndTime.setText(displayTime);
            }
        }, hour, minute, true);
        timePickerDialog.show();
    }

    private void sendBookingRequest() {
        String subjectOrSkill = etSubjectOrSkill.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (subjectOrSkill.isEmpty() || selectedDate.isEmpty()) {
            Toast.makeText(this, "Please fill subject and date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (btnSendRequest != null) btnSendRequest.setEnabled(false);

        try {
            if (sessionManager == null) {
                sessionManager = new SessionManager(this);
            }

            String accessToken = sessionManager.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            String token = "Bearer " + accessToken;

            Log.d("BookingDebug", "Token being sent: " + token);
            Log.d("BookingDebug", "Token length: " + token.length());
            Log.d("Booking", "Tutor ID: " + tutorId);
            Log.d("Booking", "Subject: " + subjectOrSkill);
            Log.d("Booking", "Date: " + selectedDate);
            Log.d("Booking", "Start: " + selectedStartTime);
            Log.d("Booking", "End: " + selectedEndTime);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);

            BookingRequest request = new BookingRequest(
                tutorId, 
                subjectOrSkill, 
                selectedDate, 
                selectedStartTime, 
                selectedEndTime, 
                message
            );

            apiService.createBooking(token, request).enqueue(new Callback<ApiResponse<BookingResponse>>() {
                @Override
                public void onResponse(Call<ApiResponse<BookingResponse>> call, Response<ApiResponse<BookingResponse>> response) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (btnSendRequest != null) btnSendRequest.setEnabled(true);

                    Log.d("Booking", "Response code: " + response.code());

                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().isSuccess()) {
                            Toast.makeText(BookingRequestActivity.this, "Booking request sent! Waiting for tutor approval.", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(BookingRequestActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                            Log.e("Booking", "Error body: " + errorBody);
                            Toast.makeText(BookingRequestActivity.this, "Failed: " + errorBody, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e("Booking", e.getMessage());
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<BookingResponse>> call, Throwable t) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (btnSendRequest != null) btnSendRequest.setEnabled(true);
                    Log.e("Booking", "Network failure: " + t.getMessage());
                    Toast.makeText(BookingRequestActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (btnSendRequest != null) btnSendRequest.setEnabled(true);
            Log.e("Booking", "Catch error: " + e.getMessage());
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage("Booking request sent successfully! The tutor will respond soon.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(this, StudentDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                })
                .setCancelable(false)
                .show();
    }

    private void redirectToLogin() {
        if (sessionManager != null) sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

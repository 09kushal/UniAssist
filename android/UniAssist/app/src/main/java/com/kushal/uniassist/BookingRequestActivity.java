package com.kushal.uniassist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
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
import com.kushal.uniassist.models.AvailabilitySlot;
import com.kushal.uniassist.models.BookingRequest;
import com.kushal.uniassist.models.BookingResponse;
import com.kushal.uniassist.models.TutorResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingRequestActivity extends AppCompatActivity {

    private EditText etSubjectOrSkill, etMessage;
    private TextView tvTutorName, tvPriceLabel, tvSelectedDate, tvNoSlots;
    private ImageView ivBack;
    private LinearLayout llSelectDate, llAvailabilitySlots;
    private Button btnSendRequest;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private ApiService apiService;
    private int tutorId;

    private String selectedDate = "";
    private String selectedStartTime = "09:00:00";
    private String selectedEndTime = "11:00:00";
    
    private int targetDayOfWeek = -1;
    private AvailabilitySlot selectedSlot = null;
    private List<AvailabilitySlot> availabilitySlots = new ArrayList<>();

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
        apiService = ApiClient.getClient().create(ApiService.class);
        tutorId = getIntent().getIntExtra("tutor_id", -1);

        if (tutorId == -1) {
            Toast.makeText(this, "Invalid tutor", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ivBack = findViewById(R.id.ivBack);
        tvTutorName = findViewById(R.id.tvTutorName);
        tvPriceLabel = findViewById(R.id.tvPriceLabel);
        
        etSubjectOrSkill = findViewById(R.id.etSubjectOrSkill);
        etMessage = findViewById(R.id.etMessage);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        
        llSelectDate = findViewById(R.id.llSelectDate);
        llAvailabilitySlots = findViewById(R.id.llAvailabilitySlots);
        tvNoSlots = findViewById(R.id.tvNoSlots);
        btnSendRequest = findViewById(R.id.btnSendRequest);
        progressBar = findViewById(R.id.progressBar);

        if (ivBack != null) ivBack.setOnClickListener(v -> finish());
        if (llSelectDate != null) llSelectDate.setOnClickListener(v -> showDatePicker());
        if (btnSendRequest != null) btnSendRequest.setOnClickListener(v -> sendBookingRequest());

        setupTutorInfo();
        loadTutorAvailability();
    }

    private void loadTutorAvailability() {
        String token = "Bearer " + sessionManager.getAccessToken();

        apiService.getTutorProfile(token, tutorId).enqueue(new Callback<ApiResponse<TutorResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TutorResponse>> call, Response<ApiResponse<TutorResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    TutorResponse tutor = response.body().getData();
                    availabilitySlots = tutor.getAvailabilitySlots();

                    if (availabilitySlots != null && !availabilitySlots.isEmpty()) {
                        showAvailabilitySlots(availabilitySlots);
                    } else {
                        targetDayOfWeek = -1;
                        tvNoSlots.setVisibility(View.VISIBLE);
                        tvNoSlots.setText("Tutor hasn't set availability yet.\nYou can still propose a time.");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TutorResponse>> call, Throwable t) {
                Log.e("Booking", "Failed to load availability");
            }
        });
    }

    private void showAvailabilitySlots(List<AvailabilitySlot> slots) {
        llAvailabilitySlots.removeAllViews();

        for (AvailabilitySlot slot : slots) {
            TextView chip = new TextView(this);

            String slotText = slot.getDayOfWeek() + " " +
                    (slot.getStartTime().length() >= 5 ? slot.getStartTime().substring(0, 5) : slot.getStartTime()) +
                    "-" +
                    (slot.getEndTime().length() >= 5 ? slot.getEndTime().substring(0, 5) : slot.getEndTime());

            chip.setText(slotText);
            chip.setPadding(24, 12, 24, 12);
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.accent));
            chip.setTextSize(13f);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 8, 0);
            chip.setLayoutParams(params);

            chip.setOnClickListener(v -> {
                // Reset all chips
                for (int i = 0; i < llAvailabilitySlots.getChildCount(); i++) {
                    View child = llAvailabilitySlots.getChildAt(i);
                    child.setBackgroundResource(R.drawable.bg_chip_unselected);
                    ((TextView) child).setTextColor(ContextCompat.getColor(this, R.color.accent));
                }

                // Highlight selected chip
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(Color.WHITE);

                // Auto fill times
                selectedSlot = slot;
                selectedStartTime = slot.getStartTime();
                selectedEndTime = slot.getEndTime();
                
                restrictDatePickerToDay(slot.getDayOfWeek());

                Toast.makeText(this, "Selected: " + slotText + "\nPick a " + slot.getDayOfWeek() + " date", Toast.LENGTH_SHORT).show();
            });

            llAvailabilitySlots.addView(chip);
        }
    }

    private void setupTutorInfo() {
        String tutorName = getIntent().getStringExtra("tutor_name");
        String tutorPrice = getIntent().getStringExtra("tutor_price");

        Log.d("BookingDebug", "Tutor name: " + tutorName);
        Log.d("BookingDebug", "Tutor price: " + tutorPrice);

        if (tvTutorName != null) {
            if (tutorName != null && !tutorName.isEmpty()) {
                tvTutorName.setText(tutorName);
            } else {
                tvTutorName.setText("Tutor");
            }
        }
        
        if (tvPriceLabel != null) {
            if (tutorPrice != null && 
                !tutorPrice.isEmpty() && 
                !tutorPrice.equals("0.00") &&
                !tutorPrice.equals("0")) {
                tvPriceLabel.setText("NPR " + tutorPrice + " per session");
                tvPriceLabel.setVisibility(View.VISIBLE);
            } else {
                tvPriceLabel.setText("Free / Price not set by tutor");
                tvPriceLabel.setVisibility(View.VISIBLE);
            }
        }
    }

    private void restrictDatePickerToDay(String dayOfWeek) {
        // Convert day abbreviation to Calendar constant
        int targetDay;
        switch (dayOfWeek) {
            case "Mon": targetDay = Calendar.MONDAY; break;
            case "Tue": targetDay = Calendar.TUESDAY; break;
            case "Wed": targetDay = Calendar.WEDNESDAY; break;
            case "Thu": targetDay = Calendar.THURSDAY; break;
            case "Fri": targetDay = Calendar.FRIDAY; break;
            case "Sat": targetDay = Calendar.SATURDAY; break;
            case "Sun": targetDay = Calendar.SUNDAY; break;
            default: targetDay = -1;
        }

        this.targetDayOfWeek = targetDay;

        // Find next occurrence of that day
        Calendar nextOccurrence = Calendar.getInstance();
        
        boolean canSelectToday = false;
        if (nextOccurrence.get(Calendar.DAY_OF_WEEK) == targetDay) {
            try {
                String[] parts = selectedStartTime.split(":");
                int hour = Integer.parseInt(parts[0]);
                int min = Integer.parseInt(parts[1]);
                Calendar slotTime = Calendar.getInstance();
                slotTime.set(Calendar.HOUR_OF_DAY, hour);
                slotTime.set(Calendar.MINUTE, min);
                if (slotTime.after(nextOccurrence)) {
                    canSelectToday = true;
                }
            } catch (Exception e) {}
        }

        if (!canSelectToday) {
            nextOccurrence.add(Calendar.DAY_OF_MONTH, 1);
            while (nextOccurrence.get(Calendar.DAY_OF_WEEK) != targetDay) {
                nextOccurrence.add(Calendar.DAY_OF_MONTH, 1);
            }
        }

        // Update date field to show next valid date
        selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                nextOccurrence.get(Calendar.YEAR),
                nextOccurrence.get(Calendar.MONTH) + 1,
                nextOccurrence.get(Calendar.DAY_OF_MONTH));

        if (tvSelectedDate != null) {
            tvSelectedDate.setText(selectedDate);
        }

        Toast.makeText(this, "Date auto-set to next " + dayOfWeek + ". Tap date to change.", Toast.LENGTH_SHORT).show();
    }

    private void showDatePicker() {
        Calendar today = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            // Validate selected day matches slot
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day);

            if (targetDayOfWeek != -1 && selected.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
                // Find day name for error message
                String[] dayNames = {"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
                String requiredDay = dayNames[targetDayOfWeek];

                Toast.makeText(this, "Please select a " + requiredDay + " — tutor is only available on " + requiredDay + "s", Toast.LENGTH_LONG).show();
                return;
            }

            Calendar now = Calendar.getInstance();
            if (year == now.get(Calendar.YEAR) && month == now.get(Calendar.MONTH) && day == now.get(Calendar.DAY_OF_MONTH)) {
                try {
                    String[] parts = selectedStartTime.split(":");
                    int hour = Integer.parseInt(parts[0]);
                    int min = Integer.parseInt(parts[1]);
                    Calendar slotTime = Calendar.getInstance();
                    slotTime.set(Calendar.HOUR_OF_DAY, hour);
                    slotTime.set(Calendar.MINUTE, min);
                    if (slotTime.before(now)) {
                        Toast.makeText(this, "This time slot has already passed today. Please select a future date.", Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (Exception e) {}
            }

            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            if (tvSelectedDate != null) tvSelectedDate.setText(selectedDate);

        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));

        // Set minimum date to today
        dialog.getDatePicker().setMinDate(today.getTimeInMillis());

        dialog.show();
    }

    private void sendBookingRequest() {
        String subjectOrSkill = etSubjectOrSkill.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (subjectOrSkill.isEmpty() || selectedDate.isEmpty()) {
            Toast.makeText(this, "Please fill subject and date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSlot == null && availabilitySlots != null && !availabilitySlots.isEmpty()) {
            Toast.makeText(this, "Please select an available time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use selected slot times if available, otherwise defaults
        String startTime = selectedSlot != null ? selectedSlot.getStartTime() : "09:00:00";
        String endTime = selectedSlot != null ? selectedSlot.getEndTime() : "11:00:00";

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

            Log.d("BookingDebug", "=== BOOKING REQUEST ===");
            Log.d("BookingDebug", "Tutor ID: " + tutorId);
            Log.d("BookingDebug", "Subject: " + subjectOrSkill);
            Log.d("BookingDebug", "Date: " + selectedDate);
            Log.d("BookingDebug", "Start: " + startTime);
            Log.d("BookingDebug", "End: " + endTime);
            Log.d("BookingDebug", "Message: " + message);
            Log.d("BookingDebug", "Token exists: " + (sessionManager.getAccessToken() != null));

            ApiService apiService = ApiClient.getClient().create(ApiService.class);

            BookingRequest request = new BookingRequest(
                tutorId, 
                subjectOrSkill, 
                selectedDate, 
                startTime, 
                endTime,
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
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                            Log.e("BookingDebug", "Error code: " + response.code());
                            Log.e("BookingDebug", "Error body: " + errorBody);
                            Toast.makeText(BookingRequestActivity.this, "Error: " + errorBody, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e("BookingDebug", "Parse error: " + e.getMessage());
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

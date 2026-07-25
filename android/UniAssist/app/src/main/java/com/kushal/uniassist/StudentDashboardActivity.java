package com.kushal.uniassist;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.PaginatedResponse;
import com.kushal.uniassist.models.BookingResponse;
import com.kushal.uniassist.models.TutorResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvGreeting, tvBookingCount, tvSessionsDone, tvPendingCount, tvLogout, tvAvatarLetter, tvNoBookings, tvNotifCount;
    private View vNotifBadge;
    private android.widget.FrameLayout flAvatar;
    private RelativeLayout rlNotifications;
    private LinearLayout llSearch;
    private CardView cardFindTutor, cardMyBookings, cardNotifications, cardMyProfile;
    private RecyclerView rvFeaturedTutors, rvRecentBookings;
    private BookingAdapter bookingAdapter;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

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

        tvGreeting = findViewById(R.id.tvGreeting);
        tvBookingCount = findViewById(R.id.tvBookingCount);
        tvSessionsDone = findViewById(R.id.tvSessionsDone);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        vNotifBadge = findViewById(R.id.vNotifBadge);
        
        flAvatar = findViewById(R.id.flAvatar);
        tvAvatarLetter = findViewById(R.id.tvAvatarLetter);
        
        rlNotifications = findViewById(R.id.rlNotifications);
        llSearch = findViewById(R.id.llSearch);
        
        cardFindTutor = findViewById(R.id.cardFindTutor);
        cardMyBookings = findViewById(R.id.cardMyBookings);
        cardNotifications = findViewById(R.id.cardNotifications);
        cardMyProfile = findViewById(R.id.cardMyProfile);
        tvNotifCount = findViewById(R.id.tvNotifCount);
        
        rvFeaturedTutors = findViewById(R.id.rvFeaturedTutors);
        rvRecentBookings = findViewById(R.id.rvRecentBookings);
        tvNoBookings = findViewById(R.id.tvNoBookings);
        tvLogout = findViewById(R.id.tvLogout);

        setupAvatar();
        setupRecentBookings();
        setupGreeting();
        setupClickListeners();
        loadDashboardData();
    }

    private void setupAvatar() {
        String name = sessionManager.getFullName();
        if (name != null && !name.isEmpty()) {
            tvAvatarLetter.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            tvAvatarLetter.setText("S");
        }
    }

    private void setupRecentBookings() {
        rvRecentBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingAdapter = new BookingAdapter(booking -> {
            Toast.makeText(this, "Booking: " + booking.getSubjectOrSkill(), Toast.LENGTH_SHORT).show();
        });
        rvRecentBookings.setAdapter(bookingAdapter);
    }

    private void setupGreeting() {
        String name = sessionManager.getFullName();
        if (name == null) name = "Student";
        
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) greeting = "Good Morning";
        else if (hour < 17) greeting = "Good Afternoon";
        else greeting = "Good Evening";
        
        tvGreeting.setText(greeting + ", " + name + "!");
    }

    private void setupClickListeners() {
        llSearch.setOnClickListener(v -> {
            Intent intent = new Intent(this, TutorListActivity.class);
            intent.putExtra("domain", "");
            startActivity(intent);
        });
        rlNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        flAvatar.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
        
        cardFindTutor.setOnClickListener(v -> {
            Intent intent = new Intent(this, TutorListActivity.class);
            intent.putExtra("domain", "");
            startActivity(intent);
        });
        
        cardMyBookings.setOnClickListener(v -> {
            startActivity(new Intent(this, MyBookingsActivity.class));
        });

        cardNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        cardMyProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });
        
        tvLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadDashboardData() {
        String authHeader = "Bearer " + sessionManager.getAccessToken();

        // 1. Unread Notification Count
        apiService.getUnreadNotificationCount(authHeader).enqueue(new Callback<ApiResponse<Integer>>() {
            @Override
            public void onResponse(Call<ApiResponse<Integer>> call, Response<ApiResponse<Integer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int count = response.body().getData() != null ? response.body().getData() : 0;
                    vNotifBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                    if (tvNotifCount != null) {
                        tvNotifCount.setText(count + " unread");
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Integer>> call, Throwable t) {}
        });

        // 2. Featured Tutors
        apiService.getTutorList(authHeader, null, 1).enqueue(new Callback<ApiResponse<PaginatedResponse<TutorResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaginatedResponse<TutorResponse>>> call, Response<ApiResponse<PaginatedResponse<TutorResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    setupFeaturedTutors(response.body().getData().getResults());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<PaginatedResponse<TutorResponse>>> call, Throwable t) {}
        });

        // 3. Booking Stats
        apiService.getMyBookings(authHeader, 1).enqueue(new Callback<ApiResponse<PaginatedResponse<BookingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaginatedResponse<BookingResponse>>> call, Response<ApiResponse<PaginatedResponse<BookingResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<BookingResponse> bookings = response.body().getData().getResults();
                    tvBookingCount.setText(String.valueOf(bookings.size()));
                    
                    int done = 0;
                    int pending = 0;
                    for (BookingResponse b : bookings) {
                        if ("completed".equalsIgnoreCase(b.getBookingStatus())) done++;
                        else if ("pending".equalsIgnoreCase(b.getBookingStatus())) pending++;
                    }
                    tvSessionsDone.setText(String.valueOf(done));
                    tvPendingCount.setText(String.valueOf(pending));

                    // Show recent bookings
                    if (bookings.isEmpty()) {
                        tvNoBookings.setVisibility(View.VISIBLE);
                        rvRecentBookings.setVisibility(View.GONE);
                    } else {
                        tvNoBookings.setVisibility(View.GONE);
                        rvRecentBookings.setVisibility(View.VISIBLE);
                        List<BookingResponse> recent = bookings.size() > 5 ? bookings.subList(0, 5) : bookings;
                        bookingAdapter.updateList(recent);
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<PaginatedResponse<BookingResponse>>> call, Throwable t) {}
        });
    }

    private void setupFeaturedTutors(List<TutorResponse> tutors) {
        rvFeaturedTutors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        FeaturedTutorAdapter adapter = new FeaturedTutorAdapter(tutors, tutor -> {
            Intent intent = new Intent(this, TutorProfileActivity.class);
            intent.putExtra("tutor_id", tutor.getId());
            startActivity(intent);
        });
        rvFeaturedTutors.setAdapter(adapter);
    }
}

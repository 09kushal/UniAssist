package com.kushal.uniassist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.BookingResponse;
import com.kushal.uniassist.models.PaginatedResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import com.kushal.uniassist.models.TutorResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyBookingsActivity extends AppCompatActivity {

    private RecyclerView rvBookings;
    private BookingDetailAdapter bookingDetailAdapter;
    private List<BookingResponse> allBookings = new ArrayList<>();
    private List<BookingResponse> filteredBookings = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;
    private View llEmptyState;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private ChipGroup chipGroupFilters;
    private SessionManager sessionManager;
    private ApiService apiService;
    private ImageView ivBack;

    private static final int PAY_REQUEST_CODE = 1001;
    private String currentFilter = "all";
    private int currentPage = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        ivBack = findViewById(R.id.ivBack);
        rvBookings = findViewById(R.id.rvBookings);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);

        ivBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        setupFilters();

        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            fetchBookings();
        });

        fetchBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPendingReview();
    }

    private void checkPendingReview() {
        int bookingId = sessionManager.getPendingReviewBookingId();
        if (bookingId != -1) {
            // Clear flag immediately so it doesn't repeat
            sessionManager.clearPendingReview();

            // Check if already reviewed
            String authHeader = "Bearer " + sessionManager.getAccessToken();
            apiService.checkReview(authHeader, bookingId).enqueue(new Callback<ApiResponse<com.kushal.uniassist.models.ReviewCheckResponse>>() {
                @Override
                public void onResponse(Call<ApiResponse<com.kushal.uniassist.models.ReviewCheckResponse>> call, Response<ApiResponse<com.kushal.uniassist.models.ReviewCheckResponse>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        if (!response.body().getData().isHasReviewed()) {
                            // Show review screen
                            Intent intent = new Intent(MyBookingsActivity.this, SubmitReviewActivity.class);
                            intent.putExtra("booking_id", bookingId);
                            startActivity(intent);
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<com.kushal.uniassist.models.ReviewCheckResponse>> call, Throwable t) {
                    Log.e("MyBookings", "Review check failed", t);
                }
            });
        }
    }

    private void setupRecyclerView() {
        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingDetailAdapter = new BookingDetailAdapter(new BookingDetailAdapter.OnBookingActionListener() {
            @Override
            public void onCancel(BookingResponse booking) {
                cancelBooking(booking);
            }

            @Override
            public void onJoin(BookingResponse booking) {
                String userName = sessionManager.getFullName();
                if (userName == null) userName = "Student";

                // Set pending review flag
                sessionManager.setPendingReviewBookingId(booking.getId());

                Intent intent = new Intent(MyBookingsActivity.this, JoinSessionActivity.class);
                intent.putExtra("booking_id", booking.getId());
                intent.putExtra("user_full_name", userName);
                intent.putExtra("user_role", "student");
                startActivity(intent);
            }

            @Override
            public void onPay(BookingResponse booking) {
                Intent intent = new Intent(MyBookingsActivity.this, EsewaPaymentActivity.class);
                intent.putExtra("booking_id", booking.getId());
                intent.putExtra("amount", booking.getTutor() != null ? booking.getTutor().getPricingPerSession() : "0");
                startActivityForResult(intent, PAY_REQUEST_CODE);
            }

            @Override
            public void onPaymentCompleted() {
                currentPage = 1;
                fetchBookings();
            }

            @Override
            public void onReport(BookingResponse booking) {
                // To be implemented
                Toast.makeText(MyBookingsActivity.this, "Reporting not implemented yet", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReschedule(BookingResponse booking) {
                // To be implemented
                Toast.makeText(MyBookingsActivity.this, "Rescheduling not implemented yet", Toast.LENGTH_SHORT).show();
            }
        });
        rvBookings.setAdapter(bookingDetailAdapter);
    }

    private void setupFilters() {
        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipPending) currentFilter = "pending";
            else if (checkedId == R.id.chipAccepted) currentFilter = "accepted";
            else if (checkedId == R.id.chipCompleted) currentFilter = "completed";
            else currentFilter = "all";
            
            applyFilter(currentFilter);
        });
    }

    private void fetchBookings() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        String token = "Bearer " + sessionManager.getAccessToken();
        
        apiService.getMyBookings(token, currentPage).enqueue(new Callback<ApiResponse<PaginatedResponse<BookingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaginatedResponse<BookingResponse>>> call, Response<ApiResponse<PaginatedResponse<BookingResponse>>> response) {
                swipeRefresh.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    PaginatedResponse<BookingResponse> data = response.body().getData();
                    List<BookingResponse> bookings = data.getResults();
                    
                    if (bookings != null && !bookings.isEmpty()) {
                        if (currentPage == 1) {
                            allBookings.clear();
                        }
                        allBookings.addAll(bookings);
                        applyFilter(currentFilter);
                        rvBookings.setVisibility(View.VISIBLE);
                        llEmptyState.setVisibility(View.GONE);
                    } else {
                        if (currentPage == 1) {
                            allBookings.clear();
                            applyFilter(currentFilter);
                            llEmptyState.setVisibility(View.VISIBLE);
                            rvBookings.setVisibility(View.GONE);
                            tvEmpty.setText("No bookings yet.\nFind a tutor to get started!");
                        }
                    }
                } else if (response.code() == 401) {
                    redirectToLogin();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e("MyBookings", "Error: " + errorBody);
                        Toast.makeText(MyBookingsActivity.this, "Failed to load bookings", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e("MyBookings", e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PaginatedResponse<BookingResponse>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
                Log.e("MyBookings", "Network error: " + t.getMessage());
                Toast.makeText(MyBookingsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyFilter(String filter) {
        filteredBookings.clear();
        if (filter.equals("all")) {
            filteredBookings.addAll(allBookings);
        } else {
            for (BookingResponse booking : allBookings) {
                if (booking.getBookingStatus().equalsIgnoreCase(filter)) {
                    filteredBookings.add(booking);
                }
            }
        }
        bookingDetailAdapter.updateList(filteredBookings);
        
        if (filteredBookings.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvBookings.setVisibility(View.GONE);
            if (filter.equals("all")) {
                tvEmpty.setText("No bookings yet.\nFind a tutor to get started!");
            } else {
                tvEmpty.setText("No " + filter + " bookings found.");
            }
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvBookings.setVisibility(View.VISIBLE);
        }
    }

    private void cancelBooking(BookingResponse booking) {
        Toast.makeText(this, "Cancelling booking #" + booking.getId(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PAY_REQUEST_CODE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Payment successful! Refreshing bookings...", Toast.LENGTH_SHORT).show();
            currentPage = 1;
            fetchBookings();
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

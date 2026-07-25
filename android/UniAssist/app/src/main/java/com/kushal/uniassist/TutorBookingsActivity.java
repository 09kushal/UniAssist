package com.kushal.uniassist;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.BookingActionRequest;
import com.kushal.uniassist.models.BookingResponse;
import com.kushal.uniassist.models.PaginatedResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TutorBookingsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private ChipGroup chipGroupFilters;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvBookings;
    private TutorBookingAdapter adapter;
    private ApiService apiService;
    private SessionManager sessionManager;
    private String currentStatus = null;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_bookings);

        // Status Bar Color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        }

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        ivBack = findViewById(R.id.ivBack);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvBookings = findViewById(R.id.rvBookings);

        ivBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        setupFilters();
        
        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            hasMoreData = true;
            loadBookings(true);
        });

        loadBookings(true);
    }

    private void setupRecyclerView() {
        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TutorBookingAdapter(new TutorBookingAdapter.OnBookingActionListener() {
            @Override
            public void onAccept(BookingResponse booking) {
                respondToBooking(booking.getId(), "accept", null);
            }

            @Override
            public void onReject(BookingResponse booking) {
                showRejectionDialog(booking.getId());
            }
        });
        rvBookings.setAdapter(adapter);

        rvBookings.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && hasMoreData) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            currentPage++;
                            loadBookings(false);
                        }
                    }
                }
            }
        });
    }

    private void setupFilters() {
        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) currentStatus = null;
            else if (checkedId == R.id.chipPending) currentStatus = "pending";
            else if (checkedId == R.id.chipAccepted) currentStatus = "accepted";
            else if (checkedId == R.id.chipRejected) currentStatus = "rejected";
            
            currentPage = 1;
            hasMoreData = true;
            loadBookings(true);
        });
    }

    private void loadBookings(boolean isRefresh) {
        if (isLoading) return;
        isLoading = true;
        swipeRefresh.setRefreshing(true);

        String token = "Bearer " + sessionManager.getAccessToken();
        apiService.getTutorBookingRequests(token, currentPage, currentStatus).enqueue(new Callback<ApiResponse<PaginatedResponse<BookingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaginatedResponse<BookingResponse>>> call, Response<ApiResponse<PaginatedResponse<BookingResponse>>> response) {
                isLoading = false;
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<BookingResponse> newBookings = response.body().getData().getResults();
                    if (isRefresh) {
                        adapter.updateList(newBookings);
                    } else {
                        List<BookingResponse> currentList = new ArrayList<>(adapter.getBookings());
                        currentList.addAll(newBookings);
                        adapter.updateList(currentList);
                    }
                    hasMoreData = response.body().getData().getNext() != null;
                } else {
                    Toast.makeText(TutorBookingsActivity.this, "Failed to load bookings", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PaginatedResponse<BookingResponse>>> call, Throwable t) {
                isLoading = false;
                swipeRefresh.setRefreshing(false);
                Toast.makeText(TutorBookingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRejectionDialog(int bookingId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reject Booking");
        
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_rejection_reason, null);
        EditText etReason = view.findViewById(R.id.etReason);
        builder.setView(view);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show();
                return;
            }
            respondToBooking(bookingId, "reject", reason);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void respondToBooking(int bookingId, String action, String reason) {
        String token = "Bearer " + sessionManager.getAccessToken();
        BookingActionRequest request = reason == null ? new BookingActionRequest(action) : new BookingActionRequest(action, reason);

        apiService.respondToBooking(token, bookingId, request).enqueue(new Callback<ApiResponse<BookingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<BookingResponse>> call, Response<ApiResponse<BookingResponse>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TutorBookingsActivity.this, "Booking " + action + "ed", Toast.LENGTH_SHORT).show();
                    currentPage = 1;
                    loadBookings(true);
                } else {
                    Toast.makeText(TutorBookingsActivity.this, "Action failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<BookingResponse>> call, Throwable t) {
                Toast.makeText(TutorBookingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

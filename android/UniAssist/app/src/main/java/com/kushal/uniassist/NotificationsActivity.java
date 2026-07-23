package com.kushal.uniassist;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.NotificationPaginatedResponse;
import com.kushal.uniassist.models.NotificationResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<NotificationResponse> notificationList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmpty, tvUnreadCount, tvMarkAllRead;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

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
        
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvUnreadCount = findViewById(R.id.tvUnreadCount);
        tvMarkAllRead = findViewById(R.id.tvMarkAllRead);

        adapter = new NotificationAdapter(notificationList, this::markAsRead);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        if (tvMarkAllRead != null) tvMarkAllRead.setOnClickListener(v -> markAllAsRead());

        fetchNotifications();
    }

    private void fetchNotifications() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + sessionManager.getAccessToken();

        apiService.getNotifications(authHeader, 1).enqueue(new Callback<ApiResponse<NotificationPaginatedResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<NotificationPaginatedResponse>> call, Response<ApiResponse<NotificationPaginatedResponse>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    NotificationPaginatedResponse data = response.body().getData();
                    notificationList.clear();
                    notificationList.addAll(data.getResults());
                    adapter.notifyDataSetChanged();
                    
                    int unread = data.getUnreadCount();
                    
                    if (tvUnreadCount != null) {
                        if (unread > 0) {
                            tvUnreadCount.setText(String.valueOf(unread));
                            tvUnreadCount.setVisibility(View.VISIBLE);
                        } else {
                            tvUnreadCount.setVisibility(View.GONE);
                        }
                    }
                    
                    if (tvEmpty != null) {
                        tvEmpty.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                } else if (response.code() == 401) {
                    redirectToLogin();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<NotificationPaginatedResponse>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(NotificationsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAsRead(NotificationResponse notification) {
        if (notification.isRead()) return;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + sessionManager.getAccessToken();

        apiService.markNotificationAsRead(authHeader, notification.getId()).enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(Call<NotificationResponse> call, Response<NotificationResponse> response) {
                if (response.isSuccessful()) {
                    fetchNotifications(); // Refresh list
                }
            }

            @Override
            public void onFailure(Call<NotificationResponse> call, Throwable t) {}
        });
    }

    private void markAllAsRead() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + sessionManager.getAccessToken();

        apiService.markAllNotificationsAsRead(authHeader).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    fetchNotifications();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void redirectToLogin() {
        if (sessionManager != null) sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

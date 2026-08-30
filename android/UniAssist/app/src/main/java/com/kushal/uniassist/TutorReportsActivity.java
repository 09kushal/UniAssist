package com.kushal.uniassist;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
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
import com.kushal.uniassist.models.MyReportsResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TutorReportsActivity extends AppCompatActivity {

    private RecyclerView rvLateness, rvReschedule;
    private LatenessReportAdapter latenessAdapter;
    private RescheduleRequestAdapter rescheduleAdapter;
    private ProgressBar progressBar;
    private TextView tvEmptyLateness, tvEmptyReschedule;
    private ImageView ivBack;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_reports);

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

        rvLateness = findViewById(R.id.rvLatenessReports);
        rvReschedule = findViewById(R.id.rvRescheduleRequests);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyLateness = findViewById(R.id.tvEmptyLateness);
        tvEmptyReschedule = findViewById(R.id.tvEmptyReschedule);
        ivBack = findViewById(R.id.ivBack);

        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        latenessAdapter = new LatenessReportAdapter();
        rvLateness.setLayoutManager(new LinearLayoutManager(this));
        rvLateness.setAdapter(latenessAdapter);

        rescheduleAdapter = new RescheduleRequestAdapter();
        rvReschedule.setLayoutManager(new LinearLayoutManager(this));
        rvReschedule.setAdapter(rescheduleAdapter);

        fetchReports();
    }

    private void fetchReports() {
        if (progressBar != null) progressBar.setVisibility(android.view.View.VISIBLE);
        String token = "Bearer " + sessionManager.getAccessToken();

        apiService.getMyReports(token).enqueue(new Callback<ApiResponse<MyReportsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MyReportsResponse>> call, Response<ApiResponse<MyReportsResponse>> response) {
                if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    MyReportsResponse data = response.body().getData();
                    
                    latenessAdapter.updateList(data.getLatenessReports());
                    rescheduleAdapter.updateList(data.getRescheduleRequests());

                    tvEmptyLateness.setVisibility(data.getLatenessReports().isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                    tvEmptyReschedule.setVisibility(data.getRescheduleRequests().isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);

                } else if (response.code() == 401) {
                    redirectToLogin();
                } else {
                    Toast.makeText(TutorReportsActivity.this, "Failed to load reports", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MyReportsResponse>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(TutorReportsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void redirectToLogin() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

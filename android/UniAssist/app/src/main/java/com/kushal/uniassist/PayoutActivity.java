package com.kushal.uniassist;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.kushal.uniassist.models.PaginatedResponse;
import com.kushal.uniassist.models.PayoutResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PayoutActivity extends AppCompatActivity {

    private static final String TAG = "PayoutActivity";
    private RecyclerView rvPayouts;
    private PayoutAdapter adapter;
    private List<PayoutResponse> payoutList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmpty, tvTotalEarned, tvPendingAmount;
    private ImageView ivBack;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payout);

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

        rvPayouts = findViewById(R.id.rvPayouts);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvTotalEarned = findViewById(R.id.tvTotalEarned);
        tvPendingAmount = findViewById(R.id.tvPendingAmount);
        ivBack = findViewById(R.id.ivBack);

        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        adapter = new PayoutAdapter();
        rvPayouts.setLayoutManager(new LinearLayoutManager(this));
        rvPayouts.setAdapter(adapter);

        fetchPayoutHistory();
    }

    private void fetchPayoutHistory() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getAccessToken();

        apiService.getPayoutHistory(token, 1).enqueue(new Callback<ApiResponse<PaginatedResponse<PayoutResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaginatedResponse<PayoutResponse>>> call, Response<ApiResponse<PaginatedResponse<PayoutResponse>>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<PayoutResponse> results = response.body().getData().getResults();
                    payoutList.clear();
                    payoutList.addAll(results);
                    adapter.updateList(payoutList);

                    calculateSummaries(results);

                    if (payoutList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }
                } else if (response.code() == 401) {
                    redirectToLogin();
                } else {
                    Toast.makeText(PayoutActivity.this, "Failed to load payouts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PaginatedResponse<PayoutResponse>>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(PayoutActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateSummaries(List<PayoutResponse> payouts) {
        double releasedTotal = 0;
        double heldTotal = 0;

        for (PayoutResponse p : payouts) {
            try {
                double amount = Double.parseDouble(p.getTutorFinalPayout());
                if ("released".equalsIgnoreCase(p.getPayoutStatus()) || "fined".equalsIgnoreCase(p.getPayoutStatus())) {
                    releasedTotal += amount;
                } else if ("held".equalsIgnoreCase(p.getPayoutStatus())) {
                    heldTotal += amount;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing payout amount: " + p.getTutorFinalPayout());
            }
        }

        tvTotalEarned.setText(String.format(java.util.Locale.US, "NPR %.2f", releasedTotal));
        tvPendingAmount.setText(String.format(java.util.Locale.US, "NPR %.2f", heldTotal));
    }

    private void redirectToLogin() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

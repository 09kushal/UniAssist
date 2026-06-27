package com.kushal.uniassist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kushal.uniassist.models.OtpVerifyRequest;
import com.kushal.uniassist.models.OtpVerifyResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpVerifyActivity extends AppCompatActivity {

    private EditText etOtp;
    private Button btnVerify;
    private ProgressBar progressBar;
    private TextView tvInstructions;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verify);

        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerify);
        progressBar = findViewById(R.id.progressBar);
        tvInstructions = findViewById(R.id.tvInstructions);

        email = getIntent().getStringExtra("email");
        if (email != null) {
            tvInstructions.setText("Enter the 6-digit code sent to " + email);
        }

        btnVerify.setOnClickListener(v -> attemptVerify());
    }

    private void attemptVerify() {
        String otpCode = etOtp.getText().toString().trim();

        if (otpCode.isEmpty() || otpCode.length() != 6) {
            Toast.makeText(this, "Please enter the 6-digit OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnVerify.setEnabled(false);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        OtpVerifyRequest request = new OtpVerifyRequest(email, otpCode);

        apiService.verifyOtp(request).enqueue(new Callback<OtpVerifyResponse>() {
            @Override
            public void onResponse(Call<OtpVerifyResponse> call, Response<OtpVerifyResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnVerify.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(OtpVerifyActivity.this,
                            "Verified! You can now log in.", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(OtpVerifyActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    String errorMsg = "Verification failed.";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    }
                    Toast.makeText(OtpVerifyActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<OtpVerifyResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnVerify.setEnabled(true);
                Toast.makeText(OtpVerifyActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
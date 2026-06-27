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

import com.kushal.uniassist.models.LoginRequest;
import com.kushal.uniassist.models.LoginResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister, tvGoToTutorRegister;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvGoToTutorRegister = findViewById(R.id.tvGoToTutorRegister);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
        tvGoToTutorRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, TutorRegisterActivity.class));
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        LoginRequest request = new LoginRequest(email, password);

        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    LoginResponse.Data data = response.body().getData();

                    SessionManager sessionManager = new SessionManager(LoginActivity.this);
                    sessionManager.saveSession(
                            data.getAccessToken(),
                            data.getRefreshToken(),
                            data.getRole(),
                            data.getFullName(),
                            data.getEmail()
                    );
                    android.util.Log.d("SESSION_CHECK", "Token saved: " + sessionManager.getAccessToken());

                    Toast.makeText(LoginActivity.this,
                            "Welcome " + data.getFullName(), Toast.LENGTH_SHORT).show();

                    // TODO: Navigate to Dashboard
                    String role = data.getRole();
                    Intent intent;

                    if ("tutor".equalsIgnoreCase(role)) {
                        intent = new Intent(LoginActivity.this, TutorDashboardActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                    }

                    startActivity(intent);
                    finish(); // close LoginActivity so back button doesn't return to it

                } else {
                    Toast.makeText(LoginActivity.this,
                            "Login failed. Check your credentials.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
package com.kushal.uniassist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kushal.uniassist.models.TutorRegisterRequest;
import com.kushal.uniassist.models.RegisterResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TutorRegisterActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword;
    private RadioGroup rgDomain;
    private Button btnRegister;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_register);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        rgDomain = findViewById(R.id.rgDomain);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        int selectedId = rgDomain.getCheckedRadioButtonId();
        String domain;
        if (selectedId == R.id.rbAcademic) {
            domain = "academic";
        } else if (selectedId == R.id.rbSkill) {
            domain = "skill";
        } else if (selectedId == R.id.rbBoth) {
            domain = "both";
        } else {
            Toast.makeText(this, "Please select a teaching domain", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        TutorRegisterRequest request = new TutorRegisterRequest(fullName, email, password, domain);

        apiService.registerTutor(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(TutorRegisterActivity.this,
                            response.body().getMessage(), Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(TutorRegisterActivity.this, OtpVerifyActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();

                } else {
                    String errorMsg = "Registration failed.";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(TutorRegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                Toast.makeText(TutorRegisterActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

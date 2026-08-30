package com.kushal.uniassist;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.ReviewSubmitRequest;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubmitReviewActivity extends AppCompatActivity {

    private int bookingId;
    private RatingBar ratingOverall, ratingKnowledge, ratingTeaching, ratingCommunication, ratingPunctuality;
    private EditText etReviewText;
    private MaterialButton btnSubmit, btnSkip;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_review);

        bookingId = getIntent().getIntExtra("booking_id", -1);
        if (bookingId == -1) {
            Toast.makeText(this, "Invalid session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SessionManager sessionManager = new SessionManager(this);
        token = "Bearer " + sessionManager.getAccessToken();

        initViews();
        setupListeners();
    }

    private void initViews() {
        ratingOverall = findViewById(R.id.ratingOverall);
        ratingKnowledge = findViewById(R.id.ratingKnowledge);
        ratingTeaching = findViewById(R.id.ratingTeaching);
        ratingCommunication = findViewById(R.id.ratingCommunication);
        ratingPunctuality = findViewById(R.id.ratingPunctuality);
        etReviewText = findViewById(R.id.etReviewText);
        btnSubmit = findViewById(R.id.btnSubmitReview);
        btnSkip = findViewById(R.id.btnSkipReview);
    }

    private void setupListeners() {
        btnSubmit.setOnClickListener(v -> submitReview());
        btnSkip.setOnClickListener(v -> finish());
    }

    private void submitReview() {
        int overallRating = (int) ratingOverall.getRating();
        String reviewText = etReviewText.getText().toString().trim();

        if (overallRating == 0) {
            Toast.makeText(this, "Please provide an overall rating", Toast.LENGTH_SHORT).show();
            return;
        }

        // review_text is now optional
        if (reviewText.isEmpty()) {
            reviewText = "No comment provided.";
        }

        ReviewSubmitRequest request = new ReviewSubmitRequest(bookingId, overallRating, reviewText);
        
        int k = (int) ratingKnowledge.getRating();
        if (k > 0) request.setKnowledgeRating(k);
        
        int t = (int) ratingTeaching.getRating();
        if (t > 0) request.setTeachingRating(t);
        
        int c = (int) ratingCommunication.getRating();
        if (c > 0) request.setCommunicationRating(c);
        
        int p = (int) ratingPunctuality.getRating();
        if (p > 0) request.setPunctualityRating(p);

        btnSubmit.setEnabled(false);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.submitReview(token, request).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                btnSubmit.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(SubmitReviewActivity.this, "Thank you for your feedback", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String error = "Failed to submit review";
                    if (response.body() != null && response.body().getMessage() != null) {
                        error = response.body().getMessage();
                    }
                    Toast.makeText(SubmitReviewActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Toast.makeText(SubmitReviewActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

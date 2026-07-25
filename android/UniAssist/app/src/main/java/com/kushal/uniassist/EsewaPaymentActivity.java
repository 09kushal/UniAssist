package com.kushal.uniassist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.PaymentInitiateRequest;
import com.kushal.uniassist.models.PaymentInitiateResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EsewaPaymentActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private int bookingId;
    private String amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esewa_payment);

        bookingId = getIntent().getIntExtra("booking_id", -1);
        amount = getIntent().getStringExtra("amount");

        if (bookingId == -1) {
            Toast.makeText(this, "Invalid booking", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupWebView();
        initiatePayment();
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                Log.d("eSewa", "Loading: " + url);

                // Detect success redirect
                if (url.contains("payment/esewa/success") || (url.contains("success") && url.contains("oid="))) {
                    progressBar.setVisibility(View.GONE);
                    handlePaymentSuccess();
                }

                // Detect failure redirect
                if (url.contains("payment/esewa/failure") || url.contains("failure")) {
                    progressBar.setVisibility(View.GONE);
                    handlePaymentFailure();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EsewaPaymentActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initiatePayment() {
        progressBar.setVisibility(View.VISIBLE);

        String token = "Bearer " + new SessionManager(this).getAccessToken();
        PaymentInitiateRequest request = new PaymentInitiateRequest(bookingId);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.initiatePayment(token, request).enqueue(new Callback<ApiResponse<PaymentInitiateResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaymentInitiateResponse>> call, Response<ApiResponse<PaymentInitiateResponse>> response) {
                Log.d("eSewa", "=== PAYMENT INITIATE RESPONSE ===");
                Log.d("eSewa", "Response code: " + response.code());
                Log.d("eSewa", "Success: " + response.isSuccessful());

                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    PaymentInitiateResponse data = response.body().getData();
                    Log.d("eSewa", "Amount: " + data.getAmount());
                    if (data.getEsewaFormData() != null) {
                        Log.d("eSewa", "UUID: " + data.getEsewaFormData().getTransactionUuid());
                        Log.d("eSewa", "Signature: " + data.getEsewaFormData().getSignature());
                        Log.d("eSewa", "Success URL: " + data.getEsewaFormData().getSuccessUrl());
                    }
                    loadEsewaPaymentPage(data);
                } else {
                    Log.e("eSewa", "Data is NULL!");
                    try {
                        String error = response.errorBody() != null ? response.errorBody().string() : "null";
                        Log.e("eSewa", "Error body: " + error);
                        Toast.makeText(EsewaPaymentActivity.this, "Payment init failed: " + error, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e("eSewa", e.getMessage());
                    }
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PaymentInitiateResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EsewaPaymentActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void loadEsewaPaymentPage(PaymentInitiateResponse data) {
        Log.d("eSewa", "=== LOADING ESEWA PAGE ===");
        if (data.getEsewaFormData() == null) {
            Log.e("eSewa", "Form data is NULL!");
            Toast.makeText(this, "Payment data error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PaymentInitiateResponse.EsewaFormData formData = data.getEsewaFormData();
        String esewaUrl = "https://rc-epay.esewa.com.np/api/epay/main/v2/form";

        Log.d("eSewa", "UUID: " + formData.getTransactionUuid());
        Log.d("eSewa", "Signature: " + formData.getSignature());
        Log.d("eSewa", "Success URL: " + formData.getSuccessUrl());
        Log.d("eSewa", "Total amount: " + formData.getTotalAmount());

        String html = "<!DOCTYPE html><html><body onload='document.f.submit()'>" +
                "<form name='f' method='POST' action='" + esewaUrl + "'>" +
                "<input type='hidden' name='amount' value='" + formData.getAmount() + "'/>" +
                "<input type='hidden' name='tax_amount' value='" + formData.getTaxAmount() + "'/>" +
                "<input type='hidden' name='total_amount' value='" + formData.getTotalAmount() + "'/>" +
                "<input type='hidden' name='transaction_uuid' value='" + formData.getTransactionUuid() + "'/>" +
                "<input type='hidden' name='product_code' value='" + formData.getProductCode() + "'/>" +
                "<input type='hidden' name='product_service_charge' value='" + formData.getProductServiceCharge() + "'/>" +
                "<input type='hidden' name='product_delivery_charge' value='" + formData.getProductDeliveryCharge() + "'/>" +
                "<input type='hidden' name='success_url' value='" + formData.getSuccessUrl() + "'/>" +
                "<input type='hidden' name='failure_url' value='" + formData.getFailureUrl() + "'/>" +
                "<input type='hidden' name='signed_field_names' value='" + formData.getSignedFieldNames() + "'/>" +
                "<input type='hidden' name='signature' value='" + formData.getSignature() + "'/>" +
                "</form></body></html>";

        Log.d("eSewa", "Full HTML: " + html);
        Log.d("eSewa", "Loading eSewa form...");
        webView.loadDataWithBaseURL(esewaUrl, html, "text/html", "UTF-8", null);
    }

    private void handlePaymentSuccess() {
        Toast.makeText(this, "Payment successful! Session is now scheduled.", Toast.LENGTH_LONG).show();
        Intent result = new Intent();
        result.putExtra("payment_success", true);
        result.putExtra("booking_id", bookingId);
        setResult(RESULT_OK, result);
        finish();
    }

    private void handlePaymentFailure() {
        Toast.makeText(this, "Payment failed or cancelled.", Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

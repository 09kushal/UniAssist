package com.kushal.uniassist;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.JoinTokenResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;

import java.net.MalformedURLException;
import java.net.URL;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JoinSessionActivity extends AppCompatActivity {

    private int bookingId;
    private String userFullName;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_session);

        bookingId = getIntent().getIntExtra("booking_id", -1);
        userFullName = getIntent().getStringExtra("user_full_name");

        if (bookingId == -1) {
            Toast.makeText(this, "Invalid session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SessionManager sessionManager = new SessionManager(this);
        token = "Bearer " + sessionManager.getAccessToken();

        checkPermissionsAndJoin();
    }

    private void checkPermissionsAndJoin() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            fetchTokenAndJoin();
        } else {
            ActivityCompat.requestPermissions(this, permissions, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            boolean allGranted = true;
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                fetchTokenAndJoin();
            } else {
                Toast.makeText(this, "Permissions required to join call", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void fetchTokenAndJoin() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getJoinToken(token, bookingId).enqueue(new Callback<ApiResponse<JoinTokenResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<JoinTokenResponse>> call, Response<ApiResponse<JoinTokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    JoinTokenResponse joinData = response.body().getData();
                    try {
                        JitsiMeetUserInfo userInfo = new JitsiMeetUserInfo();
                        userInfo.setDisplayName(userFullName != null ? userFullName : "User");

                        JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                                .setServerURL(new URL(joinData.getServerUrl()))
                                .setRoom(joinData.getRoom())
                                .setToken(joinData.getToken())
                                .setUserInfo(userInfo)
                                .setAudioMuted(false)
                                .setVideoMuted(false)
                                .setFeatureFlag("welcomepage.enabled", false)
                                .setFeatureFlag("prejoinpage.enabled", false)
                                .build();

                        JitsiMeetActivity.launch(JoinSessionActivity.this, options);
                        finish();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        Toast.makeText(JoinSessionActivity.this, "Error parsing server URL", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    String errorMsg = "Cannot join session";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    }
                    Toast.makeText(JoinSessionActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<JoinTokenResponse>> call, Throwable t) {
                Toast.makeText(JoinSessionActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}

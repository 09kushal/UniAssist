package com.kushal.uniassist;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;

import java.net.MalformedURLException;
import java.net.URL;

public class JoinSessionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_session);

        int bookingId = getIntent().getIntExtra("booking_id", -1);
        String userFullName = getIntent().getStringExtra("user_full_name");
        String userRole = getIntent().getStringExtra("user_role");

        if (bookingId == -1) {
            Toast.makeText(this, "Invalid session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Room name based on booking ID
        // This ensures student and tutor join same room
        String roomName = "UniAssist-Session-" + bookingId;

        try {
            JitsiMeetUserInfo userInfo = new JitsiMeetUserInfo();
            userInfo.setDisplayName(userFullName != null ? userFullName : "User");

            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.jit.si"))
                    .setRoom(roomName)
                    .setUserInfo(userInfo)
                    .setAudioMuted(false)
                    .setVideoMuted(false)
                    .setFeatureFlag("welcomepage.enabled", false)
                    .setFeatureFlag("prejoinpage.enabled", false)
                    .build();

            JitsiMeetActivity.launch(this, options);
            finish();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error joining session", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}

package com.kushal.uniassist;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "UniAssistSession";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_ROLE = "role";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PROFILE_PHOTO = "profile_photo";

    private SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String accessToken, String refreshToken, String role, String fullName, String email) {
        saveSession(accessToken, refreshToken, role, fullName, email, null);
    }

    public void saveSession(String accessToken, String refreshToken, String role, String fullName, String email, String photoUrl) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PROFILE_PHOTO, photoUrl);
        editor.apply();
    }

    public String getAccessToken() {
        String token = prefs.getString(KEY_ACCESS_TOKEN, null);
        android.util.Log.d("SessionManager", "Getting token: " + (token != null ? "exists, length=" + token.length() : "NULL"));
        return token;
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getProfilePhoto() {
        return prefs.getString(KEY_PROFILE_PHOTO, null);
    }

    public String getUserName() {
        return getFullName();
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void clearSession() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    public void setPendingReviewBookingId(int bookingId) {
        prefs.edit().putInt("pending_review_booking_id", bookingId).apply();
    }

    public int getPendingReviewBookingId() {
        return prefs.getInt("pending_review_booking_id", -1);
    }

    public void clearPendingReview() {
        prefs.edit().remove("pending_review_booking_id").apply();
    }
}

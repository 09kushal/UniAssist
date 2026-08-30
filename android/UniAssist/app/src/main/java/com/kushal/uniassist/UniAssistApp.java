package com.kushal.uniassist;

import android.app.Application;
import com.kushal.uniassist.network.ApiClient;

public class UniAssistApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize ApiClient with application context for the AuthInterceptor
        ApiClient.init(this);
    }
}

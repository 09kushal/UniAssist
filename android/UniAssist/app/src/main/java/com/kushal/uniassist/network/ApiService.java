package com.kushal.uniassist.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import com.kushal.uniassist.models.LoginRequest;
import com.kushal.uniassist.models.LoginResponse;
import com.kushal.uniassist.models.RegisterRequest;
import com.kushal.uniassist.models.RegisterResponse;
import com.kushal.uniassist.models.OtpVerifyRequest;
import com.kushal.uniassist.models.OtpVerifyResponse;

public interface ApiService {

    @POST("api/auth/login/")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/register/student/")
    Call<RegisterResponse> registerStudent(@Body RegisterRequest request);

    @POST("api/auth/verify-otp/")
    Call<OtpVerifyResponse> verifyOtp(@Body OtpVerifyRequest request);
}
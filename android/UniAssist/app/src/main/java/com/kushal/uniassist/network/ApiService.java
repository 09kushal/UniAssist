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
import com.kushal.uniassist.models.TutorRegisterRequest;
import com.kushal.uniassist.models.StudentProfileResponse;

import okhttp3.RequestBody;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.Part;

public interface ApiService {

    @POST("api/auth/login/")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/register/student/")
    Call<RegisterResponse> registerStudent(@Body RegisterRequest request);

    @POST("api/auth/register/tutor/")
    Call<RegisterResponse> registerTutor(@Body TutorRegisterRequest request);

    @POST("api/auth/verify-otp/")
    Call<OtpVerifyResponse> verifyOtp(@Body OtpVerifyRequest request);

    @Multipart
    @PATCH("api/auth/student/profile/setup/")
    Call<StudentProfileResponse> updateStudentProfile(
            @Header("Authorization") String authHeader,
            @Part("grade_or_university") RequestBody gradeOrUniversity,
            @Part("subjects_of_interest") RequestBody subjectsOfInterest
    );
}
package com.kushal.uniassist.network;

import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.AvailabilityRequest;
import com.kushal.uniassist.models.AvailabilitySlot;
import com.kushal.uniassist.models.BookingActionRequest;
import com.kushal.uniassist.models.PaginatedResponse;
import com.kushal.uniassist.models.BookingRequest;
import com.kushal.uniassist.models.BookingResponse;
import com.kushal.uniassist.models.JoinTokenResponse;
import com.kushal.uniassist.models.LatenessReportResponse;
import com.kushal.uniassist.models.LoginRequest;
import com.kushal.uniassist.models.LoginResponse;
import com.kushal.uniassist.models.MyReportsResponse;
import com.kushal.uniassist.models.NotificationPaginatedResponse;
import com.kushal.uniassist.models.NotificationResponse;
import com.kushal.uniassist.models.OtpVerifyRequest;
import com.kushal.uniassist.models.OtpVerifyResponse;
import com.kushal.uniassist.models.PaginatedResponse;
import com.kushal.uniassist.models.PaymentInitiateRequest;
import com.kushal.uniassist.models.PaymentInitiateResponse;
import com.kushal.uniassist.models.PayoutResponse;
import com.kushal.uniassist.models.RegisterRequest;
import com.kushal.uniassist.models.RegisterResponse;
import com.kushal.uniassist.models.SkillRequest;
import com.kushal.uniassist.models.SkillResponse;
import com.kushal.uniassist.models.StudentProfileResponse;
import com.kushal.uniassist.models.SubjectRequest;
import com.kushal.uniassist.models.SubjectResponse;
import com.kushal.uniassist.models.TutorProfileRequest;
import com.kushal.uniassist.models.TutorRegisterRequest;
import com.kushal.uniassist.models.TutorResponse;
import com.kushal.uniassist.models.TutorDocument;
import com.kushal.uniassist.models.UnreadCountResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

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
    Call<ApiResponse<StudentProfileResponse>> updateStudentProfile(
            @Header("Authorization") String authHeader,
            @Part("grade_or_university") RequestBody gradeOrUniversity,
            @Part("subjects_of_interest") RequestBody subjectsOfInterest,
            @Part MultipartBody.Part profilePhoto
    );

    @PATCH("api/auth/student/profile/setup/")
    Call<ApiResponse<StudentProfileResponse>> updateStudentProfile(
            @Header("Authorization") String token,
            @Body com.kushal.uniassist.models.StudentProfileRequest request
    );

    @GET("api/auth/student/profile/")
    Call<ApiResponse<StudentProfileResponse>> getStudentProfile(
            @Header("Authorization") String authHeader
    );

    // Tutor Endpoints
    @GET("api/tutors/list/")
    Call<ApiResponse<PaginatedResponse<TutorResponse>>> getTutorList(
            @Header("Authorization") String authHeader,
            @Query("domain") String domain,
            @Query("page") int page
    );

    @GET("api/tutors/{id}/profile/")
    Call<ApiResponse<TutorResponse>> getTutorProfile(
            @Header("Authorization") String authHeader,
            @Path("id") int tutorId
    );

    @GET("api/tutors/my-profile/")
    Call<ApiResponse<TutorResponse>> getMyTutorProfile(
            @Header("Authorization") String authHeader
    );

    @PATCH("api/tutors/profile/setup/")
    Call<ApiResponse<TutorResponse>> setupTutorProfile(
            @Header("Authorization") String token,
            @Body TutorProfileRequest request
    );

    @Multipart
    @PATCH("api/tutors/profile/setup/")
    Call<ApiResponse<TutorResponse>> setupTutorProfileWithPhoto(
            @Header("Authorization") String token,
            @Part("bio") RequestBody bio,
            @Part("pricing_per_session") RequestBody pricing,
            @Part MultipartBody.Part photo
    );

    @Multipart
    @POST("api/tutors/documents/upload/")
    Call<ApiResponse<TutorDocument>> uploadTutorDocument(
            @Header("Authorization") String token,
            @Part("doc_type") RequestBody docType,
            @Part MultipartBody.Part file
    );

    @GET("api/tutors/documents/")
    Call<ApiResponse<List<TutorDocument>>> getTutorDocuments(
            @Header("Authorization") String token
    );

    @POST("api/tutors/subjects/add/")
    Call<ApiResponse<SubjectResponse>> addSubject(
            @Header("Authorization") String token,
            @Body SubjectRequest request
    );

    @DELETE("api/tutors/subjects/{id}/remove/")
    Call<ApiResponse<Object>> removeSubject(
            @Header("Authorization") String token,
            @Path("id") int subjectId
    );

    @POST("api/tutors/skills/add/")
    Call<ApiResponse<SkillResponse>> addSkill(
            @Header("Authorization") String token,
            @Body SkillRequest request
    );

    @DELETE("api/tutors/skills/{id}/remove/")
    Call<ApiResponse<Object>> removeSkill(
            @Header("Authorization") String token,
            @Path("id") int skillId
    );

    @POST("api/tutors/availability/add/")
    Call<ApiResponse<AvailabilitySlot>> addAvailability(
            @Header("Authorization") String token,
            @Body AvailabilityRequest request
    );

    @DELETE("api/tutors/availability/{id}/remove/")
    Call<ApiResponse<Object>> removeAvailability(
            @Header("Authorization") String token,
            @Path("id") int slotId
    );

    // Booking Endpoints
    @POST("api/booking/request/")
    Call<ApiResponse<BookingResponse>> createBooking(
            @Header("Authorization") String token,
            @Body BookingRequest bookingRequest
    );

    @GET("api/booking/my-bookings/")
    Call<ApiResponse<PaginatedResponse<BookingResponse>>> getMyBookings(
            @Header("Authorization") String authHeader,
            @Query("page") int page
    );

    @GET("api/booking/my-requests/")
    Call<ApiResponse<PaginatedResponse<BookingResponse>>> getTutorBookingRequests(
            @Header("Authorization") String authHeader,
            @Query("page") int page,
            @Query("status") String status
    );

    @PATCH("api/booking/{booking_id}/respond/")
    Call<ApiResponse<BookingResponse>> respondToBooking(
            @Header("Authorization") String token,
            @Path("booking_id") int bookingId,
            @Body BookingActionRequest request
    );

    @POST("api/booking/{id}/join-token/")
    Call<ApiResponse<JoinTokenResponse>> getJoinToken(
            @Header("Authorization") String token,
            @Path("id") int bookingId
    );

    @GET("api/payments/tutor/payouts/")
    Call<ApiResponse<PaginatedResponse<PayoutResponse>>> getPayoutHistory(
            @Header("Authorization") String token,
            @Query("page") int page
    );

    // Report Endpoints
    @GET("api/reports/my-reports/")
    Call<ApiResponse<MyReportsResponse>> getMyReports(
            @Header("Authorization") String token
    );

    @POST("api/reports/lateness/file/")
    Call<ApiResponse<Object>> fileLatenessReport(
            @Header("Authorization") String token,
            @Body RequestBody reportData
    );

    @POST("api/reports/student/file/")
    Call<ApiResponse<Object>> fileStudentReport(
            @Header("Authorization") String token,
            @Body RequestBody reportData
    );

    @POST("api/reports/reschedule/request/")
    Call<ApiResponse<Object>> requestReschedule(
            @Header("Authorization") String token,
            @Body RequestBody requestData
    );

    // Review Endpoints
    @POST("api/reviews/submit/")
    Call<ApiResponse<Object>> submitReview(
            @Header("Authorization") String token,
            @Body com.kushal.uniassist.models.ReviewSubmitRequest request
    );

    @GET("api/reviews/check/{booking_id}/")
    Call<ApiResponse<com.kushal.uniassist.models.ReviewCheckResponse>> checkReview(
            @Header("Authorization") String token,
            @Path("booking_id") int bookingId
    );

    @GET("api/reviews/tutor/{tutor_id}/")
    Call<ApiResponse<com.kushal.uniassist.models.TutorReviewsResponse>> getTutorReviews(
            @Path("tutor_id") int tutorId,
            @Query("page") int page
    );

    // Notification Endpoints
    @GET("api/notifications/")
    Call<ApiResponse<NotificationPaginatedResponse>> getNotifications(
            @Header("Authorization") String authHeader,
            @Query("page") int page
    );

    @PATCH("api/notifications/{id}/read/")
    Call<NotificationResponse> markNotificationAsRead(
            @Header("Authorization") String authHeader,
            @Path("id") int notificationId
    );

    @PATCH("api/notifications/read-all/")
    Call<Void> markAllNotificationsAsRead(
            @Header("Authorization") String authHeader
    );

    @GET("api/notifications/unread-count/")
    Call<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @Header("Authorization") String authHeader
    );

    // Payment Endpoints
    @POST("api/payments/initiate/")
    Call<ApiResponse<PaymentInitiateResponse>> initiatePayment(
            @Header("Authorization") String token,
            @Body PaymentInitiateRequest request
    );

    @POST("api/payments/demo-complete/")
    Call<ApiResponse<Object>> demoCompletePayment(
            @Header("Authorization") String token,
            @Body PaymentInitiateRequest request
    );

    // Password Reset
    @POST("api/auth/password-reset/request/")
    Call<Void> requestPasswordReset(@Body RequestBody emailBody);

    @POST("api/auth/password-reset/confirm/")
    Call<Void> confirmPasswordReset(@Body RequestBody confirmBody);
}

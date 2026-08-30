package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class RescheduleRequestResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("booking_id")
    private int bookingId;

    @SerializedName("student_name")
    private String studentName;

    @SerializedName("reason")
    private String reason;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public int getBookingId() { return bookingId; }
    public String getStudentName() { return studentName; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}

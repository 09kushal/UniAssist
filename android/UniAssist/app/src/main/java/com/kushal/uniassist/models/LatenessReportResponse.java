package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class LatenessReportResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("session_id")
    private int sessionId;

    @SerializedName("booking_id")
    private int bookingId;

    @SerializedName("reported_by_name")
    private String reportedByName;

    @SerializedName("reported_against_name")
    private String reportedAgainstName;

    @SerializedName("reporter_role")
    private String reporterRole;

    @SerializedName("delay_range")
    private String delayRange;

    @SerializedName("description")
    private String description;

    @SerializedName("admin_action")
    private String adminAction;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public int getSessionId() { return sessionId; }
    public int getBookingId() { return bookingId; }
    public String getReportedByName() { return reportedByName; }
    public String getReportedAgainstName() { return reportedAgainstName; }
    public String getReporterRole() { return reporterRole; }
    public String getDelayRange() { return delayRange; }
    public String getDescription() { return description; }
    public String getAdminAction() { return adminAction; }
    public String getCreatedAt() { return createdAt; }
}

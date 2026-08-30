package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class PayoutResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("booking_id")
    private int bookingId;

    @SerializedName("tutor_name")
    private String tutorName;

    @SerializedName("total_paid")
    private String totalPaid;

    @SerializedName("commission_amount")
    private String commissionAmount;

    @SerializedName("tutor_base_share")
    private String tutorBaseShare;

    @SerializedName("fine_percentage")
    private String finePercentage;

    @SerializedName("fine_amount")
    private String fineAmount;

    @SerializedName("fine_reason")
    private String fineReason;

    @SerializedName("student_refund")
    private String studentRefund;

    @SerializedName("tutor_final_payout")
    private String tutorFinalPayout;

    @SerializedName("payout_status")
    private String payoutStatus;

    @SerializedName("released_at")
    private String releasedAt;

    public int getId() { return id; }
    public int getBookingId() { return bookingId; }
    public String getTutorName() { return tutorName; }
    public String getTotalPaid() { return totalPaid; }
    public String getCommissionAmount() { return commissionAmount; }
    public String getTutorBaseShare() { return tutorBaseShare; }
    public String getFinePercentage() { return finePercentage; }
    public String getFineAmount() { return fineAmount; }
    public String getFineReason() { return fineReason; }
    public String getStudentRefund() { return studentRefund; }
    public String getTutorFinalPayout() { return tutorFinalPayout; }
    public String getPayoutStatus() { return payoutStatus; }
    public String getReleasedAt() { return releasedAt; }
}

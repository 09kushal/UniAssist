package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class BookingActionRequest {
    @SerializedName("action")
    private String action;
    
    @SerializedName("rejection_reason")
    private String rejectionReason;
    
    public BookingActionRequest(String action) {
        this.action = action;
    }
    
    public BookingActionRequest(String action, String reason) {
        this.action = action;
        this.rejectionReason = reason;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}

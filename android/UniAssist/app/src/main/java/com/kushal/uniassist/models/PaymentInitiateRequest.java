package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class PaymentInitiateRequest {
    @SerializedName("booking_id")
    private int bookingId;
    
    public PaymentInitiateRequest(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }
}

package com.kushal.uniassist.models;

public class OtpVerifyRequest {
    private String email;
    private String otp_code;

    public OtpVerifyRequest(String email, String otp_code) {
        this.email = email;
        this.otp_code = otp_code;
    }
}
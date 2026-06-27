package com.kushal.uniassist.models;

public class OtpVerifyResponse {
    private boolean success;
    private String message;
    private Data data;

    public static class Data {
        private String email;
        public String getEmail() { return email; }
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Data getData() { return data; }
}
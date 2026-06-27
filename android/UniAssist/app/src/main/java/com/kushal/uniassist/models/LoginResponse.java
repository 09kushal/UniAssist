package com.kushal.uniassist.models;

public class LoginResponse {
    private boolean success;
    private String message;
    private Data data;

    public static class Data {
        private String access_token;
        private String refresh_token;
        private String role;
        private String full_name;
        private String email;

        public String getAccessToken() { return access_token; }
        public String getRefreshToken() { return refresh_token; }
        public String getRole() { return role; }
        public String getFullName() { return full_name; }
        public String getEmail() { return email; }
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Data getData() { return data; }
}

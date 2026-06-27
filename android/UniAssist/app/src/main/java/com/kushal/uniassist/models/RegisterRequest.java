package com.kushal.uniassist.models;

public class RegisterRequest {
    private String full_name;
    private String email;
    private String password;

    public RegisterRequest(String full_name, String email, String password) {
        this.full_name = full_name;
        this.email = email;
        this.password = password;
    }
}
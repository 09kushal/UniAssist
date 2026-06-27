package com.kushal.uniassist.models;

public class TutorRegisterRequest {
    private String full_name;
    private String email;
    private String password;
    private String domain;

    public TutorRegisterRequest(String full_name, String email, String password, String domain) {
        this.full_name = full_name;
        this.email = email;
        this.password = password;
        this.domain = domain;
    }
}

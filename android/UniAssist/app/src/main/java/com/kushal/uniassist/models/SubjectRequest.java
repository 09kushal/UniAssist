package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class SubjectRequest {
    @SerializedName("name")
    private String name;

    public SubjectRequest(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

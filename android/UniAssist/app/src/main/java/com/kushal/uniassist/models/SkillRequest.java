package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class SkillRequest {
    @SerializedName("name")
    private String name;

    public SkillRequest(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

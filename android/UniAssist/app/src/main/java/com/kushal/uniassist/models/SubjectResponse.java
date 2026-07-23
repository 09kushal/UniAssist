package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class SubjectResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name = "";

    public int getId() { return id; }
    public String getName() { 
        return name != null ? name : ""; 
    }
}

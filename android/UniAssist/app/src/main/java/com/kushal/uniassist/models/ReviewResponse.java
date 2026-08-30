package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class ReviewResponse {
    @SerializedName("id")
    private int id;
    
    @SerializedName("student_name")
    private String studentName;
    
    @SerializedName("rating")
    private int rating;
    
    @SerializedName("review_text")
    private String reviewText;
    
    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public String getStudentName() { return studentName; }
    public int getRating() { return rating; }
    public String getReviewText() { return reviewText; }
    public String getCreatedAt() { return createdAt; }
}

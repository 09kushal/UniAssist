package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TutorReviewsResponse {
    @SerializedName("results")
    private List<ReviewResponse> results;
    
    @SerializedName("count")
    private int count;

    public List<ReviewResponse> getResults() { return results; }
    public int getCount() { return count; }
}

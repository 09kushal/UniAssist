package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class ReviewCheckResponse {
    @SerializedName("has_reviewed")
    private boolean hasReviewed;

    public boolean isHasReviewed() {
        return hasReviewed;
    }
}

package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class TutorProfileRequest {
    @SerializedName("bio")
    private String bio;
    
    @SerializedName("pricing_per_session")
    private String pricingPerSession;
    
    public TutorProfileRequest(String bio, String pricing) {
        this.bio = bio;
        this.pricingPerSession = pricing;
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getPricingPerSession() { return pricingPerSession; }
    public void setPricingPerSession(String pricingPerSession) { this.pricingPerSession = pricingPerSession; }
}

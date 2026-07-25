package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class TutorResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("full_name")
    private String fullName = "";

    @SerializedName("domain")
    private String domain = "";

    @SerializedName("bio")
    private String bio = "";

    @SerializedName("pricing_per_session")
    private String pricingPerSession = "0.00";

    @SerializedName("profile_photo_url")
    private String profilePhotoUrl;

    @SerializedName("punctuality_score")
    private String punctualityScore = "0";

    @SerializedName("total_sessions_done")
    private int totalSessionsDone = 0;

    @SerializedName("average_rating")
    private String averageRating;

    @SerializedName("review_count")
    private int reviewCount = 0;

    @SerializedName("is_verified_badge")
    private boolean isVerifiedBadge = false;

    @SerializedName("is_verified")
    private boolean isVerified = false;

    @SerializedName("subjects")
    private List<SubjectResponse> subjects;

    @SerializedName("skills")
    private List<SkillResponse> skills;

    @SerializedName("availability_slots")
    private List<AvailabilitySlot> availabilitySlots;

    // Safe getters
    public int getId() { return id; }
    
    public String getFullName() { 
        return fullName != null ? fullName : ""; 
    }
    
    public String getDomain() { 
        return domain != null ? domain : ""; 
    }
    
    public String getBio() { 
        return bio != null ? bio : ""; 
    }
    
    public String getPricingPerSession() { 
        return pricingPerSession != null ? 
            pricingPerSession : "0.00"; 
    }
    
    public String getProfilePhotoUrl() { 
        return profilePhotoUrl; 
    }
    
    public boolean isVerifiedBadge() { 
        return isVerifiedBadge; 
    }

    public boolean isVerified() {
        return isVerifiedBadge;
    }

    public int getTotalSessionsDone() {
        return totalSessionsDone;
    }
    
    public float getAverageRatingFloat() {
        if (averageRating == null || 
            averageRating.isEmpty() ||
            averageRating.equals("null")) return 0f;
        try {
            return Float.parseFloat(averageRating);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    public String getAverageRating() {
        return averageRating != null ? averageRating : "0.0";
    }
    
    public List<SubjectResponse> getSubjects() {
        return subjects != null ? subjects : new ArrayList<>();
    }
    
    public List<SkillResponse> getSkills() {
        return skills != null ? skills : new ArrayList<>();
    }
    
    public List<AvailabilitySlot> getAvailabilitySlots() {
        return availabilitySlots != null ? 
            availabilitySlots : new ArrayList<>();
    }
}

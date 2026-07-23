package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class BookingResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("student")
    private StudentInfo student;

    @SerializedName("tutor")
    private TutorInfo tutor;

    @SerializedName("selected_slot")
    private Object selectedSlot;

    @SerializedName("subject_or_skill")
    private String subjectOrSkill = "";

    @SerializedName("proposed_date")
    private String proposedDate = "";

    @SerializedName("proposed_start_time")
    private String proposedStartTime = "";

    @SerializedName("proposed_end_time")
    private String proposedEndTime = "";

    @SerializedName("message")
    private String message = "";

    @SerializedName("booking_status")
    private String bookingStatus = "";

    @SerializedName("rejection_reason")
    private String rejectionReason = "";

    @SerializedName("officially_scheduled")
    private boolean officiallyScheduled = false;

    @SerializedName("created_at")
    private String createdAt = "";

    // Getters
    public int getId() { return id; }
    
    public StudentInfo getStudent() { return student; }
    
    public TutorInfo getTutor() { return tutor; }
    
    public String getSubjectOrSkill() {
        return subjectOrSkill != null ? subjectOrSkill : "";
    }
    
    public String getProposedDate() {
        return proposedDate != null ? proposedDate : "";
    }
    
    public String getProposedStartTime() {
        return proposedStartTime != null ? 
            proposedStartTime : "";
    }
    
    public String getProposedEndTime() {
        return proposedEndTime != null ? 
            proposedEndTime : "";
    }
    
    public String getMessage() {
        return message != null ? message : "";
    }
    
    public String getBookingStatus() {
        return bookingStatus != null ? bookingStatus : "";
    }
    
    public String getRejectionReason() {
        return rejectionReason != null ? 
            rejectionReason : "";
    }
    
    public boolean isOfficiallyScheduled() {
        return officiallyScheduled;
    }
    
    public String getCreatedAt() {
        return createdAt != null ? createdAt : "";
    }

    // Inner class for student info
    public static class StudentInfo {
        @SerializedName("id")
        private int id;

        @SerializedName("full_name")
        private String fullName = "";

        @SerializedName("email")
        private String email = "";

        @SerializedName("profile_photo_url")
        private String profilePhotoUrl;

        public int getId() { return id; }
        
        public String getFullName() {
            return fullName != null ? fullName : "";
        }
        
        public String getEmail() {
            return email != null ? email : "";
        }
        
        public String getProfilePhotoUrl() {
            return profilePhotoUrl;
        }
    }

    // Inner class for tutor info
    public static class TutorInfo {
        @SerializedName("id")
        private int id;

        @SerializedName("full_name")
        private String fullName = "";

        @SerializedName("email")
        private String email = "";

        @SerializedName("domain")
        private String domain = "";

        @SerializedName("pricing_per_session")
        private String pricingPerSession = "0.00";

        @SerializedName("profile_photo_url")
        private String profilePhotoUrl;

        @SerializedName("is_verified")
        private boolean isVerified = false;

        public int getId() { return id; }
        
        public String getFullName() {
            return fullName != null ? fullName : "";
        }
        
        public String getEmail() {
            return email != null ? email : "";
        }
        
        public String getDomain() {
            return domain != null ? domain : "";
        }
        
        public String getPricingPerSession() {
            return pricingPerSession != null ? 
                pricingPerSession : "0.00";
        }
        
        public String getProfilePhotoUrl() {
            return profilePhotoUrl;
        }
        
        public boolean isVerified() {
            return isVerified;
        }
    }
}

package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class BookingRequest {
    @SerializedName("tutor_id")
    private int tutorId;
    
    @SerializedName("subject_or_skill")
    private String subjectOrSkill;
    
    @SerializedName("proposed_date")
    private String proposedDate;
    
    @SerializedName("proposed_start_time")
    private String proposedStartTime;
    
    @SerializedName("proposed_end_time")
    private String proposedEndTime;
    
    @SerializedName("message")
    private String message;

    public BookingRequest(int tutorId, String subjectOrSkill, String proposedDate, String proposedStartTime, String proposedEndTime, String message) {
        this.tutorId = tutorId;
        this.subjectOrSkill = subjectOrSkill;
        this.proposedDate = proposedDate;
        this.proposedStartTime = proposedStartTime;
        this.proposedEndTime = proposedEndTime;
        this.message = message;
    }
}

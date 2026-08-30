package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class StudentProfileResponse {
    @SerializedName("id")
    private int id;
    
    @SerializedName("full_name")
    private String fullName;
    
    @SerializedName("grade_or_university")
    private String gradeOrUniversity;
    
    @SerializedName("subjects_of_interest")
    private String subjectsOfInterest;
    
    @SerializedName("profile_photo_url")
    private String profilePhotoUrl;

    public int getId() { return id; }
    public String getFullName() { return fullName != null ? fullName : ""; }
    public String getGradeOrUniversity() { return gradeOrUniversity != null ? gradeOrUniversity : ""; }
    public String getSubjectsOfInterest() { return subjectsOfInterest != null ? subjectsOfInterest : ""; }
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
}

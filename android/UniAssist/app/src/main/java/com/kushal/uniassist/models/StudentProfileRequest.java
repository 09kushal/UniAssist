package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class StudentProfileRequest {
    @SerializedName("grade_or_university")
    private String gradeOrUniversity;
    
    @SerializedName("subjects_of_interest")
    private String subjectsOfInterest;
    
    public StudentProfileRequest(String grade, String subjects) {
        this.gradeOrUniversity = grade;
        this.subjectsOfInterest = subjects;
    }

    public String getGradeOrUniversity() {
        return gradeOrUniversity;
    }

    public void setGradeOrUniversity(String gradeOrUniversity) {
        this.gradeOrUniversity = gradeOrUniversity;
    }

    public String getSubjectsOfInterest() {
        return subjectsOfInterest;
    }

    public void setSubjectsOfInterest(String subjectsOfInterest) {
        this.subjectsOfInterest = subjectsOfInterest;
    }
}

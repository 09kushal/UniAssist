package com.kushal.uniassist.models;

public class StudentProfileResponse {
    private boolean success;
    private String message;
    private Data data;

    public static class Data {
        private int id;
        private String full_name;
        private String grade_or_university;
        private String subjects_of_interest;
        private String profile_photo_url;

        public int getId() { return id; }
        public String getFullName() { return full_name; }
        public String getGradeOrUniversity() { return grade_or_university; }
        public String getSubjectsOfInterest() { return subjects_of_interest; }
        public String getProfilePhotoUrl() { return profile_photo_url; }
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Data getData() { return data; }
}

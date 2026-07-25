package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class AvailabilityRequest {
    @SerializedName("day_of_week")
    private String dayOfWeek;
    
    @SerializedName("start_time")
    private String startTime;
    
    @SerializedName("end_time")
    private String endTime;
    
    public AvailabilityRequest(String day, String start, String end) {
        this.dayOfWeek = day;
        this.startTime = start;
        this.endTime = end;
    }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}

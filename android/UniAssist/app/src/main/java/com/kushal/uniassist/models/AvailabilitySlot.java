package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class AvailabilitySlot {
    @SerializedName("id")
    private int id;
    
    @SerializedName("day_of_week")
    private String dayOfWeek;
    
    @SerializedName("start_time")
    private String startTime;
    
    @SerializedName("end_time")
    private String endTime;

    public int getId() { return id; }
    public String getDayOfWeek() { return dayOfWeek; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
}

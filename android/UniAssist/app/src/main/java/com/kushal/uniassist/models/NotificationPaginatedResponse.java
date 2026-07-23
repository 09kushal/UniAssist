package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class NotificationPaginatedResponse {
    @SerializedName("unread_count")
    private int unreadCount;
    
    @SerializedName("count")
    private int count;
    
    @SerializedName("pages")
    private int pages;
    
    @SerializedName("results")
    private List<NotificationResponse> results;
    
    public int getUnreadCount() { return unreadCount; }
    public int getCount() { return count; }
    public int getPages() { return pages; }
    public List<NotificationResponse> getResults() {
        return results != null ? results : new ArrayList<>();
    }
}

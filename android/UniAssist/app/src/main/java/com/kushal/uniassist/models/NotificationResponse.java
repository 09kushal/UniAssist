package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class NotificationResponse {
    @SerializedName("id")
    private int id;
    
    @SerializedName("title")
    private String title = "";
    
    @SerializedName("message")
    private String message = "";
    
    @SerializedName("notification_type")
    private String notificationType = "";
    
    @SerializedName("is_read")
    private boolean isRead = false;
    
    @SerializedName("created_at")
    private String createdAt = "";
    
    public int getId() { return id; }
    public String getTitle() { 
        return title != null ? title : ""; 
    }
    public String getMessage() { 
        return message != null ? message : ""; 
    }
    public String getNotificationType() { 
        return notificationType != null ? 
            notificationType : ""; 
    }
    public boolean isRead() { return isRead; }
    public String getCreatedAt() { 
        return createdAt != null ? createdAt : ""; 
    }
}

package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class JoinTokenResponse {
    @SerializedName("token")
    private String token;

    @SerializedName("room")
    private String room;

    @SerializedName("server_url")
    private String serverUrl;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}

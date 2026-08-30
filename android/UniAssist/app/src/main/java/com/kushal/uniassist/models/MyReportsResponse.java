package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MyReportsResponse {
    @SerializedName("lateness_reports")
    private List<LatenessReportResponse> latenessReports;

    @SerializedName("reschedule_requests")
    private List<RescheduleRequestResponse> rescheduleRequests;

    public List<LatenessReportResponse> getLatenessReports() { return latenessReports; }
    public List<RescheduleRequestResponse> getRescheduleRequests() { return rescheduleRequests; }
}

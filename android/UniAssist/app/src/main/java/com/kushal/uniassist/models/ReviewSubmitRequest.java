package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class ReviewSubmitRequest {
    @SerializedName("booking_id")
    private int bookingId;

    @SerializedName("rating")
    private int rating;

    @SerializedName("review_text")
    private String reviewText;

    @SerializedName("knowledge_rating")
    private Integer knowledgeRating;

    @SerializedName("teaching_rating")
    private Integer teachingRating;

    @SerializedName("communication_rating")
    private Integer communicationRating;

    @SerializedName("punctuality_rating")
    private Integer punctualityRating;

    public ReviewSubmitRequest(int bookingId, int rating, String reviewText) {
        this.bookingId = bookingId;
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public void setKnowledgeRating(Integer knowledgeRating) {
        this.knowledgeRating = knowledgeRating;
    }

    public void setTeachingRating(Integer teachingRating) {
        this.teachingRating = teachingRating;
    }

    public void setCommunicationRating(Integer communicationRating) {
        this.communicationRating = communicationRating;
    }

    public void setPunctualityRating(Integer punctualityRating) {
        this.punctualityRating = punctualityRating;
    }
}

package model.coaching_session;

import java.util.Date;

public class SessionFeedback {
    private Integer id;
    private Integer sessionId;
    private Integer coachingRequestId;
    private Integer userId;
    private Integer coachId;
    private Integer coachRating;
    private String userFeedback;
    private String userComment;
    private Date createdAt;
    private Date updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getCoachingRequestId() {
        return coachingRequestId;
    }

    public void setCoachingRequestId(Integer coachingRequestId) {
        this.coachingRequestId = coachingRequestId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCoachId() {
        return coachId;
    }

    public void setCoachId(Integer coachId) {
        this.coachId = coachId;
    }

    public Integer getCoachRating() {
        return coachRating;
    }

    public void setCoachRating(Integer coachRating) {
        if (coachRating != null && (coachRating < 1 || coachRating > 5)) {
            throw new IllegalArgumentException("La note du coach doit etre entre 1 et 5");
        }
        this.coachRating = coachRating;
    }

    public String getUserFeedback() {
        return userFeedback;
    }

    public void setUserFeedback(String userFeedback) {
        this.userFeedback = userFeedback;
    }

    public String getUserComment() {
        return userComment;
    }

    public void setUserComment(String userComment) {
        this.userComment = userComment;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}

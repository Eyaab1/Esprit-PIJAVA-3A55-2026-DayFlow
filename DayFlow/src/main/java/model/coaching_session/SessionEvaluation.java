package model.coaching_session;

import java.util.Date;

public class SessionEvaluation {
    private Integer id;
    private Integer sessionId;
    private Integer coachingRequestId;
    private Integer userId;
    private Integer coachId;
    private Integer progressDelta;
    private Integer disciplineScore;
    private Integer goalAchievementScore;
    private Integer evolutionScore;
    private Integer coachFeedbackScore;
    private String coachRemarks;
    private String recommendations;
    private String nextAction;
    private String programAdjustment;
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

    public Integer getProgressDelta() {
        return progressDelta;
    }

    public void setProgressDelta(Integer progressDelta) {
        if (progressDelta != null && (progressDelta < -30 || progressDelta > 30)) {
            throw new IllegalArgumentException("Le delta de progression doit etre entre -30 et 30");
        }
        this.progressDelta = progressDelta;
    }

    public Integer getDisciplineScore() {
        return disciplineScore;
    }

    public void setDisciplineScore(Integer disciplineScore) {
        this.disciplineScore = clampPercentage(disciplineScore);
    }

    public Integer getGoalAchievementScore() {
        return goalAchievementScore;
    }

    public void setGoalAchievementScore(Integer goalAchievementScore) {
        this.goalAchievementScore = clampPercentage(goalAchievementScore);
    }

    public Integer getEvolutionScore() {
        return evolutionScore;
    }

    public void setEvolutionScore(Integer evolutionScore) {
        this.evolutionScore = clampPercentage(evolutionScore);
    }

    public Integer getCoachFeedbackScore() {
        return coachFeedbackScore;
    }

    public void setCoachFeedbackScore(Integer coachFeedbackScore) {
        this.coachFeedbackScore = clampPercentage(coachFeedbackScore);
    }

    public String getCoachRemarks() {
        return coachRemarks;
    }

    public void setCoachRemarks(String coachRemarks) {
        this.coachRemarks = coachRemarks;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public String getProgramAdjustment() {
        return programAdjustment;
    }

    public void setProgramAdjustment(String programAdjustment) {
        this.programAdjustment = programAdjustment;
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

    private Integer clampPercentage(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("Le score doit etre entre 0 et 100");
        }
        return value;
    }
}

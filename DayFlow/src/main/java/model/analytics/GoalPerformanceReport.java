package model.analytics;

import java.time.LocalDateTime;

/**
 * Goal Performance Report DTO
 * Contains detailed performance analysis for a specific goal
 */
public class GoalPerformanceReport {

    private int goalId;
    private String goalTitle;
    private int progress;
    private String status;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;

    // Performance Metrics
    private PerformanceLevel performanceLevel;
    private double completionRate;
    private int daysUntilDeadline;
    private boolean isOverdue;
    private boolean isAtRisk;

    // Activity Metrics
    private int totalActivities;
    private int completedActivities;
    private int pendingActivities;

    // Routine Metrics
    private int totalRoutines;
    private int completedRoutines;
    private int activeRoutines;

    // Warnings and Recommendations
    private String warningMessage;
    private String recommendation;
    private boolean needsAttention;

    // Metadata
    private LocalDateTime calculatedAt;

    // Constructor
    public GoalPerformanceReport() {
        this.calculatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getGoalId() {
        return goalId;
    }

    public void setGoalId(int goalId) {
        this.goalId = goalId;
    }

    public String getGoalTitle() {
        return goalTitle;
    }

    public void setGoalTitle(String goalTitle) {
        this.goalTitle = goalTitle;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public PerformanceLevel getPerformanceLevel() {
        return performanceLevel;
    }

    public void setPerformanceLevel(PerformanceLevel performanceLevel) {
        this.performanceLevel = performanceLevel;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public int getDaysUntilDeadline() {
        return daysUntilDeadline;
    }

    public void setDaysUntilDeadline(int daysUntilDeadline) {
        this.daysUntilDeadline = daysUntilDeadline;
    }

    public boolean isOverdue() {
        return isOverdue;
    }

    public void setOverdue(boolean overdue) {
        isOverdue = overdue;
    }

    public boolean isAtRisk() {
        return isAtRisk;
    }

    public void setAtRisk(boolean atRisk) {
        isAtRisk = atRisk;
    }

    public int getTotalActivities() {
        return totalActivities;
    }

    public void setTotalActivities(int totalActivities) {
        this.totalActivities = totalActivities;
    }

    public int getCompletedActivities() {
        return completedActivities;
    }

    public void setCompletedActivities(int completedActivities) {
        this.completedActivities = completedActivities;
    }

    public int getPendingActivities() {
        return pendingActivities;
    }

    public void setPendingActivities(int pendingActivities) {
        this.pendingActivities = pendingActivities;
    }

    public int getTotalRoutines() {
        return totalRoutines;
    }

    public void setTotalRoutines(int totalRoutines) {
        this.totalRoutines = totalRoutines;
    }

    public int getCompletedRoutines() {
        return completedRoutines;
    }

    public void setCompletedRoutines(int completedRoutines) {
        this.completedRoutines = completedRoutines;
    }

    public int getActiveRoutines() {
        return activeRoutines;
    }

    public void setActiveRoutines(int activeRoutines) {
        this.activeRoutines = activeRoutines;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public boolean isNeedsAttention() {
        return needsAttention;
    }

    public void setNeedsAttention(boolean needsAttention) {
        this.needsAttention = needsAttention;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    @Override
    public String toString() {
        return "GoalPerformanceReport{" +
                "goalId=" + goalId +
                ", goalTitle='" + goalTitle + '\'' +
                ", performanceLevel=" + performanceLevel +
                ", progress=" + progress +
                ", isOverdue=" + isOverdue +
                ", daysUntilDeadline=" + daysUntilDeadline +
                '}';
    }
}

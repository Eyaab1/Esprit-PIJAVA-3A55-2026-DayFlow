package model.analytics;

import java.time.LocalDateTime;

/**
 * Progress Statistics DTO
 * Contains comprehensive statistics for user progress tracking
 */
public class ProgressStatistics {

    // Goal Statistics
    private int totalGoals;
    private int completedGoals;
    private int activeGoals;
    private int pausedGoals;
    private int overdueGoals;
    private int draftGoals;

    // Routine Statistics
    private int totalRoutines;
    private int activeRoutines;
    private int inactiveRoutines;
    private int completedRoutines;

    // Activity Statistics
    private int totalActivities;
    private int completedActivities;
    private int pendingActivities;
    private int inProgressActivities;

    // Deadline Statistics
    private int missedDeadlines;
    private int upcomingDeadlines;

    // Performance Metrics
    private double overallProductivityScore;
    private double goalCompletionRate;
    private double activityCompletionRate;
    private double onTimeCompletionRate;

    // Time-based Statistics
    private int goalsCompletedThisWeek;
    private int goalsCompletedThisMonth;
    private int activitiesCompletedThisWeek;
    private int activitiesCompletedThisMonth;

    // Metadata
    private LocalDateTime calculatedAt;
    private int userId;

    // Constructor
    public ProgressStatistics() {
        this.calculatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getTotalGoals() {
        return totalGoals;
    }

    public void setTotalGoals(int totalGoals) {
        this.totalGoals = totalGoals;
    }

    public int getCompletedGoals() {
        return completedGoals;
    }

    public void setCompletedGoals(int completedGoals) {
        this.completedGoals = completedGoals;
    }

    public int getActiveGoals() {
        return activeGoals;
    }

    public void setActiveGoals(int activeGoals) {
        this.activeGoals = activeGoals;
    }

    public int getPausedGoals() {
        return pausedGoals;
    }

    public void setPausedGoals(int pausedGoals) {
        this.pausedGoals = pausedGoals;
    }

    public int getOverdueGoals() {
        return overdueGoals;
    }

    public void setOverdueGoals(int overdueGoals) {
        this.overdueGoals = overdueGoals;
    }

    public int getDraftGoals() {
        return draftGoals;
    }

    public void setDraftGoals(int draftGoals) {
        this.draftGoals = draftGoals;
    }

    public int getTotalRoutines() {
        return totalRoutines;
    }

    public void setTotalRoutines(int totalRoutines) {
        this.totalRoutines = totalRoutines;
    }

    public int getActiveRoutines() {
        return activeRoutines;
    }

    public void setActiveRoutines(int activeRoutines) {
        this.activeRoutines = activeRoutines;
    }

    public int getInactiveRoutines() {
        return inactiveRoutines;
    }

    public void setInactiveRoutines(int inactiveRoutines) {
        this.inactiveRoutines = inactiveRoutines;
    }

    public int getCompletedRoutines() {
        return completedRoutines;
    }

    public void setCompletedRoutines(int completedRoutines) {
        this.completedRoutines = completedRoutines;
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

    public int getInProgressActivities() {
        return inProgressActivities;
    }

    public void setInProgressActivities(int inProgressActivities) {
        this.inProgressActivities = inProgressActivities;
    }

    public int getMissedDeadlines() {
        return missedDeadlines;
    }

    public void setMissedDeadlines(int missedDeadlines) {
        this.missedDeadlines = missedDeadlines;
    }

    public int getUpcomingDeadlines() {
        return upcomingDeadlines;
    }

    public void setUpcomingDeadlines(int upcomingDeadlines) {
        this.upcomingDeadlines = upcomingDeadlines;
    }

    public double getOverallProductivityScore() {
        return overallProductivityScore;
    }

    public void setOverallProductivityScore(double overallProductivityScore) {
        this.overallProductivityScore = overallProductivityScore;
    }

    public double getGoalCompletionRate() {
        return goalCompletionRate;
    }

    public void setGoalCompletionRate(double goalCompletionRate) {
        this.goalCompletionRate = goalCompletionRate;
    }

    public double getActivityCompletionRate() {
        return activityCompletionRate;
    }

    public void setActivityCompletionRate(double activityCompletionRate) {
        this.activityCompletionRate = activityCompletionRate;
    }

    public double getOnTimeCompletionRate() {
        return onTimeCompletionRate;
    }

    public void setOnTimeCompletionRate(double onTimeCompletionRate) {
        this.onTimeCompletionRate = onTimeCompletionRate;
    }

    public int getGoalsCompletedThisWeek() {
        return goalsCompletedThisWeek;
    }

    public void setGoalsCompletedThisWeek(int goalsCompletedThisWeek) {
        this.goalsCompletedThisWeek = goalsCompletedThisWeek;
    }

    public int getGoalsCompletedThisMonth() {
        return goalsCompletedThisMonth;
    }

    public void setGoalsCompletedThisMonth(int goalsCompletedThisMonth) {
        this.goalsCompletedThisMonth = goalsCompletedThisMonth;
    }

    public int getActivitiesCompletedThisWeek() {
        return activitiesCompletedThisWeek;
    }

    public void setActivitiesCompletedThisWeek(int activitiesCompletedThisWeek) {
        this.activitiesCompletedThisWeek = activitiesCompletedThisWeek;
    }

    public int getActivitiesCompletedThisMonth() {
        return activitiesCompletedThisMonth;
    }

    public void setActivitiesCompletedThisMonth(int activitiesCompletedThisMonth) {
        this.activitiesCompletedThisMonth = activitiesCompletedThisMonth;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Calculated Properties
    public int getPendingGoals() {
        return totalGoals - completedGoals - overdueGoals;
    }

    public double getGoalProgressPercentage() {
        if (totalGoals == 0) return 0.0;
        return Math.round((completedGoals * 100.0 / totalGoals) * 100.0) / 100.0;
    }

    public double getActivityProgressPercentage() {
        if (totalActivities == 0) return 0.0;
        return Math.round((completedActivities * 100.0 / totalActivities) * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return "ProgressStatistics{" +
                "totalGoals=" + totalGoals +
                ", completedGoals=" + completedGoals +
                ", overdueGoals=" + overdueGoals +
                ", productivityScore=" + overallProductivityScore +
                ", calculatedAt=" + calculatedAt +
                '}';
    }
}

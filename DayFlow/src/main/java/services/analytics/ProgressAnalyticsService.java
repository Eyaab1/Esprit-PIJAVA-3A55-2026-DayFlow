package services.analytics;

import model.analytics.GoalPerformanceReport;
import model.analytics.PerformanceLevel;
import model.analytics.ProgressStatistics;
import model.goals_activity_management.Activity;
import model.goals_activity_management.Goal;
import model.goals_activity_management.Routine;
import services.goals_routines.ActivityService;
import services.goals_routines.GoalService;
import services.goals_routines.RoutineService;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Progress Analytics Service
 * Provides advanced business logic for progress tracking, performance evaluation,
 * and intelligent analysis of user productivity
 */
public class ProgressAnalyticsService {

    private Connection cnx;
    private GoalService goalService;
    private RoutineService routineService;
    private ActivityService activityService;

    public ProgressAnalyticsService() {
        this.cnx = DbConnexion.getInstance().getCnx();
        this.goalService = new GoalService();
        this.routineService = new RoutineService();
        this.activityService = new ActivityService();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. PROGRESS PERCENTAGE CALCULATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate progress percentage for a goal based on completed activities
     * Business Rule: progress = (completed activities / total activities) * 100
     */
    public int calculateGoalProgress(int goalId) throws SQLException {
        String sql = """
                SELECT 
                    COUNT(a.id) as total_activities,
                    COUNT(CASE WHEN LOWER(TRIM(a.status)) = 'completed' THEN 1 END) as completed_activities
                FROM activity a
                INNER JOIN routine r ON r.id = a.routine_id
                WHERE r.goal_id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total_activities");
                    int completed = rs.getInt("completed_activities");
                    
                    if (total == 0) return 0;
                    return (int) Math.round((completed * 100.0) / total);
                }
            }
        }
        return 0;
    }

    /**
     * Calculate progress percentage for a routine based on completed activities
     */
    public int calculateRoutineProgress(int routineId) throws SQLException {
        String sql = """
                SELECT 
                    COUNT(id) as total_activities,
                    COUNT(CASE WHEN LOWER(TRIM(status)) = 'completed' THEN 1 END) as completed_activities
                FROM activity
                WHERE routine_id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, routineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total_activities");
                    int completed = rs.getInt("completed_activities");
                    
                    if (total == 0) return 0;
                    return (int) Math.round((completed * 100.0) / total);
                }
            }
        }
        return 0;
    }

    /**
     * Calculate overall user productivity score
     * Business Rule: weighted average of goal completion, activity completion, and on-time delivery
     */
    public double calculateProductivityScore(int userId) throws SQLException {
        ProgressStatistics stats = getProgressStatistics(userId);
        
        double goalWeight = 0.4;
        double activityWeight = 0.4;
        double timelinessWeight = 0.2;
        
        double goalScore = stats.getGoalCompletionRate();
        double activityScore = stats.getActivityCompletionRate();
        double timelinessScore = stats.getOnTimeCompletionRate();
        
        double productivityScore = (goalScore * goalWeight) + 
                                   (activityScore * activityWeight) + 
                                   (timelinessScore * timelinessWeight);
        
        return Math.round(productivityScore * 100.0) / 100.0;
    }

    /**
     * Update goal progress automatically
     * This should be called whenever an activity status changes
     */
    public void updateGoalProgress(int goalId) throws SQLException {
        int calculatedProgress = calculateGoalProgress(goalId);
        
        String sql = "UPDATE goal SET progress = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, calculatedProgress);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, goalId);
            ps.executeUpdate();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. COMPLETION STATISTICS DASHBOARD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get comprehensive progress statistics for a user
     */
    public ProgressStatistics getProgressStatistics(int userId) throws SQLException {
        ProgressStatistics stats = new ProgressStatistics();
        stats.setUserId(userId);

        // Goal Statistics
        String goalSql = """
                SELECT 
                    COUNT(*) as total,
                    COUNT(CASE WHEN LOWER(TRIM(status)) = 'completed' THEN 1 END) as completed,
                    COUNT(CASE WHEN LOWER(TRIM(status)) = 'active' THEN 1 END) as active,
                    COUNT(CASE WHEN LOWER(TRIM(status)) = 'paused' THEN 1 END) as paused,
                    COUNT(CASE WHEN LOWER(TRIM(status)) = 'draft' THEN 1 END) as draft,
                    COUNT(CASE WHEN deadline IS NOT NULL AND deadline < NOW() AND progress < 100 THEN 1 END) as overdue
                FROM goal
                WHERE user_id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(goalSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalGoals(rs.getInt("total"));
                    stats.setCompletedGoals(rs.getInt("completed"));
                    stats.setActiveGoals(rs.getInt("active"));
                    stats.setPausedGoals(rs.getInt("paused"));
                    stats.setDraftGoals(rs.getInt("draft"));
                    stats.setOverdueGoals(rs.getInt("overdue"));
                }
            }
        }

        // Routine Statistics
        String routineSql = """
                SELECT 
                    COUNT(*) as total,
                    COUNT(CASE WHEN LOWER(TRIM(r.status)) = 'active' THEN 1 END) as active,
                    COUNT(CASE WHEN LOWER(TRIM(r.status)) = 'completed' THEN 1 END) as completed
                FROM routine r
                INNER JOIN goal g ON g.id = r.goal_id
                WHERE g.user_id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(routineSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalRoutines(rs.getInt("total"));
                    stats.setActiveRoutines(rs.getInt("active"));
                    stats.setCompletedRoutines(rs.getInt("completed"));
                }
            }
        }

        // Activity Statistics
        String activitySql = """
                SELECT 
                    COUNT(*) as total,
                    COUNT(CASE WHEN LOWER(TRIM(a.status)) = 'completed' THEN 1 END) as completed,
                    COUNT(CASE WHEN LOWER(TRIM(a.status)) = 'pending' THEN 1 END) as pending,
                    COUNT(CASE WHEN LOWER(TRIM(a.status)) = 'in_progress' THEN 1 END) as in_progress
                FROM activity a
                INNER JOIN routine r ON r.id = a.routine_id
                INNER JOIN goal g ON g.id = r.goal_id
                WHERE g.user_id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(activitySql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalActivities(rs.getInt("total"));
                    stats.setCompletedActivities(rs.getInt("completed"));
                    stats.setPendingActivities(rs.getInt("pending"));
                    stats.setInProgressActivities(rs.getInt("in_progress"));
                }
            }
        }

        // Deadline Statistics
        String deadlineSql = """
                SELECT 
                    COUNT(CASE WHEN deadline < NOW() AND progress < 100 THEN 1 END) as missed,
                    COUNT(CASE WHEN deadline > NOW() AND deadline < (NOW() + INTERVAL '7 days') THEN 1 END) as upcoming
                FROM goal
                WHERE user_id = ? AND deadline IS NOT NULL
                """;

        try (PreparedStatement ps = cnx.prepareStatement(deadlineSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setMissedDeadlines(rs.getInt("missed"));
                    stats.setUpcomingDeadlines(rs.getInt("upcoming"));
                }
            }
        }

        // Weekly Statistics
        String weeklySql = """
                SELECT 
                    COUNT(CASE WHEN LOWER(TRIM(status)) = 'completed' AND updated_at >= (NOW() - INTERVAL '7 days') THEN 1 END) as goals_week,
                    (SELECT COUNT(*) FROM activity a
                     INNER JOIN routine r ON r.id = a.routine_id
                     INNER JOIN goal g ON g.id = r.goal_id
                     WHERE g.user_id = ? AND LOWER(TRIM(a.status)) = 'completed' 
                     AND a.completed_date >= (NOW() - INTERVAL '7 days')) as activities_week
                FROM goal
                WHERE user_id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(weeklySql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setGoalsCompletedThisWeek(rs.getInt("goals_week"));
                    stats.setActivitiesCompletedThisWeek(rs.getInt("activities_week"));
                }
            }
        }

        // Monthly Statistics
        String monthlySql = """
                SELECT 
                    COUNT(CASE WHEN LOWER(TRIM(status)) = 'completed' AND updated_at >= (NOW() - INTERVAL '30 days') THEN 1 END) as goals_month,
                    (SELECT COUNT(*) FROM activity a
                     INNER JOIN routine r ON r.id = a.routine_id
                     INNER JOIN goal g ON g.id = r.goal_id
                     WHERE g.user_id = ? AND LOWER(TRIM(a.status)) = 'completed' 
                     AND a.completed_date >= (NOW() - INTERVAL '30 days')) as activities_month
                FROM goal
                WHERE user_id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(monthlySql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setGoalsCompletedThisMonth(rs.getInt("goals_month"));
                    stats.setActivitiesCompletedThisMonth(rs.getInt("activities_month"));
                }
            }
        }

        // Calculate Performance Metrics
        if (stats.getTotalGoals() > 0) {
            stats.setGoalCompletionRate(
                Math.round((stats.getCompletedGoals() * 100.0 / stats.getTotalGoals()) * 100.0) / 100.0
            );
        }

        if (stats.getTotalActivities() > 0) {
            stats.setActivityCompletionRate(
                Math.round((stats.getCompletedActivities() * 100.0 / stats.getTotalActivities()) * 100.0) / 100.0
            );
        }

        // On-time completion rate (goals completed before deadline)
        String onTimeSql = """
                SELECT 
                    COUNT(CASE WHEN deadline IS NOT NULL THEN 1 END) as total_with_deadline,
                    COUNT(CASE WHEN deadline IS NOT NULL AND updated_at <= deadline AND LOWER(TRIM(status)) = 'completed' THEN 1 END) as on_time
                FROM goal
                WHERE user_id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(onTimeSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int totalWithDeadline = rs.getInt("total_with_deadline");
                    int onTime = rs.getInt("on_time");
                    if (totalWithDeadline > 0) {
                        stats.setOnTimeCompletionRate(
                            Math.round((onTime * 100.0 / totalWithDeadline) * 100.0) / 100.0
                        );
                    }
                }
            }
        }

        // Calculate Overall Productivity Score
        stats.setOverallProductivityScore(calculateProductivityScore(userId));

        return stats;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. DELAYED GOAL DETECTION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Detect and mark overdue goals
     * Business Rule: if current date > deadline AND progress < 100% then status = OVERDUE
     */
    public List<Goal> detectOverdueGoals() throws SQLException {
        String sql = """
                SELECT id, title, deadline, progress, status, user_id
                FROM goal
                WHERE deadline IS NOT NULL 
                AND deadline < NOW()
                AND progress < 100
                AND LOWER(TRIM(status)) NOT IN ('completed', 'failed', 'archived')
                """;

        List<Goal> overdueGoals = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Goal goal = new Goal();
                goal.setId(rs.getInt("id"));
                goal.setTitleDirect(rs.getString("title"));
                goal.setDeadline(rs.getTimestamp("deadline").toLocalDateTime());
                goal.setProgress(rs.getInt("progress"));
                goal.setStatus(rs.getString("status"));
                overdueGoals.add(goal);
            }
        }

        return overdueGoals;
    }

    /**
     * Mark a goal as overdue
     */
    public void markGoalAsOverdue(int goalId) throws SQLException {
        String sql = "UPDATE goal SET status = 'failed', updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, goalId);
            ps.executeUpdate();
        }
    }

    // Continued in next message...

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. INACTIVE ROUTINE DETECTION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Detect inactive routines (no completed activities for 7+ days)
     * Business Rule: if no progress for 7+ days then routine becomes INACTIVE
     */
    public List<Routine> detectInactiveRoutines(int daysThreshold) throws SQLException {
        String sql = """
                SELECT DISTINCT r.id, r.title, r.status, r.goal_id,
                       MAX(a.completed_date) as last_activity_date
                FROM routine r
                LEFT JOIN activity a ON a.routine_id = r.id AND LOWER(TRIM(a.status)) = 'completed'
                WHERE LOWER(TRIM(r.status)) = 'active'
                GROUP BY r.id, r.title, r.status, r.goal_id
                HAVING MAX(a.completed_date) < (NOW() - INTERVAL '? days') OR MAX(a.completed_date) IS NULL
                """;

        List<Routine> inactiveRoutines = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql.replace("?", String.valueOf(daysThreshold)))) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Routine routine = new Routine();
                    routine.setId(rs.getInt("id"));
                    routine.setTitle(rs.getString("title"));
                    routine.setStatus(rs.getString("status"));
                    inactiveRoutines.add(routine);
                }
            }
        }

        return inactiveRoutines;
    }

    /**
     * Mark a routine as inactive
     */
    public void markRoutineAsInactive(int routineId) throws SQLException {
        String sql = "UPDATE routine SET status = 'paused', updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, routineId);
            ps.executeUpdate();
        }
    }

    /**
     * Get days since last activity for a routine
     */
    public int getDaysSinceLastActivity(int routineId) throws SQLException {
        String sql = """
                SELECT MAX(completed_date) as last_completed
                FROM activity
                WHERE routine_id = ? AND LOWER(TRIM(status)) = 'completed'
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, routineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp lastCompleted = rs.getTimestamp("last_completed");
                    if (lastCompleted != null) {
                        return (int) ChronoUnit.DAYS.between(
                            lastCompleted.toLocalDateTime(),
                            LocalDateTime.now()
                        );
                    }
                }
            }
        }
        return -1; // No completed activities
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. SMART DEADLINE WARNING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Detect goals that need deadline warnings
     * Business Rule: deadline in less than 3 days AND progress < 50%
     */
    public List<Goal> detectGoalsNeedingWarning() throws SQLException {
        String sql = """
                SELECT id, title, deadline, progress, status, user_id
                FROM goal
                WHERE deadline IS NOT NULL
                AND deadline > NOW()
                AND deadline < (NOW() + INTERVAL '3 days')
                AND progress < 50
                AND LOWER(TRIM(status)) IN ('active', 'draft')
                """;

        List<Goal> goalsNeedingWarning = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Goal goal = new Goal();
                goal.setId(rs.getInt("id"));
                goal.setTitleDirect(rs.getString("title"));
                goal.setDeadline(rs.getTimestamp("deadline").toLocalDateTime());
                goal.setProgress(rs.getInt("progress"));
                goal.setStatus(rs.getString("status"));
                goalsNeedingWarning.add(goal);
            }
        }

        return goalsNeedingWarning;
    }

    /**
     * Generate warning message for a goal
     */
    public String generateWarningMessage(Goal goal) {
        if (goal.getDeadline() == null) {
            return null;
        }

        long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDateTime.now(), goal.getDeadline());
        int progress = goal.getProgress();

        if (daysUntilDeadline < 0) {
            return "⚠️ OVERDUE: This goal has passed its deadline!";
        } else if (daysUntilDeadline == 0) {
            return "🚨 URGENT: Deadline is TODAY and progress is only " + progress + "%!";
        } else if (daysUntilDeadline <= 3 && progress < 50) {
            return "⚠️ WARNING: Deadline in " + daysUntilDeadline + " days but progress is only " + progress + "%!";
        } else if (daysUntilDeadline <= 7 && progress < 30) {
            return "⚠️ ATTENTION: Deadline approaching in " + daysUntilDeadline + " days with low progress (" + progress + "%)";
        }

        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 6. PERFORMANCE SUMMARY REPORT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generate comprehensive performance report for a goal
     */
    public GoalPerformanceReport generatePerformanceReport(int goalId) throws SQLException {
        GoalPerformanceReport report = new GoalPerformanceReport();

        // Get goal details
        String goalSql = """
                SELECT id, title, progress, status, deadline, created_at
                FROM goal
                WHERE id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(goalSql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    report.setGoalId(rs.getInt("id"));
                    report.setGoalTitle(rs.getString("title"));
                    report.setProgress(rs.getInt("progress"));
                    report.setStatus(rs.getString("status"));
                    
                    Timestamp deadline = rs.getTimestamp("deadline");
                    if (deadline != null) {
                        report.setDeadline(deadline.toLocalDateTime());
                    }
                    
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        report.setCreatedAt(createdAt.toLocalDateTime());
                    }
                }
            }
        }

        // Get activity metrics
        String activitySql = """
                SELECT 
                    COUNT(*) as total,
                    COUNT(CASE WHEN LOWER(TRIM(a.status)) = 'completed' THEN 1 END) as completed,
                    COUNT(CASE WHEN LOWER(TRIM(a.status)) != 'completed' THEN 1 END) as pending
                FROM activity a
                INNER JOIN routine r ON r.id = a.routine_id
                WHERE r.goal_id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(activitySql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    report.setTotalActivities(rs.getInt("total"));
                    report.setCompletedActivities(rs.getInt("completed"));
                    report.setPendingActivities(rs.getInt("pending"));
                }
            }
        }

        // Get routine metrics
        String routineSql = """
                SELECT 
                    COUNT(*) as total,
                    COUNT(CASE WHEN LOWER(TRIM(status)) = 'completed' THEN 1 END) as completed,
                    COUNT(CASE WHEN LOWER(TRIM(status)) = 'active' THEN 1 END) as active
                FROM routine
                WHERE goal_id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(routineSql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    report.setTotalRoutines(rs.getInt("total"));
                    report.setCompletedRoutines(rs.getInt("completed"));
                    report.setActiveRoutines(rs.getInt("active"));
                }
            }
        }

        // Calculate metrics
        if (report.getTotalActivities() > 0) {
            report.setCompletionRate(
                Math.round((report.getCompletedActivities() * 100.0 / report.getTotalActivities()) * 100.0) / 100.0
            );
        }

        // Calculate days until deadline
        if (report.getDeadline() != null) {
            long days = ChronoUnit.DAYS.between(LocalDateTime.now(), report.getDeadline());
            report.setDaysUntilDeadline((int) days);
            report.setOverdue(days < 0);
        }

        // Determine performance level
        report.setPerformanceLevel(evaluatePerformanceLevel(report));

        // Generate warning and recommendation
        report.setWarningMessage(generateWarningForReport(report));
        report.setRecommendation(generateRecommendation(report));
        report.setNeedsAttention(report.getPerformanceLevel() == PerformanceLevel.NEEDS_ATTENTION || 
                                 report.getPerformanceLevel() == PerformanceLevel.CRITICAL_DELAY);

        // Check if at risk
        report.setAtRisk(isGoalAtRisk(report));

        return report;
    }

    /**
     * Evaluate performance level based on multiple factors
     */
    private PerformanceLevel evaluatePerformanceLevel(GoalPerformanceReport report) {
        int progress = report.getProgress();
        int daysUntilDeadline = report.getDaysUntilDeadline();
        boolean isOverdue = report.isOverdue();

        // Critical Delay: Overdue or deadline very close with low progress
        if (isOverdue || (daysUntilDeadline <= 1 && progress < 70)) {
            return PerformanceLevel.CRITICAL_DELAY;
        }

        // Needs Attention: Deadline approaching with insufficient progress
        if (daysUntilDeadline <= 3 && progress < 50) {
            return PerformanceLevel.NEEDS_ATTENTION;
        }

        if (daysUntilDeadline <= 7 && progress < 30) {
            return PerformanceLevel.NEEDS_ATTENTION;
        }

        // Excellent Progress: High progress or completed
        if (progress >= 80 || "completed".equalsIgnoreCase(report.getStatus())) {
            return PerformanceLevel.EXCELLENT_PROGRESS;
        }

        // On Track: Reasonable progress for timeline
        if (report.getDeadline() != null && report.getCreatedAt() != null) {
            long totalDays = ChronoUnit.DAYS.between(report.getCreatedAt(), report.getDeadline());
            long elapsedDays = ChronoUnit.DAYS.between(report.getCreatedAt(), LocalDateTime.now());
            
            if (totalDays > 0) {
                double expectedProgress = (elapsedDays * 100.0) / totalDays;
                if (progress >= expectedProgress * 0.8) { // Within 80% of expected
                    return PerformanceLevel.ON_TRACK;
                }
            }
        }

        // Default to On Track if progress is reasonable
        if (progress >= 40) {
            return PerformanceLevel.ON_TRACK;
        }

        return PerformanceLevel.NEEDS_ATTENTION;
    }

    /**
     * Generate warning message for performance report
     */
    private String generateWarningForReport(GoalPerformanceReport report) {
        if (report.isOverdue()) {
            return "🚨 This goal is OVERDUE! Immediate action required.";
        }

        if (report.getDaysUntilDeadline() <= 1 && report.getProgress() < 70) {
            return "🚨 URGENT: Deadline is in " + report.getDaysUntilDeadline() + " day(s) with only " + 
                   report.getProgress() + "% progress!";
        }

        if (report.getDaysUntilDeadline() <= 3 && report.getProgress() < 50) {
            return "⚠️ WARNING: Deadline approaching in " + report.getDaysUntilDeadline() + 
                   " days but progress is only " + report.getProgress() + "%";
        }

        if (report.getPendingActivities() > report.getCompletedActivities() * 2) {
            return "⚠️ Many pending activities remaining. Consider prioritizing completion.";
        }

        return null;
    }

    /**
     * Generate recommendation based on performance
     */
    private String generateRecommendation(GoalPerformanceReport report) {
        PerformanceLevel level = report.getPerformanceLevel();

        switch (level) {
            case EXCELLENT_PROGRESS:
                return "✅ Great work! Keep up the momentum to complete this goal.";
            
            case ON_TRACK:
                return "👍 You're on track. Continue with your current pace.";
            
            case NEEDS_ATTENTION:
                if (report.getDaysUntilDeadline() > 0) {
                    return "⚠️ Increase your effort to meet the deadline. Focus on completing pending activities.";
                } else {
                    return "⚠️ Consider extending the deadline or adjusting the goal scope.";
                }
            
            case CRITICAL_DELAY:
                if (report.isOverdue()) {
                    return "🚨 This goal is overdue. Reassess priorities and consider if this goal is still relevant.";
                } else {
                    return "🚨 Immediate action required! Prioritize this goal's activities to avoid missing the deadline.";
                }
            
            default:
                return "Continue working on your goal activities.";
        }
    }

    /**
     * Check if goal is at risk
     */
    private boolean isGoalAtRisk(GoalPerformanceReport report) {
        if (report.isOverdue()) return true;
        if (report.getDaysUntilDeadline() <= 3 && report.getProgress() < 50) return true;
        if (report.getDaysUntilDeadline() <= 7 && report.getProgress() < 30) return true;
        return false;
    }

    /**
     * Get performance reports for all user goals
     */
    public List<GoalPerformanceReport> getAllPerformanceReports(int userId) throws SQLException {
        List<GoalPerformanceReport> reports = new ArrayList<>();
        
        String sql = "SELECT id FROM goal WHERE user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int goalId = rs.getInt("id");
                    reports.add(generatePerformanceReport(goalId));
                }
            }
        }

        return reports;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 7. HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get goals that need attention (at risk or overdue)
     */
    public List<GoalPerformanceReport> getGoalsNeedingAttention(int userId) throws SQLException {
        List<GoalPerformanceReport> allReports = getAllPerformanceReports(userId);
        return allReports.stream()
                .filter(GoalPerformanceReport::isNeedsAttention)
                .toList();
    }

    /**
     * Get top performing goals
     */
    public List<GoalPerformanceReport> getTopPerformingGoals(int userId, int limit) throws SQLException {
        List<GoalPerformanceReport> allReports = getAllPerformanceReports(userId);
        return allReports.stream()
                .filter(r -> r.getPerformanceLevel() == PerformanceLevel.EXCELLENT_PROGRESS)
                .limit(limit)
                .toList();
    }
}

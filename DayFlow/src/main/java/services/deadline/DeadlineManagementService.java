package services.deadline;

import model.goals_activity_management.Goal;
import model.goals_activity_management.Routine;
import model.goals_activity_management.Activity;
import model.notification.Notification;
import model.notification.ReminderLog;
import services.goals_routines.GoalService;
import services.goals_routines.RoutineService;
import services.goals_routines.ActivityService;
import services.notification.NotificationService;
import services.notification.ReminderService;
import services.chatroom.GoalParticipationService;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Advanced Deadline Management Service
 * Handles deadline processing, reminder scheduling, and notification generation.
 * This is the core business logic for the deadline management system.
 */
public class DeadlineManagementService {

    private final GoalService goalService;
    private final RoutineService routineService;
    private final ActivityService activityService;
    private final NotificationService notificationService;
    private final ReminderService reminderService;
    private final GoalParticipationService participationService;
    private final Connection cnx;

    // Reminder thresholds (in hours)
    private static final int REMINDER_24H = 24;
    private static final int REMINDER_1H = 1;

    public DeadlineManagementService() {
        this.cnx = DbConnexion.getInstance().getCnx();
        this.goalService = new GoalService();
        this.routineService = new RoutineService();
        this.activityService = new ActivityService();
        this.notificationService = new NotificationService();
        this.reminderService = new ReminderService();
        this.participationService = new GoalParticipationService();
    }

    /**
     * Main scheduler method - runs periodically to check deadlines and send reminders
     * Should be called every 5-10 minutes by a scheduled task
     */
    public void processDeadlines() {
        try {
            System.out.println("[DeadlineManagementService] Starting deadline processing at " + LocalDateTime.now());
            
            // Process goals
            processGoalDeadlines();
            
            // Process routines
            processRoutineDeadlines();
            
            // Process activities
            processActivityDeadlines();
            
            // Mark overdue entities
            markOverdueEntities();
            
            System.out.println("[DeadlineManagementService] Deadline processing completed");
        } catch (Exception e) {
            System.err.println("[DeadlineManagementService] Error processing deadlines: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Process goal deadlines and send reminders
     */
    private void processGoalDeadlines() throws SQLException {
        // Get all goals from dashboard
        List<GoalService.GoalListRow> goals = goalService.findAllForDashboard();
        
        for (GoalService.GoalListRow row : goals) {
            Goal goal = row.goal();
            if (goal.getDeadline() == null) continue;
            if (!isEligibleForReminder(goal.getStatus())) continue;
            
            LocalDateTime deadline = goal.getDeadline();
            LocalDateTime now = LocalDateTime.now();
            
            // Get all participants (owner + approved members)
            List<Integer> userIds = getEligibleUsers(goal.getId());
            
            for (Integer userId : userIds) {
                // Check 24-hour reminder
                if (shouldSendReminder(now, deadline, REMINDER_24H)) {
                    if (!reminderService.hasReminderBeenSent(userId, "goal", goal.getId(), 
                            ReminderLog.ReminderType.REMINDER_24H, deadline)) {
                        sendGoalReminder(userId, goal, "24 hours", ReminderLog.ReminderType.REMINDER_24H);
                    }
                }
                
                // Check 1-hour reminder
                if (shouldSendReminder(now, deadline, REMINDER_1H)) {
                    if (!reminderService.hasReminderBeenSent(userId, "goal", goal.getId(), 
                            ReminderLog.ReminderType.REMINDER_1H, deadline)) {
                        sendGoalReminder(userId, goal, "1 hour", ReminderLog.ReminderType.REMINDER_1H);
                    }
                }
                
                // Check deadline reached
                if (isDeadlineReached(now, deadline) && !isDeadlineMissed(now, deadline)) {
                    if (!reminderService.hasReminderBeenSent(userId, "goal", goal.getId(), 
                            ReminderLog.ReminderType.DEADLINE_REACHED, deadline)) {
                        sendGoalReminder(userId, goal, "reached", ReminderLog.ReminderType.DEADLINE_REACHED);
                    }
                }
                
                // Check deadline missed
                if (isDeadlineMissed(now, deadline)) {
                    if (!reminderService.hasReminderBeenSent(userId, "goal", goal.getId(), 
                            ReminderLog.ReminderType.DEADLINE_MISSED, deadline)) {
                        sendGoalReminder(userId, goal, "missed", ReminderLog.ReminderType.DEADLINE_MISSED);
                    }
                }
            }
        }
    }

    /**
     * Process routine deadlines and send reminders
     */
    private void processRoutineDeadlines() throws SQLException {
        // Get all goals to iterate through their routines
        List<GoalService.GoalListRow> goals = goalService.findAllForDashboard();
        
        for (GoalService.GoalListRow row : goals) {
            Goal goal = row.goal();
            List<Routine> routines = routineService.findByGoalId(goal.getId());
            
            for (Routine routine : routines) {
                if (routine.getDeadline() == null) continue;
                if (!isEligibleForReminder(routine.getStatus())) continue;
                
                LocalDateTime deadline = routine.getDeadline();
                LocalDateTime now = LocalDateTime.now();
                
                // Get all participants of the parent goal
                List<Integer> userIds = getEligibleUsers(goal.getId());
                
                for (Integer userId : userIds) {
                    // Check 24-hour reminder
                    if (shouldSendReminder(now, deadline, REMINDER_24H)) {
                        if (!reminderService.hasReminderBeenSent(userId, "routine", routine.getId(), 
                                ReminderLog.ReminderType.REMINDER_24H, deadline)) {
                            sendRoutineReminder(userId, routine, "24 hours", ReminderLog.ReminderType.REMINDER_24H);
                        }
                    }
                    
                    // Check 1-hour reminder
                    if (shouldSendReminder(now, deadline, REMINDER_1H)) {
                        if (!reminderService.hasReminderBeenSent(userId, "routine", routine.getId(), 
                                ReminderLog.ReminderType.REMINDER_1H, deadline)) {
                            sendRoutineReminder(userId, routine, "1 hour", ReminderLog.ReminderType.REMINDER_1H);
                        }
                    }
                    
                    // Check deadline reached
                    if (isDeadlineReached(now, deadline) && !isDeadlineMissed(now, deadline)) {
                        if (!reminderService.hasReminderBeenSent(userId, "routine", routine.getId(), 
                                ReminderLog.ReminderType.DEADLINE_REACHED, deadline)) {
                            sendRoutineReminder(userId, routine, "reached", ReminderLog.ReminderType.DEADLINE_REACHED);
                        }
                    }
                    
                    // Check deadline missed
                    if (isDeadlineMissed(now, deadline)) {
                        if (!reminderService.hasReminderBeenSent(userId, "routine", routine.getId(), 
                                ReminderLog.ReminderType.DEADLINE_MISSED, deadline)) {
                            sendRoutineReminder(userId, routine, "missed", ReminderLog.ReminderType.DEADLINE_MISSED);
                        }
                    }
                }
            }
        }
    }

    /**
     * Process activity deadlines and send reminders
     */
    private void processActivityDeadlines() throws SQLException {
        // Get all goals to iterate through their routines and activities
        List<GoalService.GoalListRow> goals = goalService.findAllForDashboard();
        
        for (GoalService.GoalListRow row : goals) {
            Goal goal = row.goal();
            List<Routine> routines = routineService.findByGoalId(goal.getId());
            
            for (Routine routine : routines) {
                List<Activity> activities = activityService.findByRoutineId(routine.getId());
                
                for (Activity activity : activities) {
                    if (activity.getDeadline() == null) continue;
                    if (!isEligibleForReminder(activity.getStatus())) continue;
                    
                    LocalDateTime deadline = activity.getDeadline();
                    LocalDateTime now = LocalDateTime.now();
                    
                    // Get participants through parent goal
                    List<Integer> userIds = getEligibleUsers(goal.getId());
                    
                    for (Integer userId : userIds) {
                        // Check 24-hour reminder
                        if (shouldSendReminder(now, deadline, REMINDER_24H)) {
                            if (!reminderService.hasReminderBeenSent(userId, "activity", activity.getId(), 
                                    ReminderLog.ReminderType.REMINDER_24H, deadline)) {
                                sendActivityReminder(userId, activity, "24 hours", ReminderLog.ReminderType.REMINDER_24H);
                            }
                        }
                        
                        // Check 1-hour reminder
                        if (shouldSendReminder(now, deadline, REMINDER_1H)) {
                            if (!reminderService.hasReminderBeenSent(userId, "activity", activity.getId(), 
                                    ReminderLog.ReminderType.REMINDER_1H, deadline)) {
                                sendActivityReminder(userId, activity, "1 hour", ReminderLog.ReminderType.REMINDER_1H);
                            }
                        }
                        
                        // Check deadline reached
                        if (isDeadlineReached(now, deadline) && !isDeadlineMissed(now, deadline)) {
                            if (!reminderService.hasReminderBeenSent(userId, "activity", activity.getId(), 
                                    ReminderLog.ReminderType.DEADLINE_REACHED, deadline)) {
                                sendActivityReminder(userId, activity, "reached", ReminderLog.ReminderType.DEADLINE_REACHED);
                            }
                        }
                        
                        // Check deadline missed
                        if (isDeadlineMissed(now, deadline)) {
                            if (!reminderService.hasReminderBeenSent(userId, "activity", activity.getId(), 
                                    ReminderLog.ReminderType.DEADLINE_MISSED, deadline)) {
                                sendActivityReminder(userId, activity, "missed", ReminderLog.ReminderType.DEADLINE_MISSED);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Mark overdue entities and update their status
     */
    private void markOverdueEntities() throws SQLException {
        // This would be handled by database triggers or manual updates
        System.out.println("[DeadlineManagementService] Checking for overdue entities");
    }

    /**
     * Send goal reminder notification
     */
    private void sendGoalReminder(int userId, Goal goal, String timeframe, ReminderLog.ReminderType reminderType) throws SQLException {
        String title = "Goal Deadline Reminder: " + goal.getTitle();
        String message = "Your goal '" + goal.getTitle() + "' deadline is " + timeframe + " away.";
        
        Notification notification = new Notification(
            userId,
            mapReminderTypeToNotificationType(reminderType),
            Notification.EntityType.GOAL,
            goal.getId(),
            title,
            message,
            "/user/goals_routines/goal_detail.fxml?id=" + goal.getId()
        );
        
        notificationService.insert(notification);
        
        ReminderLog reminderLog = new ReminderLog(userId, "goal", goal.getId(), reminderType, goal.getDeadline());
        reminderService.insert(reminderLog);
        
        System.out.println("[DeadlineManagementService] Sent reminder to user " + userId + " for goal " + goal.getId());
    }

    /**
     * Send routine reminder notification
     */
    private void sendRoutineReminder(int userId, Routine routine, String timeframe, ReminderLog.ReminderType reminderType) throws SQLException {
        String title = "Routine Deadline Reminder: " + routine.getTitle();
        String message = "Your routine '" + routine.getTitle() + "' deadline is " + timeframe + " away.";
        
        Notification notification = new Notification(
            userId,
            mapReminderTypeToNotificationType(reminderType),
            Notification.EntityType.ROUTINE,
            routine.getId(),
            title,
            message
        );
        
        notificationService.insert(notification);
        
        ReminderLog reminderLog = new ReminderLog(userId, "routine", routine.getId(), reminderType, routine.getDeadline());
        reminderService.insert(reminderLog);
        
        System.out.println("[DeadlineManagementService] Sent reminder to user " + userId + " for routine " + routine.getId());
    }

    /**
     * Send activity reminder notification
     */
    private void sendActivityReminder(int userId, Activity activity, String timeframe, ReminderLog.ReminderType reminderType) throws SQLException {
        String title = "Activity Deadline Reminder: " + activity.getTitle();
        String message = "Your activity '" + activity.getTitle() + "' deadline is " + timeframe + " away.";
        
        Notification notification = new Notification(
            userId,
            mapReminderTypeToNotificationType(reminderType),
            Notification.EntityType.ACTIVITY,
            activity.getId(),
            title,
            message
        );
        
        notificationService.insert(notification);
        
        ReminderLog reminderLog = new ReminderLog(userId, "activity", activity.getId(), reminderType, activity.getDeadline());
        reminderService.insert(reminderLog);
        
        System.out.println("[DeadlineManagementService] Sent reminder to user " + userId + " for activity " + activity.getId());
    }

    /**
     * Get eligible users for a goal (owner + approved participants)
     */
    private List<Integer> getEligibleUsers(int goalId) throws SQLException {
        String sql = """
                SELECT DISTINCT gp.user_id
                FROM goal_participation gp
                WHERE gp.goal_id = ? AND gp.status = 'accepted'
                """;
        
        List<Integer> userIds = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getInt("user_id"));
                }
            }
        }
        return userIds;
    }

    /**
     * Check if entity status is eligible for reminders
     */
    private boolean isEligibleForReminder(String status) {
        if (status == null) return false;
        String lower = status.toLowerCase();
        return lower.equals("active") || lower.equals("in_progress") || lower.equals("draft");
    }

    /**
     * Check if reminder should be sent based on time threshold
     */
    private boolean shouldSendReminder(LocalDateTime now, LocalDateTime deadline, int hoursThreshold) {
        long hoursDiff = ChronoUnit.HOURS.between(now, deadline);
        return hoursDiff <= hoursThreshold && hoursDiff > (hoursThreshold - 1);
    }

    /**
     * Check if deadline has been reached (within 1 minute)
     */
    private boolean isDeadlineReached(LocalDateTime now, LocalDateTime deadline) {
        long minutesDiff = ChronoUnit.MINUTES.between(now, deadline);
        return minutesDiff <= 0 && minutesDiff > -1;
    }

    /**
     * Check if deadline has been missed
     */
    private boolean isDeadlineMissed(LocalDateTime now, LocalDateTime deadline) {
        return now.isAfter(deadline);
    }

    /**
     * Map ReminderType to NotificationType
     */
    private Notification.NotificationType mapReminderTypeToNotificationType(ReminderLog.ReminderType reminderType) {
        return switch (reminderType) {
            case REMINDER_24H -> Notification.NotificationType.DEADLINE_24H;
            case REMINDER_1H -> Notification.NotificationType.DEADLINE_1H;
            case DEADLINE_REACHED -> Notification.NotificationType.DEADLINE_REACHED;
            case DEADLINE_MISSED -> Notification.NotificationType.DEADLINE_MISSED;
        };
    }

    /**
     * Recalculate reminders when deadline is updated
     */
    public void recalculateReminders(String entityType, int entityId, LocalDateTime oldDeadline, LocalDateTime newDeadline) throws SQLException {
        // Delete old reminders
        reminderService.deleteByEntity(entityType, entityId);
        
        System.out.println("[DeadlineManagementService] Recalculated reminders for " + entityType + " " + entityId);
    }

    /**
     * Cancel pending reminders when entity is completed
     */
    public void cancelPendingReminders(String entityType, int entityId) throws SQLException {
        reminderService.deleteByEntity(entityType, entityId);
        
        System.out.println("[DeadlineManagementService] Cancelled pending reminders for " + entityType + " " + entityId);
    }
}

package services.analytics;

import model.goals_activity_management.Goal;
import model.goals_activity_management.Routine;
import model.notification.Notification;
import model.notification.Notification.NotificationType;
import model.notification.Notification.EntityType;
import services.notification.NotificationService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Analytics Scheduler
 * Runs periodic checks for overdue goals, inactive routines, and performance warnings
 */
public class AnalyticsScheduler {

    private static AnalyticsScheduler instance;
    private ScheduledExecutorService scheduler;
    private ProgressAnalyticsService analyticsService;
    private NotificationService notificationService;
    private static final long INTERVAL_MINUTES = 30; // Run every 30 minutes

    private AnalyticsScheduler() {
        this.analyticsService = new ProgressAnalyticsService();
        this.notificationService = new NotificationService();
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "AnalyticsScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Get singleton instance
     */
    public static synchronized AnalyticsScheduler getInstance() {
        if (instance == null) {
            instance = new AnalyticsScheduler();
        }
        return instance;
    }

    /**
     * Start the scheduler
     */
    public void start() {
        System.out.println("[AnalyticsScheduler] Starting analytics scheduler...");
        
        scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    System.out.println("[AnalyticsScheduler] Running analytics check at " + LocalDateTime.now());
                    runAnalyticsChecks();
                } catch (Exception e) {
                    System.err.println("[AnalyticsScheduler] Error in scheduled task: " + e.getMessage());
                    e.printStackTrace();
                }
            },
            0,
            INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        System.out.println("[AnalyticsScheduler] Scheduler started. Running every " + INTERVAL_MINUTES + " minutes");
    }

    /**
     * Run all analytics checks
     */
    private void runAnalyticsChecks() {
        try {
            checkOverdueGoals();
            checkInactiveRoutines();
            checkDeadlineWarnings();
        } catch (Exception e) {
            System.err.println("[AnalyticsScheduler] Error running analytics checks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check for overdue goals and mark them
     */
    private void checkOverdueGoals() {
        try {
            List<Goal> overdueGoals = analyticsService.detectOverdueGoals();
            
            for (Goal goal : overdueGoals) {
                // Mark as overdue
                analyticsService.markGoalAsOverdue(goal.getId());
                
                // Create notification
                Notification notification = new Notification();
                notification.setUserId(goal.getUser() != null ? goal.getUser().getId() : 0);
                notification.setEntityType(EntityType.GOAL);
                notification.setEntityId(goal.getId());
                notification.setType(NotificationType.DEADLINE_MISSED);
                notification.setTitle("Goal Overdue");
                notification.setMessage("⚠️ Goal '" + goal.getTitle() + "' is now OVERDUE!");
                notification.setRead(false);
                notification.setCreatedAt(LocalDateTime.now());
                
                notificationService.create(notification);
                
                System.out.println("[AnalyticsScheduler] Marked goal " + goal.getId() + " as overdue");
            }
            
            if (!overdueGoals.isEmpty()) {
                System.out.println("[AnalyticsScheduler] Processed " + overdueGoals.size() + " overdue goals");
            }
        } catch (SQLException e) {
            System.err.println("[AnalyticsScheduler] Error checking overdue goals: " + e.getMessage());
        }
    }

    /**
     * Check for inactive routines
     */
    private void checkInactiveRoutines() {
        try {
            List<Routine> inactiveRoutines = analyticsService.detectInactiveRoutines(7);
            
            for (Routine routine : inactiveRoutines) {
                // Mark as inactive
                analyticsService.markRoutineAsInactive(routine.getId());
                
                // Create notification
                if (routine.getGoal() != null && routine.getGoal().getUser() != null) {
                    Notification notification = new Notification();
                    notification.setUserId(routine.getGoal().getUser().getId());
                    notification.setEntityType(EntityType.ROUTINE);
                    notification.setEntityId(routine.getId());
                    notification.setType(NotificationType.STATUS_CHANGED);
                    notification.setTitle("Routine Inactive");
                    notification.setMessage("⚠️ Routine '" + routine.getTitle() + "' has been inactive for 7+ days. Consider resuming activity.");
                    notification.setRead(false);
                    notification.setCreatedAt(LocalDateTime.now());
                    
                    notificationService.create(notification);
                }
                
                System.out.println("[AnalyticsScheduler] Marked routine " + routine.getId() + " as inactive");
            }
            
            if (!inactiveRoutines.isEmpty()) {
                System.out.println("[AnalyticsScheduler] Processed " + inactiveRoutines.size() + " inactive routines");
            }
        } catch (SQLException e) {
            System.err.println("[AnalyticsScheduler] Error checking inactive routines: " + e.getMessage());
        }
    }

    /**
     * Check for goals needing deadline warnings
     */
    private void checkDeadlineWarnings() {
        try {
            List<Goal> goalsNeedingWarning = analyticsService.detectGoalsNeedingWarning();
            
            for (Goal goal : goalsNeedingWarning) {
                String warningMessage = analyticsService.generateWarningMessage(goal);
                
                if (warningMessage != null && goal.getUser() != null) {
                    // Create notification
                    Notification notification = new Notification();
                    notification.setUserId(goal.getUser().getId());
                    notification.setEntityType(EntityType.GOAL);
                    notification.setEntityId(goal.getId());
                    notification.setType(NotificationType.DEADLINE_1H);
                    notification.setTitle("Deadline Warning");
                    notification.setMessage(warningMessage);
                    notification.setRead(false);
                    notification.setCreatedAt(LocalDateTime.now());
                    
                    notificationService.create(notification);
                    
                    System.out.println("[AnalyticsScheduler] Created warning for goal " + goal.getId());
                }
            }
            
            if (!goalsNeedingWarning.isEmpty()) {
                System.out.println("[AnalyticsScheduler] Processed " + goalsNeedingWarning.size() + " deadline warnings");
            }
        } catch (SQLException e) {
            System.err.println("[AnalyticsScheduler] Error checking deadline warnings: " + e.getMessage());
        }
    }

    /**
     * Stop the scheduler
     */
    public void stop() {
        System.out.println("[AnalyticsScheduler] Stopping analytics scheduler...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[AnalyticsScheduler] Scheduler stopped");
    }

    /**
     * Get the analytics service
     */
    public ProgressAnalyticsService getAnalyticsService() {
        return analyticsService;
    }

    /**
     * Check if scheduler is running
     */
    public boolean isRunning() {
        return !scheduler.isShutdown();
    }
}

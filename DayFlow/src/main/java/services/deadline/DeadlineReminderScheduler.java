package services.deadline;

import config.AppConfig;
import services.goals_routines.GoalService;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodic scheduler for goal deadline email reminders.
 */
public class DeadlineReminderScheduler {

    private static DeadlineReminderScheduler instance;
    private final ScheduledExecutorService scheduler;
    private final DeadlineEmailReminderWorkflowService workflowService;

    private DeadlineReminderScheduler() {
        this.workflowService = new DeadlineEmailReminderWorkflowService();
        this.workflowService.setGoalService(new GoalService()); // Set GoalService to avoid null pointer
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "DeadlineReminderScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public static synchronized DeadlineReminderScheduler getInstance() {
        if (instance == null) {
            instance = new DeadlineReminderScheduler();
        }
        return instance;
    }

    public void start() {
        boolean enabled = Boolean.parseBoolean(AppConfig.get("app.goalReminder.scheduler.enabled", "true"));
        if (!enabled) {
            System.out.println("[DeadlineReminderScheduler] Disabled by configuration");
            return;
        }
        long intervalMinutes = Long.parseLong(AppConfig.get("app.goalReminder.scheduler.intervalMinutes", "60"));
        System.out.println("[DeadlineReminderScheduler] Starting scheduler...");
        scheduler.scheduleAtFixedRate(() -> {
                    try {
                        System.out.println("[DeadlineReminderScheduler] Checking reminders at " + LocalDateTime.now());
                        workflowService.processGoalDeadlineReminders();
                    } catch (Exception e) {
                        System.err.println("[DeadlineReminderScheduler] Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                },
                0,
                Math.max(1, intervalMinutes),
                TimeUnit.MINUTES);
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public DeadlineEmailReminderWorkflowService getWorkflowService() {
        return workflowService;
    }
}

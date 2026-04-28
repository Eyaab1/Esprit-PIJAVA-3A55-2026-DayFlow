package services.deadline;

import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Deadline Scheduler - Manages scheduled deadline processing
 * Runs the deadline management service at regular intervals
 */
public class DeadlineScheduler {

    private static DeadlineScheduler instance;
    private ScheduledExecutorService scheduler;
    private DeadlineManagementService deadlineService;
    private static final long INTERVAL_MINUTES = 5; // Run every 5 minutes

    private DeadlineScheduler() {
        this.deadlineService = new DeadlineManagementService();
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "DeadlineScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Get singleton instance
     */
    public static synchronized DeadlineScheduler getInstance() {
        if (instance == null) {
            instance = new DeadlineScheduler();
        }
        return instance;
    }

    /**
     * Start the scheduler
     */
    public void start() {
        System.out.println("[DeadlineScheduler] Starting deadline scheduler...");
        
        scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    System.out.println("[DeadlineScheduler] Running deadline check at " + LocalDateTime.now());
                    deadlineService.processDeadlines();
                } catch (Exception e) {
                    System.err.println("[DeadlineScheduler] Error in scheduled task: " + e.getMessage());
                    e.printStackTrace();
                }
            },
            0,
            INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        System.out.println("[DeadlineScheduler] Scheduler started. Running every " + INTERVAL_MINUTES + " minutes");
    }

    /**
     * Stop the scheduler
     */
    public void stop() {
        System.out.println("[DeadlineScheduler] Stopping deadline scheduler...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[DeadlineScheduler] Scheduler stopped");
    }

    /**
     * Get the deadline management service
     */
    public DeadlineManagementService getDeadlineService() {
        return deadlineService;
    }

    /**
     * Check if scheduler is running
     */
    public boolean isRunning() {
        return !scheduler.isShutdown();
    }
}

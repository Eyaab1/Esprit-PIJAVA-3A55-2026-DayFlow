package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class Activity {

    // ─── Attributes ───────────────────────────────────────────

    private int id;

    private String title;                       // NotBlank, length 3–255

    private LocalDateTime startTime;            // NotNull
    private LocalTime duration;                 // NotNull (TIME type)

    private String status;                      // pending | in_progress | completed | skipped | cancelled
    private String priority;                    // low | medium | high (nullable)

    private boolean hasReminder = false;
    private LocalDateTime reminderAt;           // Required if hasReminder is true

    private LocalDate deadline;                 // Optional
    private boolean isFavorite = false;

    private LocalDateTime completedAt;          // Optional
    private Integer actualDurationMinutes;      // Optional
    private Integer plannedDurationMinutes;     // Optional

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Routine routine;

    // Valid values
    private static final List<String> VALID_STATUSES   = List.of("pending", "in_progress", "completed", "skipped", "cancelled");
    private static final List<String> VALID_PRIORITIES = List.of("low", "medium", "high");

    // ─── Constructor ──────────────────────────────────────────

    public Activity() {
        this.status      = "pending";
        this.hasReminder = false;
        this.createdAt   = LocalDateTime.now();
    }

    // ─── Validation ───────────────────────────────────────────

    public void validate() {
        validateTitle(this.title);
        validateStartTime(this.startTime);
        validateDuration(this.duration);
        validateStatus(this.status);
        validateReminder(this.hasReminder, this.reminderAt);
        if (this.priority != null) validatePriority(this.priority);
    }

    // --- Title ---
    public static void validateTitle(String title) {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Le titre est obligatoire.");
        if (title.length() < 3)
            throw new IllegalArgumentException("Le titre doit contenir au moins 3 caractères.");
        if (title.length() > 255)
            throw new IllegalArgumentException("Le titre ne peut pas dépasser 255 caractères.");
    }

    // --- Start Time ---
    public static void validateStartTime(LocalDateTime startTime) {
        if (startTime == null)
            throw new IllegalArgumentException("L'heure de début est obligatoire.");
    }

    // --- Duration ---
    public static void validateDuration(LocalTime duration) {
        if (duration == null)
            throw new IllegalArgumentException("La durée est obligatoire.");
    }

    // --- Status ---
    public static void validateStatus(String status) {
        if (status == null || !VALID_STATUSES.contains(status))
            throw new IllegalArgumentException(
                "Le statut doit être : pending, in_progress, completed, skipped ou cancelled."
            );
    }

    // --- Priority ---
    public static void validatePriority(String priority) {
        if (!VALID_PRIORITIES.contains(priority))
            throw new IllegalArgumentException("La priorité doit être : low, medium ou high.");
    }

    // --- Reminder (reminderAt required if hasReminder is true) ---
    public static void validateReminder(boolean hasReminder, LocalDateTime reminderAt) {
        if (hasReminder && reminderAt == null)
            throw new IllegalArgumentException(
                "Si un rappel est activé, la date/heure de rappel doit être définie."
            );
    }

    // ─── Setters with inline validation ───────────────────────

    public void setTitle(String title) {
        validateTitle(title);
        this.title = title;
    }

    public void setStartTime(LocalDateTime startTime) {
        validateStartTime(startTime);
        this.startTime = startTime;
    }

    public void setDuration(LocalTime duration) {
        validateDuration(duration);
        this.duration = duration;
    }

    public void setStatus(String status) {
        validateStatus(status);
        this.status = status;
    }

    public void setPriority(String priority) {
        if (priority != null) validatePriority(priority);
        this.priority = priority;
    }

    public void setHasReminder(boolean hasReminder) {
        validateReminder(hasReminder, this.reminderAt);
        this.hasReminder = hasReminder;
    }

    public void setReminderAt(LocalDateTime reminderAt) {
        this.reminderAt = reminderAt;
        // Re-check reminder consistency
        validateReminder(this.hasReminder, reminderAt);
    }

    public void setDeadline(LocalDate deadline)                     { this.deadline               = deadline; }
    public void setFavorite(boolean isFavorite)                     { this.isFavorite             = isFavorite; }
    public void setCompletedAt(LocalDateTime completedAt)           { this.completedAt            = completedAt; }
    public void setActualDurationMinutes(Integer minutes)           { this.actualDurationMinutes  = minutes; }
    public void setPlannedDurationMinutes(Integer minutes)          { this.plannedDurationMinutes = minutes; }
    public void setRoutine(Routine routine)                         { this.routine                = routine; }
    public void setUpdatedAt(LocalDateTime t)                       { this.updatedAt              = t; }

    // ─── Getters ──────────────────────────────────────────────

    public int            getId()                      { return id; }
    public String         getTitle()                   { return title; }
    public LocalDateTime  getStartTime()               { return startTime; }
    public LocalTime      getDuration()                { return duration; }
    public String         getStatus()                  { return status; }
    public String         getPriority()                { return priority; }
    public boolean        isHasReminder()              { return hasReminder; }
    public LocalDateTime  getReminderAt()              { return reminderAt; }
    public LocalDate      getDeadline()                { return deadline; }
    public boolean        isFavorite()                 { return isFavorite; }
    public LocalDateTime  getCompletedAt()             { return completedAt; }
    public Integer        getActualDurationMinutes()   { return actualDurationMinutes; }
    public Integer        getPlannedDurationMinutes()  { return plannedDurationMinutes; }
    public Routine        getRoutine()                 { return routine; }
    public LocalDateTime  getCreatedAt()               { return createdAt; }
    public LocalDateTime  getUpdatedAt()               { return updatedAt; }

    // ─── Lifecycle ────────────────────────────────────────────

    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Business helpers ─────────────────────────────────────

    public int getDurationInMinutes() {
        if (duration == null) return 0;
        return (duration.getHour() * 60) + duration.getMinute();
    }

    public Double getTimeEfficiency() {
        if (plannedDurationMinutes == null || plannedDurationMinutes == 0) return null;
        if (actualDurationMinutes  == null) return null;
        return Math.round((actualDurationMinutes * 100.0 / plannedDurationMinutes) * 100.0) / 100.0;
    }

    public boolean isCompletedEfficiently() {
        Double efficiency = getTimeEfficiency();
        return efficiency != null && efficiency <= 110;
    }

    // ─── Deadline helpers ─────────────────────────────────────

    public boolean isDeadlineNear() {
        if (deadline == null) return false;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        return days >= 0 && days <= 7;
    }

    public int getUrgencyScore() {
        int score = 0;

        if      ("high".equals(priority))   score += 30;
        else if ("medium".equals(priority)) score += 20;
        else if ("low".equals(priority))    score += 10;

        if (deadline != null) {
            long days    = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
            boolean past = LocalDate.now().isAfter(deadline);

            if      (past)       score += 70;
            else if (days <= 1)  score += 60;
            else if (days <= 3)  score += 50;
            else if (days <= 7)  score += 40;
            else if (days <= 14) score += 30;
            else if (days <= 30) score += 20;
            else                 score += 10;
        }

        return score;
    }
}
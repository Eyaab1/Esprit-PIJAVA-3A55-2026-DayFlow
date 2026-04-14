package model.goals_activity_management;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Routine {

    // ─── Attributes ───────────────────────────────────────────

    private int id;

    private String title;           // NotBlank, length 3–255
    private String description;     // Optional, max 1000 chars

    private String visibility;      // public | private
    private String status;          // draft | active | paused | completed | skipped
    private String priority;        // low | medium | high (nullable)

    private LocalDate deadline;     // Optional
    private boolean isFavorite = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Goal goal;
    private List<Activity> activities = new ArrayList<>();

    // Valid values
    private static final List<String> VALID_STATUSES     = List.of("draft", "active", "paused", "completed", "skipped");
    private static final List<String> VALID_VISIBILITIES = List.of("public", "private");
    private static final List<String> VALID_PRIORITIES   = List.of("low", "medium", "high");

    // ─── Constructor ──────────────────────────────────────────

    public Routine() {
        this.status     = "draft";
        this.visibility = "private";
        this.createdAt  = LocalDateTime.now();
    }

    // ─── Validation ───────────────────────────────────────────

    public void validate() {
        validateTitle(this.title);
        validateDescription(this.description);
        validateVisibility(this.visibility);
        validateStatus(this.status);
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

    // --- Description ---
    public static void validateDescription(String description) {
        if (description != null && description.length() > 1000)
            throw new IllegalArgumentException("La description ne peut pas dépasser 1000 caractères.");
    }

    // --- Visibility ---
    public static void validateVisibility(String visibility) {
        if (visibility == null || !VALID_VISIBILITIES.contains(visibility))
            throw new IllegalArgumentException("La visibilité doit être public ou private.");
    }

    // --- Status ---
    public static void validateStatus(String status) {
        if (status == null || !VALID_STATUSES.contains(status))
            throw new IllegalArgumentException(
                "Le statut doit être : draft, active, paused, completed ou skipped."
            );
    }

    // --- Priority ---
    public static void validatePriority(String priority) {
        if (!VALID_PRIORITIES.contains(priority))
            throw new IllegalArgumentException("La priorité doit être : low, medium ou high.");
    }

    // ─── Setters with inline validation ───────────────────────

    public void setTitle(String title) {
        validateTitle(title);
        this.title = title;
    }

    public void setDescription(String description) {
        validateDescription(description);
        this.description = description;
    }

    public void setVisibility(String visibility) {
        validateVisibility(visibility);
        this.visibility = visibility;
    }

    public void setStatus(String status) {
        validateStatus(status);
        this.status = status;
    }

    public void setPriority(String priority) {
        if (priority != null) validatePriority(priority);
        this.priority = priority;
    }

    public void setDeadline(LocalDate deadline)     { this.deadline   = deadline; }
    public void setFavorite(boolean isFavorite)     { this.isFavorite = isFavorite; }
    public void setGoal(Goal goal) {
        if (this.goal == goal) {
            return;
        }
        if (this.goal != null) {
            this.goal.getRoutines().remove(this);
        }
        this.goal = goal;
        if (goal != null && !goal.getRoutines().contains(this)) {
            goal.getRoutines().add(this);
        }
    }
    public void setUpdatedAt(LocalDateTime t)       { this.updatedAt  = t; }

    public void setId(int id) {
        this.id = id;
    }

    // ─── Getters ──────────────────────────────────────────────

    public int              getId()          { return id; }
    public String           getTitle()       { return title; }
    public String           getDescription() { return description; }
    public String           getVisibility()  { return visibility; }
    public String           getStatus()      { return status; }
    public String           getPriority()    { return priority; }
    public LocalDate        getDeadline()    { return deadline; }
    public boolean          isFavorite()     { return isFavorite; }
    public Goal             getGoal()        { return goal; }
    public List<Activity>   getActivities()  { return activities; }
    public LocalDateTime    getCreatedAt()   { return createdAt; }
    public LocalDateTime    getUpdatedAt()   { return updatedAt; }

    // ─── Lifecycle ────────────────────────────────────────────

    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addActivity(Activity activity) {
        if (activity == null || activities.contains(activity)) {
            return;
        }
        activities.add(activity);
        activity.setRoutine(this);
    }

    public void removeActivity(Activity activity) {
        if (activity == null) {
            return;
        }
        if (activities.remove(activity) && activity.getRoutine() == this) {
            activity.setRoutine(null);
        }
    }

    // ─── Business helpers ─────────────────────────────────────

    public boolean canBeExecuted() {
        return this.goal != null &&
               this.goal.canExecuteActivities() &&
               "active".equals(this.status);
    }

    public void activate() {
        if ("draft".equals(this.status) || "paused".equals(this.status))
            this.status = "active";
    }

    public void pause() {
        if ("active".equals(this.status))
            this.status = "paused";
    }

    public void updateAutoStatus() {
        if (activities.isEmpty()) return;
        boolean allDone = activities.stream()
                                    .allMatch(a -> "completed".equals(a.getStatus()));
        if (allDone && !"completed".equals(this.status))
            this.status = "completed";
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
            long days   = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
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
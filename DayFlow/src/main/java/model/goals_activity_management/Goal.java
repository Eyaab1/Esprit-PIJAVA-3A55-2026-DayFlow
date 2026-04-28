package model.goals_activity_management;

import model.chatroom.Chatroom;
import model.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Goal {

    // ─── Attributes

    private int id;

    private String title;           // NotBlank, length 3–255
    private String description;     // Optional, max 1000 chars

    private LocalDate startDate;    // NotNull
    private LocalDate endDate;      // NotNull, must be after startDate
    private LocalDateTime deadline; // Optional

    private String status;          // draft | active | paused | completed | failed | archived
    private String priority;        // low | medium | high (nullable)

    private boolean isFavorite = false;
    private int progress = 0;
    private Integer requiredTasks;
    private String trelloBoardId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** ManyToOne user — obligatoire côté Symfony (nullable: false). */
    private User user;

    /** OneToOne mappedBy goal — inverse du côté {@link Chatroom}. */
    private Chatroom chatroom;

    /** OneToMany goalParticipations. */
    private List<GoalParticipation> goalParticipations = new ArrayList<>();

    /** OneToMany routines (orphanRemoval côté Symfony). */
    private List<Routine> routines = new ArrayList<>();

    // Valid values
    private static final List<String> VALID_STATUSES   = List.of("draft", "active", "paused", "completed", "failed", "archived");
    private static final List<String> VALID_PRIORITIES = List.of("low", "medium", "high");

    // ─── Constructor ──────────────────────────────────────────

    public Goal() {
        this.status    = "draft";
        this.createdAt = LocalDateTime.now();
    }

    // ─── Validation ───────────────────────────────────────────

    /**
     * Validates all fields and throws IllegalArgumentException if any rule is violated.
     */
    public void validate() {
        validateTitle(this.title);
        validateDescription(this.description);
        validateStartDate(this.startDate);
        validateEndDate(this.endDate);
        validateDateRange(this.startDate, this.endDate);
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

    // --- Start Date ---
    public static void validateStartDate(LocalDate startDate) {
        if (startDate == null)
            throw new IllegalArgumentException("La date de début est obligatoire.");
    }

    // --- End Date ---
    public static void validateEndDate(LocalDate endDate) {
        if (endDate == null)
            throw new IllegalArgumentException("La date de fin est obligatoire.");
    }

    // --- Date Range (endDate must be after startDate) ---
    public static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && !endDate.isAfter(startDate))
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
    }

    // --- Status ---
    public static void validateStatus(String status) {
        if (status == null || !VALID_STATUSES.contains(status))
            throw new IllegalArgumentException(
                "Le statut doit être : draft, active, paused, completed, failed ou archived."
            );
    }

    // --- Priority ---
    public static void validatePriority(String priority) {
        if (!VALID_PRIORITIES.contains(priority))
            throw new IllegalArgumentException("La priorité doit être : low, medium ou high.");
    }

    // --- Progress (0–100) ---
    public static void validateProgress(int progress) {
        if (progress < 0 || progress > 100)
            throw new IllegalArgumentException("La progression doit être comprise entre 0 et 100.");
    }

    // ─── Setters with inline validation ───────────────────────

    public void setTitle(String title) {
        validateTitle(title);
        this.title = title;
    }

    /** Setter sans validation — pour les cas où title est stocké en mémoire uniquement (pas en BD). */
    public void setTitleDirect(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        validateDescription(description);
        this.description = description;
    }

    public void setStartDate(LocalDate startDate) {
        validateStartDate(startDate);
        if (this.endDate != null) validateDateRange(startDate, this.endDate);
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        validateEndDate(endDate);
        if (this.startDate != null) validateDateRange(this.startDate, endDate);
        this.endDate = endDate;
    }

    public void setStatus(String status) {
        validateStatus(status);
        this.status = status;
    }

    public void setPriority(String priority) {
        if (priority != null) validatePriority(priority);
        this.priority = priority;
    }

    public void setProgress(int progress) {
        validateProgress(progress);
        this.progress = progress;
    }

    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public void setFavorite(boolean favorite)   { this.isFavorite = favorite; }
    public void setRequiredTasks(Integer n)     { this.requiredTasks = n; }
    public void setTrelloBoardId(String id)     { this.trelloBoardId = id; }
    public void setCreatedAt(LocalDateTime t)   { this.createdAt = t; }
    public void setUpdatedAt(LocalDateTime t)   { this.updatedAt = t; }

    public void setUser(User user) {
        if (this.user == user) {
            return;
        }
        if (this.user != null) {
            this.user.getGoals().remove(this);
        }
        this.user = user;
        if (user != null && !user.getGoals().contains(this)) {
            user.getGoals().add(this);
        }
    }

    public void setChatroom(Chatroom chatroom) {
        this.chatroom = chatroom;
        if (chatroom != null && chatroom.getGoal() != this) {
            chatroom.setGoal(this);
        }
    }

    // ─── Getters ──────────────────────────────────────────────

    public void setId(int id) {
        this.id = id;
    }

    public int            getId()            { return id; }
    public String         getTitle()         { return title; }
    public String         getDescription()   { return description; }
    public LocalDate      getStartDate()     { return startDate; }
    public LocalDate      getEndDate()       { return endDate; }
    public LocalDateTime  getDeadline()      { return deadline; }
    public String         getStatus()        { return status; }
    public String         getPriority()      { return priority; }
    public boolean        isFavorite()       { return isFavorite; }
    public int            getProgress()      { return progress; }
    public Integer        getRequiredTasks() { return requiredTasks; }
    public String         getTrelloBoardId() { return trelloBoardId; }
    public LocalDateTime  getCreatedAt()     { return createdAt; }
    public LocalDateTime  getUpdatedAt()     { return updatedAt; }
    public User           getUser()          { return user; }
    public Chatroom       getChatroom()      { return chatroom; }
    public List<GoalParticipation> getGoalParticipations() { return goalParticipations; }
    public List<Routine>  getRoutines()      { return routines; }

    public void addRoutine(Routine routine) {
        if (routine == null || routines.contains(routine)) {
            return;
        }
        routines.add(routine);
        routine.setGoal(this);
    }

    public void removeRoutine(Routine routine) {
        if (routine == null) {
            return;
        }
        if (routines.remove(routine) && routine.getGoal() == this) {
            routine.setGoal(null);
        }
    }

    public void addGoalParticipation(GoalParticipation participation) {
        if (participation == null || goalParticipations.contains(participation)) {
            return;
        }
        goalParticipations.add(participation);
        participation.setGoal(this);
    }

    public void removeGoalParticipation(GoalParticipation participation) {
        if (participation == null) {
            return;
        }
        if (goalParticipations.remove(participation) && participation.getGoal() == this) {
            participation.setGoal(null);
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────

    /** Call this before persisting an update (mirrors @PreUpdate). */
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Business helpers ─────────────────────────────────────

    public boolean canBeModified() {
        return !List.of("completed", "failed", "archived").contains(this.status);
    }

    public boolean canExecuteActivities() {
        return "active".equals(this.status);
    }

    public void activate() {
        if ("draft".equals(this.status))   this.status = "active";
    }

    public void pause() {
        if ("active".equals(this.status))  this.status = "paused";
    }

    public void resume() {
        if ("paused".equals(this.status))  this.status = "active";
    }

    public void archive() {
        this.status = "archived";
    }

    // ─── Progress calculation ─────────────────────────────────

    public double getProgressPercentage() {
        long total = 0, completed = 0;
        for (Routine r : routines) {
            for (Activity a : r.getActivities()) {
                total++;
                if ("completed".equals(a.getStatus())) completed++;
            }
        }
        return total == 0 ? 0 : Math.round((completed * 100.0 / total) * 100.0) / 100.0;
    }

    // ─── Deadline helpers ─────────────────────────────────────

    public boolean isDeadlineNear() {
        if (deadline == null) return false;
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), deadline);
        return days >= 0 && days <= 7;
    }

    public boolean isOverdue() {
        if (deadline == null) return false;
        return LocalDateTime.now().isAfter(deadline) && !"completed".equals(status);
    }

    public boolean isAtRisk() {
        return isDeadlineNear() && getProgressPercentage() < 40;
    }

    public boolean isUserParticipating(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        for (GoalParticipation p : goalParticipations) {
            if (p.getUser() != null && user.getId().equals(p.getUser().getId())) {
                return true;
            }
            if (p.getUser() == null && p.getUserId() == user.getId().intValue()) {
                return true;
            }
        }
        return false;
    }

    public GoalParticipation getUserParticipation(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        for (GoalParticipation p : goalParticipations) {
            if (p.getUser() != null && user.getId().equals(p.getUser().getId())) {
                return p;
            }
            if (p.getUser() == null && p.getUserId() == user.getId().intValue()) {
                return p;
            }
        }
        return null;
    }

    public boolean canUserModifyGoal(User user) {
        GoalParticipation p = getUserParticipation(user);
        return p != null && (p.isOwner() || p.isAdmin());
    }

    public boolean canUserDeleteGoal(User user) {
        GoalParticipation p = getUserParticipation(user);
        return p != null && p.isOwner();
    }

    public boolean canUserRemoveMembers(User user) {
        GoalParticipation p = getUserParticipation(user);
        return p != null && (p.isOwner() || p.isAdmin());
    }

    public List<GoalParticipation> getPendingRequests() {
        return goalParticipations.stream()
                .filter(GoalParticipation::isPending)
                .collect(Collectors.toList());
    }

    public int getPendingRequestsCount() {
        return (int) goalParticipations.stream().filter(GoalParticipation::isPending).count();
    }

    public boolean hasUserRequestedAccess(User user) {
        GoalParticipation p = getUserParticipation(user);
        return p != null && p.isPending();
    }

    public void updateAutoStatus() {
        if ("archived".equals(status)) {
            return;
        }
        boolean hasRoutines = false;
        boolean allRoutinesCompleted = true;
        for (Routine r : routines) {
            hasRoutines = true;
            if (!"completed".equals(r.getStatus())) {
                allRoutinesCompleted = false;
                break;
            }
        }
        if (hasRoutines && allRoutinesCompleted && !"completed".equals(status)) {
            status = "completed";
            return;
        }
        if (deadline != null && !"completed".equals(status)) {
            if (LocalDateTime.now().isAfter(deadline) && getProgressPercentage() < 50) {
                status = "failed";
            }
        } else if (endDate != null && !"completed".equals(status)) {
            if (LocalDate.now().isAfter(endDate) && getProgressPercentage() < 50) {
                status = "failed";
            }
        }
    }
}
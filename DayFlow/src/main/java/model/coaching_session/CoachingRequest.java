package model.coaching_session;

import java.util.Date;

public class CoachingRequest {

    // 🔵 CONSTANTES
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_DECLINED = "declined";

    public static final String PRIORITY_NORMAL = "normal";
    public static final String PRIORITY_MEDIUM = "medium";
    public static final String PRIORITY_URGENT = "urgent";

    // 🔵 ATTRIBUTS
    private int id;
    private int userId;
    private int coachId;
    private String message;
    private String status;
    private Date createdAt;
    private Date respondedAt;
    private String goal;
    private String level;
    private String frequency;
    private Double budget;
    private String coachingType;
    private String priority;

    // 🔵 CONSTRUCTEUR
    public CoachingRequest() {
        this.status = STATUS_PENDING;
        this.priority = PRIORITY_NORMAL;
        this.createdAt = new Date();
    }

    // 🔴 SETTERS AVEC VALIDATION

    public void setId(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("ID invalide");
        }
        this.id = id;
    }

    public void setUserId(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID invalide");
        }
        this.userId = userId;
    }

    public void setCoachId(int coachId) {
        if (coachId <= 0) {
            throw new IllegalArgumentException("Coach ID invalide");
        }
        this.coachId = coachId;
    }

    public void setMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message obligatoire");
        }
        if (message.length() < 10) {
            throw new IllegalArgumentException("Minimum 10 caractères");
        }
        if (message.length() > 1000) {
            throw new IllegalArgumentException("Maximum 1000 caractères");
        }
        this.message = message;
    }

    public void setStatus(String status) {
        if (!status.equals(STATUS_PENDING) &&
            !status.equals(STATUS_ACCEPTED) &&
            !status.equals(STATUS_DECLINED)) {

            throw new IllegalArgumentException("Statut invalide");
        }

        this.status = status;

        // logique Symfony
        if (status.equals(STATUS_ACCEPTED) || status.equals(STATUS_DECLINED)) {
            this.respondedAt = new Date();
        }
    }

    public void setGoal(String goal) {
        if (goal != null && goal.length() > 100) {
            throw new IllegalArgumentException("Goal trop long");
        }
        this.goal = goal;
    }

    public void setLevel(String level) {
        if (level != null && level.length() > 50) {
            throw new IllegalArgumentException("Level trop long");
        }
        this.level = level;
    }

    public void setFrequency(String frequency) {
        if (frequency != null && frequency.length() > 50) {
            throw new IllegalArgumentException("Frequency trop longue");
        }
        this.frequency = frequency;
    }

    public void setBudget(Double budget) {
        if (budget != null && budget < 0) {
            throw new IllegalArgumentException("Budget doit être positif");
        }
        this.budget = budget;
    }

    public void setCoachingType(String coachingType) {
        if (coachingType != null && coachingType.length() > 50) {
            throw new IllegalArgumentException("Type invalide");
        }
        this.coachingType = coachingType;
    }

    public void setPriority(String priority) {
        if (!priority.equals(PRIORITY_NORMAL) &&
            !priority.equals(PRIORITY_MEDIUM) &&
            !priority.equals(PRIORITY_URGENT)) {

            throw new IllegalArgumentException("Priorité invalide");
        }
        this.priority = priority;
    }

    public void setCreatedAt(Date createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Date obligatoire");
        }
        this.createdAt = createdAt;
    }

    public void setRespondedAt(Date respondedAt) {
        this.respondedAt = respondedAt;
    }

    // 🔵 GETTERS

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public int getCoachId() {
        return coachId;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getRespondedAt() {
        return respondedAt;
    }

    public String getGoal() {
        return goal;
    }

    public String getLevel() {
        return level;
    }

    public String getFrequency() {
        return frequency;
    }

    public Double getBudget() {
        return budget;
    }

    public String getCoachingType() {
        return coachingType;
    }

    public String getPriority() {
        return priority;
    }
}
package model.coaching_session;

import model.user.User;

import java.util.Date;
import java.util.Locale;

public class CoachingRequest {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_PAID = "paid";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_DECLINED = "declined";

    public static final String PRIORITY_NORMAL = "normal";
    public static final String PRIORITY_MEDIUM = "medium";
    public static final String PRIORITY_URGENT = "urgent";

    private int id;
    private int userId;
    private int coachId;
    private String message;
    private String status;
    private Date createdAt;
    private Date respondedAt;
    /** Colonne texte objectif / thème (pas l'entité {@link model.goals_activity_management.Goal}). */
    private String goal;
    private String level;
    private String frequency;
    private Double budget;
    private String coachingType;
    private String priority;
    private String detectedNeed;
    private Integer compatibilityScore;
    private String justification;
    private Integer assignedCoachId;

    private Integer timeSlotId;
    private User user;
    private User coach;
    private User assignedCoach;
    private TimeSlot timeSlot;
    /** OneToOne inverse : le côté propriétaire est {@link Session#coachingRequest}. */
    private Session session;

    public CoachingRequest() {
        this.status = STATUS_PENDING;
        this.priority = PRIORITY_NORMAL;
        this.createdAt = new Date();
    }

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
        if (user != null && user.getId() != null && !user.getId().equals(userId)) {
            this.user = null;
        }
    }

    public void setCoachId(int coachId) {
        if (coachId <= 0) {
            throw new IllegalArgumentException("Coach ID invalide");
        }
        this.coachId = coachId;
        if (coach != null && coach.getId() != null && !coach.getId().equals(coachId)) {
            this.coach = null;
        }
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null && user.getId() != null) {
            this.userId = user.getId();
        }
    }

    public void setCoach(User coach) {
        this.coach = coach;
        if (coach != null && coach.getId() != null) {
            this.coachId = coach.getId();
        }
    }

    public User getUser() {
        return user;
    }

    public User getCoach() {
        return coach;
    }

    public Integer getAssignedCoachId() {
        return assignedCoachId;
    }

    public void setAssignedCoachId(Integer assignedCoachId) {
        if (assignedCoachId != null && assignedCoachId <= 0) {
            throw new IllegalArgumentException("Assigned coach ID invalide");
        }
        this.assignedCoachId = assignedCoachId;
        if (assignedCoach != null && assignedCoach.getId() != null && !assignedCoach.getId().equals(assignedCoachId)) {
            this.assignedCoach = null;
        }
        if (assignedCoachId != null) {
            this.coachId = assignedCoachId;
        }
    }

    public User getAssignedCoach() {
        return assignedCoach;
    }

    public void setAssignedCoach(User assignedCoach) {
        this.assignedCoach = assignedCoach;
        if (assignedCoach != null && assignedCoach.getId() != null) {
            this.assignedCoachId = assignedCoach.getId();
            this.coachId = assignedCoach.getId();
        }
    }

    public Integer getTimeSlotId() {
        return timeSlotId;
    }

    public void setTimeSlotId(Integer timeSlotId) {
        this.timeSlotId = timeSlotId;
        if (timeSlot != null && timeSlot.getId() != timeSlotId) {
            this.timeSlot = null;
        }
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
        if (timeSlot != null) {
            this.timeSlotId = timeSlot.getId();
        } else {
            this.timeSlotId = null;
        }
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        if (this.session == session) {
            return;
        }
        Session previous = this.session;
        this.session = session;
        if (previous != null && previous.getCoachingRequest() == this) {
            previous.setCoachingRequest(null);
        }
        if (session != null && session.getCoachingRequest() != this) {
            session.setCoachingRequest(this);
        }
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
        if (!isAllowedStatus(status)) {
            throw new IllegalArgumentException("Statut invalide");
        }
        this.status = status;
        /* Aligné Symfony : horodatage seulement si pas déjà renseigné (ex. rechargement JDBC). */
        if ((STATUS_ACCEPTED.equals(status) || STATUS_DECLINED.equals(status)) && this.respondedAt == null) {
            this.respondedAt = new Date();
        }
    }

    private static boolean isAllowedStatus(String status) {
        return STATUS_PENDING.equals(status)
                || STATUS_ACCEPTED.equals(status)
                || STATUS_PAID.equals(status)
                || STATUS_CONFIRMED.equals(status)
                || STATUS_COMPLETED.equals(status)
                || STATUS_CANCELLED.equals(status)
                || STATUS_DECLINED.equals(status);
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
            throw new IllegalArgumentException("Budget doit être positif ou nul");
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
        if (!priority.equals(PRIORITY_NORMAL)
                && !priority.equals(PRIORITY_MEDIUM)
                && !priority.equals(PRIORITY_URGENT)) {
            throw new IllegalArgumentException("Priorité invalide");
        }
        this.priority = priority;
    }

    public String getDetectedNeed() {
        return detectedNeed;
    }

    public void setDetectedNeed(String detectedNeed) {
        if (detectedNeed != null && detectedNeed.length() > 255) {
            throw new IllegalArgumentException("Besoin détecté trop long");
        }
        this.detectedNeed = detectedNeed;
    }

    public Integer getCompatibilityScore() {
        return compatibilityScore;
    }

    public void setCompatibilityScore(Integer compatibilityScore) {
        if (compatibilityScore != null && (compatibilityScore < 0 || compatibilityScore > 100)) {
            throw new IllegalArgumentException("Le score de compatibilité doit être entre 0 et 100");
        }
        this.compatibilityScore = compatibilityScore;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        if (justification != null && justification.length() > 1000) {
            throw new IllegalArgumentException("Justification trop longue");
        }
        this.justification = justification;
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

    public boolean isUrgent() {
        return PRIORITY_URGENT.equals(priority);
    }

    public boolean isMedium() {
        return PRIORITY_MEDIUM.equals(priority);
    }

    public boolean isNormal() {
        return PRIORITY_NORMAL.equals(priority);
    }

    /**
     * Détecte la priorité à partir du texte du message (équivalent Symfony).
     */
    public CoachingRequest detectAndSetPriority() {
        String messageLower = message == null ? "" : message.toLowerCase(Locale.ROOT);
        String[] urgentKeywords = {
                "urgent", "urgence", "crise", "choc", "immédiat", "critique", "grave",
                "aide", "sos", "rapidement", "vite"
        };
        String[] mediumKeywords = {
                "important", "bientôt", "besoin", "problème", "difficulté", "stress",
                "anxiété", "préoccupé"
        };
        for (String keyword : urgentKeywords) {
            if (messageLower.contains(keyword)) {
                this.priority = PRIORITY_URGENT;
                return this;
            }
        }
        for (String keyword : mediumKeywords) {
            if (messageLower.contains(keyword)) {
                this.priority = PRIORITY_MEDIUM;
                return this;
            }
        }
        this.priority = PRIORITY_NORMAL;
        return this;
    }

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

    public boolean hasAiRecommendation() {
        return detectedNeed != null && !detectedNeed.isBlank()
                && compatibilityScore != null
                && assignedCoachId != null;
    }
}

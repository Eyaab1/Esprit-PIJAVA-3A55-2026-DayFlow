package model.notification;

import java.time.LocalDateTime;

/**
 * ReminderLog entity for tracking sent reminders to prevent duplicates.
 * Ensures that each user receives only one reminder per deadline event.
 */
public class ReminderLog {

    public enum ReminderType {
        REMINDER_24H("24 hours before"),
        REMINDER_1H("1 hour before"),
        DEADLINE_REACHED("Deadline reached"),
        DEADLINE_MISSED("Deadline missed");

        private final String description;

        ReminderType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private int id;
    private int userId;
    private String entityType; // goal, routine, activity
    private int entityId;
    private ReminderType reminderType;
    private LocalDateTime deadline;
    private LocalDateTime sentAt;

    // Constructors
    public ReminderLog() {
        this.sentAt = LocalDateTime.now();
    }

    public ReminderLog(int userId, String entityType, int entityId, 
                      ReminderType reminderType, LocalDateTime deadline) {
        this();
        this.userId = userId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.reminderType = reminderType;
        this.deadline = deadline;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public ReminderType getReminderType() {
        return reminderType;
    }

    public void setReminderType(ReminderType reminderType) {
        this.reminderType = reminderType;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    @Override
    public String toString() {
        return "ReminderLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                ", reminderType=" + reminderType +
                ", deadline=" + deadline +
                ", sentAt=" + sentAt +
                '}';
    }
}

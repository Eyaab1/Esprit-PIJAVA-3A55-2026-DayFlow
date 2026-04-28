package model.notification;

import java.time.LocalDateTime;

public class Notification {

    public enum NotificationType {
        DEADLINE_24H("24 hours before deadline"),
        DEADLINE_1H("1 hour before deadline"),
        DEADLINE_REACHED("Deadline reached"),
        DEADLINE_MISSED("Deadline missed - overdue"),
        STATUS_CHANGED("Status changed");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum EntityType {
        GOAL("Goal"),
        ROUTINE("Routine"),
        ACTIVITY("Activity");

        private final String displayName;

        EntityType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private Integer id;
    private Integer userId;
    private String type;
    private String entityType;
    private Integer entityId;
    private String title;
    private String message;
    private boolean isRead;
    private String actionUrl;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    public Notification(int userId, NotificationType type, EntityType entityType,
                        int entityId, String title, String message) {
        this();
        this.userId = userId;
        this.type = type.name();
        this.entityType = entityType.name().toLowerCase();
        this.entityId = entityId;
        this.title = title;
        this.message = message;
    }

    public Notification(int userId, NotificationType type, EntityType entityType,
                        int entityId, String title, String message, String actionUrl) {
        this(userId, type, entityType, entityId, title, message);
        this.actionUrl = actionUrl;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public NotificationType getTypeEnum() {
        return type == null ? null : NotificationType.valueOf(type.toUpperCase());
    }

    public void setType(NotificationType type) {
        this.type = type == null ? null : type.name();
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public EntityType getEntityTypeEnum() {
        return entityType == null ? null : EntityType.valueOf(entityType.toUpperCase());
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType == null ? null : entityType.name().toLowerCase();
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
        if (read && this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", userId=" + userId +
                ", type=" + type +
                ", entityType=" + entityType +
                ", entityId=" + entityId +
                ", title='" + title + '\'' +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}

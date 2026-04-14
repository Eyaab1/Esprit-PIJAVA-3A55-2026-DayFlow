package model.chatroom;

import java.time.LocalDateTime;

public class Message {

    private int id;
    private String content;
    private LocalDateTime createdAt;
    private boolean isPinned;
    private boolean isEdited;

    private int chatroomId;
    private int authorId;

    public Message() {
        this.content = "";
        this.createdAt = LocalDateTime.now();
        this.isPinned = false;
        this.isEdited = false;
    }

    public Message(String content, int chatroomId, int authorId) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content required");
        }
        setContent(content);
        setChatroomId(chatroomId);
        setAuthorId(authorId);
        this.createdAt = LocalDateTime.now();
        this.isPinned = false;
        this.isEdited = false;
    }

    /**
     * Accepte un contenu vide pour l’hydratation depuis la BD (NULL ou chaîne vide).
     * Les nouveaux messages sont validés côté service avant insertion.
     */
    public void setContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            this.content = "";
            return;
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("Message too long");
        }
        this.content = content.trim();
    }

    public void setChatroomId(int chatroomId) {
        if (chatroomId <= 0) {
            throw new IllegalArgumentException("Invalid chatroom ID");
        }
        this.chatroomId = chatroomId;
    }

    public void setAuthorId(int authorId) {
        if (authorId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        this.authorId = authorId;
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getChatroomId() {
        return chatroomId;
    }

    public int getAuthorId() {
        return authorId;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

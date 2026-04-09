package model.interaction;

import java.time.LocalDateTime;

public class SavedPost {

    public static final String TABLE = "saved_posts";

    public Integer id;
    public Integer userId;
    public Integer postId;
    public LocalDateTime savedAt;

    public SavedPost(Integer id, Integer userId, Integer postId, LocalDateTime savedAt) {
        this.id = id;
        this.userId = userId;
        this.postId = postId;
        this.savedAt = savedAt;
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

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }
}
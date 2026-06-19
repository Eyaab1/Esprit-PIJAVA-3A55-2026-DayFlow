package model.interaction;

import java.time.LocalDateTime;

public class SavedPost {

    public static final String TABLE = "saved_posts";

    // Fields in exact DB column order: id, saved_at, user_id, post_id
    public Integer id;
    public LocalDateTime savedAt;
    public Integer userId;
    public Integer postId;

    public SavedPost() {
    }

    // Constructor matching DB column order
    public SavedPost(Integer id, LocalDateTime savedAt, Integer userId, Integer postId) {
        this.id = id;
        this.savedAt = savedAt;
        this.userId = userId;
        this.postId = postId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
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
}
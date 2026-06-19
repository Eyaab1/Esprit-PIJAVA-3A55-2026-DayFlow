package model.interaction;

import java.time.LocalDateTime;

public class CommentLike {

    public static final String TABLE = "comment_like";

    // Fields in exact DB column order: id, created_at, comment_id, user_id
    public Integer id;
    public LocalDateTime createdAt;
    public Integer commentId;
    public Integer userId;

    public CommentLike() {
    }

    // Constructor matching DB column order
    public CommentLike(Integer id, LocalDateTime createdAt, Integer commentId, Integer userId) {
        this.id = id;
        this.createdAt = createdAt;
        this.commentId = commentId;
        this.userId = userId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
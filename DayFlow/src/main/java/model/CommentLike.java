package model;

import java.time.LocalDateTime;

public class CommentLike {

    public static final String TABLE = "comment_like";

    public Integer id;
    public Integer commentId;
    public Integer userId;
    public LocalDateTime createdAt;

    public CommentLike(Integer id, Integer commentId, Integer userId, LocalDateTime createdAt) {
        this.id = id;
        this.commentId = commentId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
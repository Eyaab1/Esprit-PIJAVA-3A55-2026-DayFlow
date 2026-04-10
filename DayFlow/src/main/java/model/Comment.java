package model;

import java.time.LocalDateTime;

public class Comment {

    public static final String TABLE = "comment";

    public Integer id;
    public String content;
    public LocalDateTime createdAt;

    public Integer postId;
    public Integer commenterId;

    public Integer parentCommentId;

    public Comment(Integer id, String content, LocalDateTime createdAt, Integer postId, Integer commenterId, Integer parentCommentId) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.postId = postId;
        this.commenterId = commenterId;
        this.parentCommentId = parentCommentId;
    }

    public Comment(String content, LocalDateTime createdAt, Integer postId, Integer commenterId, Integer parentCommentId) {
        this.content = content;
        this.createdAt = createdAt;
        this.postId = postId;
        this.commenterId = commenterId;
        this.parentCommentId = parentCommentId;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public Integer getCommenterId() {
        return commenterId;
    }

    public void setCommenterId(Integer commenterId) {
        this.commenterId = commenterId;
    }

    public Integer getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Integer parentCommentId) {
        this.parentCommentId = parentCommentId;
    }
}
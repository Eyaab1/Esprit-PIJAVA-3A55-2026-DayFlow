package model.interaction;

import model.user.User;

import java.time.LocalDateTime;

public class Comment {

    public static final String TABLE = "comment";

    public Integer id;
    public String content;
    public LocalDateTime createdAt;

    public Integer postId;
    public Integer commenterId;
    private User commenter;

    public Integer parentCommentId;

    public Comment() {
    }

    public Comment(Integer id, String content, LocalDateTime createdAt, Integer postId, Integer commenterId, Integer parentCommentId) {
        this.id = id;
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
        if (commenter != null && commenter.getId() != null && !commenter.getId().equals(commenterId)) {
            this.commenter = null;
        }
    }

    public User getCommenter() {
        return commenter;
    }

    public void setCommenter(User user) {
        if (this.commenter == user) {
            return;
        }
        if (this.commenter != null) {
            this.commenter.getComments().remove(this);
        }
        this.commenter = user;
        if (user != null) {
            if (user.getId() != null) {
                this.commenterId = user.getId();
            }
            if (!user.getComments().contains(this)) {
                user.getComments().add(this);
            }
        } else {
            this.commenterId = null;
        }
    }

    public Integer getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Integer parentCommentId) {
        this.parentCommentId = parentCommentId;
    }
}
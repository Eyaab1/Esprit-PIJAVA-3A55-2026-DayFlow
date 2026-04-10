package model.interaction;

import model.user.User;

import java.time.LocalDateTime;

public class PostLike {

    public static final String TABLE = "post_like";

    public Integer id;
    public Integer postId;
    public Integer userId;
    public LocalDateTime createdAt;
    private User liker;

    public PostLike() {
    }

    public PostLike(Integer id, Integer postId, Integer userId, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
        if (liker != null && liker.getId() != null && !liker.getId().equals(userId)) {
            this.liker = null;
        }
    }

    public User getLiker() {
        return liker;
    }

    public void setLiker(User user) {
        if (this.liker == user) {
            return;
        }
        if (this.liker != null) {
            this.liker.getPostLikes().remove(this);
        }
        this.liker = user;
        if (user != null) {
            if (user.getId() != null) {
                this.userId = user.getId();
            }
            if (!user.getPostLikes().contains(this)) {
                user.getPostLikes().add(this);
            }
        } else {
            this.userId = null;
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

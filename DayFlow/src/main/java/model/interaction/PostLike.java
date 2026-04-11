package model.interaction;

import model.user.User;

import java.time.LocalDateTime;

public class PostLike {

    public static final String TABLE = "post_like";

    // Fields in exact DB column order: id, post_id, liker_id
    public Integer id;
    public Integer postId;
    public Integer likerId;

    // Relations (for ORM compatibility, not used in JDBC)
    private User liker;

    public PostLike() {
    }

    // Constructor matching DB column order
    public PostLike(Integer id, Integer postId, Integer likerId) {
        this.id = id;
        this.postId = postId;
        this.likerId = likerId;
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

    public Integer getLikerId() {
        return likerId;
    }

    public void setLikerId(Integer likerId) {
        this.likerId = likerId;
        if (liker != null && liker.getId() != null && !liker.getId().equals(likerId)) {
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
                this.likerId = user.getId();
            }
            if (!user.getPostLikes().contains(this)) {
                user.getPostLikes().add(this);
            }
        } else {
            this.likerId = null;
        }
    }
}
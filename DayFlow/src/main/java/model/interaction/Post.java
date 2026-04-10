package model.interaction;

import enums.PostStatus;
import model.user.User;

import java.time.LocalDateTime;
import java.util.List;

public class Post {

    public static final String TABLE = "post";

    public Integer id;
    public String title;
    public String content;

    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime deletedAt;
    public LocalDateTime scheduledAt;

    public PostStatus status;

    public List<String> images;

    // Relations → ID pour JDBC ; objet pour graphe Symfony (mappedBy createdBy)
    public Integer createdById;
    private User createdBy;

    public Integer viewCount;
    public Integer clickCount;

    public Post() {
    }

    public Post(Integer id, String title, String content, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt, List<String> images, Integer createdById, Integer clickCount, Integer viewCount, PostStatus status, LocalDateTime scheduledAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.images = images;
        this.createdById = createdById;
        this.clickCount = clickCount;
        this.viewCount = viewCount;
        this.status = status;
        this.scheduledAt = scheduledAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public Integer getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Integer createdById) {
        this.createdById = createdById;
        if (createdBy != null && createdBy.getId() != null && !createdBy.getId().equals(createdById)) {
            this.createdBy = null;
        }
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User user) {
        if (this.createdBy == user) {
            return;
        }
        if (this.createdBy != null) {
            this.createdBy.getPosts().remove(this);
        }
        this.createdBy = user;
        if (user != null) {
            if (user.getId() != null) {
                this.createdById = user.getId();
            }
            if (!user.getPosts().contains(this)) {
                user.getPosts().add(this);
            }
        } else {
            this.createdById = null;
        }
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getClickCount() {
        return clickCount;
    }

    public void setClickCount(Integer clickCount) {
        this.clickCount = clickCount;
    }

}
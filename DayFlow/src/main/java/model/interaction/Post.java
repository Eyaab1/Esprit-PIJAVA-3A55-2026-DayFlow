package model.interaction;

import enums.PostStatus;
import model.user.User;

import java.time.LocalDateTime;
import java.util.List;

public class Post {

    public static final String TABLE = "post";

    // Fields in exact DB column order
    public Integer id;
    public String title;
    public String content;
    public LocalDateTime createdAt;
    public Integer createdById;
    public PostStatus status;
    public List<String> images;
    public LocalDateTime scheduledAt;
    public LocalDateTime updatedAt;
    public String slug;
    public LocalDateTime deletedAt;
    public Integer viewCount;
    public Integer clickCount;

    // Relations (for ORM compatibility, not used in JDBC)
    private User createdBy;

    public Post() {
    }

    // Constructor matching DB column order
    public Post(Integer id, String title, String content, LocalDateTime createdAt,
                Integer createdById, PostStatus status, List<String> images,
                LocalDateTime scheduledAt, LocalDateTime updatedAt, String slug,
                LocalDateTime deletedAt, Integer viewCount, Integer clickCount) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.createdById = createdById;
        this.status = status;
        this.images = images;
        this.scheduledAt = scheduledAt;
        this.updatedAt = updatedAt;
        this.slug = slug;
        this.deletedAt = deletedAt;
        this.viewCount = viewCount;
        this.clickCount = clickCount;
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

    public Integer getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Integer createdById) {
        this.createdById = createdById;
        if (createdBy != null && createdBy.getId() != null && !createdBy.getId().equals(createdById)) {
            this.createdBy = null;
        }
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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
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
}
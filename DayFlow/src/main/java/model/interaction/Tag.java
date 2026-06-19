package model.interaction;

import java.time.LocalDateTime;

public class Tag {

    public static final String TABLE = "tags";

    // Fields in exact DB column order: id, name, slug, created_at, usage_count
    public Integer id;
    public String name;
    public String slug;
    public LocalDateTime createdAt;
    public Integer usageCount;

    public Tag() {
    }

    // Constructor matching DB column order
    public Tag(Integer id, String name, String slug, LocalDateTime createdAt, Integer usageCount) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.createdAt = createdAt;
        this.usageCount = usageCount;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }
}
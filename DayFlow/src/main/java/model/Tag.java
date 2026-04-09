package model;

import java.time.LocalDateTime;

public class Tag {

    public static final String TABLE = "tags";

    public Integer id;
    public String name;
    public LocalDateTime createdAt;
    public Integer usageCount;

    public Tag(Integer id, String name, LocalDateTime createdAt, Integer usageCount) {
        this.id = id;
        this.name = name;
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
package model;

import java.time.LocalDateTime;

public class GoalParticipation {

    // 🔹 roles
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_OWNER = "OWNER";

    // 🔹 status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private int id;
    private int userId;
    private int goalId;
    private LocalDateTime createdAt;
    private String role;
    private String status;

    // 🔹 constructeur par défaut
    public GoalParticipation() {
        this.createdAt = LocalDateTime.now();
        this.role = ROLE_MEMBER;
        this.status = STATUS_APPROVED;
    }

    // 🔹 constructeur avec paramètres
    public GoalParticipation(int userId, int goalId) {
        setUserId(userId);
        setGoalId(goalId);
        this.createdAt = LocalDateTime.now();
        this.role = ROLE_MEMBER;
        this.status = STATUS_APPROVED;
    }

    // 🔒 validation
    public void setUserId(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        this.userId = userId;
    }

    public void setGoalId(int goalId) {
        if (goalId <= 0) {
            throw new IllegalArgumentException("Invalid goal ID");
        }
        this.goalId = goalId;
    }

    public void setRole(String role) {
        if (!role.equals(ROLE_MEMBER) &&
            !role.equals(ROLE_ADMIN) &&
            !role.equals(ROLE_OWNER)) {
            throw new IllegalArgumentException("Invalid role");
        }
        this.role = role;
    }

    public void setStatus(String status) {
        if (!status.equals(STATUS_PENDING) &&
            !status.equals(STATUS_APPROVED) &&
            !status.equals(STATUS_REJECTED)) {
            throw new IllegalArgumentException("Invalid status");
        }
        this.status = status;
    }

    // 🔹 getters (OBLIGATOIRE pour service)
    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public int getGoalId() {
        return goalId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    // 🔹 setters utiles
    public void setId(int id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
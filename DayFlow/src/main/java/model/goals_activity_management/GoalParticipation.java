package model.goals_activity_management;

import model.user.User;

import java.time.LocalDateTime;

public class GoalParticipation {

    public static final String ROLE_MEMBER = "member";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_OWNER = "owner";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "accepted";
    public static final String STATUS_REJECTED = "rejected";

    private int id;
    private int userId;
    private int goalId;
    private LocalDateTime createdAt;
    private String role;
    private String status;

    /** ManyToOne user (Symfony) — les IDs restent utilisés par JDBC. */
    private User user;
    /** ManyToOne goal (Symfony) — les IDs restent utilisés par JDBC. */
    private Goal goal;

    public GoalParticipation() {
        this.createdAt = LocalDateTime.now();
        this.role = ROLE_MEMBER;
        this.status = STATUS_APPROVED;
    }

    public GoalParticipation(int userId, int goalId) {
        setUserId(userId);
        setGoalId(goalId);
        this.createdAt = LocalDateTime.now();
        this.role = ROLE_MEMBER;
        this.status = STATUS_APPROVED;
    }

    public void setUserId(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        this.userId = userId;
        if (this.user != null && this.user.getId() != null && this.user.getId() != userId) {
            this.user = null;
        }
    }

    public void setGoalId(int goalId) {
        if (goalId <= 0) {
            throw new IllegalArgumentException("Invalid goal ID");
        }
        this.goalId = goalId;
        if (this.goal != null && this.goal.getId() != goalId) {
            this.goal = null;
        }
    }

    public void setUser(User user) {
        if (this.user == user) {
            return;
        }
        if (this.user != null) {
            this.user.getGoalParticipations().remove(this);
        }
        this.user = user;
        if (user != null) {
            if (user.getId() != null) {
                this.userId = user.getId();
            }
            if (!user.getGoalParticipations().contains(this)) {
                user.getGoalParticipations().add(this);
            }
        }
    }

    public void setGoal(Goal goal) {
        if (this.goal == goal) {
            return;
        }
        if (this.goal != null) {
            this.goal.getGoalParticipations().remove(this);
        }
        this.goal = goal;
        if (goal != null) {
            this.goalId = goal.getId();
            if (!goal.getGoalParticipations().contains(this)) {
                goal.getGoalParticipations().add(this);
            }
        }
    }

    public User getUser() {
        return user;
    }

    public Goal getGoal() {
        return goal;
    }

    public boolean isOwner() {
        return ROLE_OWNER.equals(role);
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role) || isOwner();
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
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

    public void setId(int id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

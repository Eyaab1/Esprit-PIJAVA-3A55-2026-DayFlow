package model.chatroom;

import model.goals_activity_management.Goal;

import java.time.LocalDateTime;

public class Chatroom {

    private int id;
    private LocalDateTime createdAt;
    private int goalId;
    private String state;

    /** OneToOne côté propriétaire (FK goal_id sur chatroom), inverse de {@link Goal#getChatroom()}. */
    private Goal goal;

    public Chatroom() {
        this.createdAt = LocalDateTime.now();
        this.state = "active";
    }

    public Chatroom(int goalId, String state) {
        setGoalId(goalId);
        setState(state);
        this.createdAt = LocalDateTime.now();
    }

    public void setState(String state) {
        if (state == null || state.isEmpty()) {
            throw new IllegalArgumentException("State required");
        }
        if (!state.equals("active") && !state.equals("inactive")) {
            throw new IllegalArgumentException("Invalid state");
        }
        this.state = state;
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

    public Goal getGoal() {
        return goal;
    }

    public void setGoal(Goal goal) {
        this.goal = goal;
        if (goal != null) {
            this.goalId = goal.getId();
            if (goal.getChatroom() != this) {
                goal.setChatroom(this);
            }
        }
    }

    public int getId() {
        return id;
    }

    public int getGoalId() {
        return goalId;
    }

    public String getState() {
        return state;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

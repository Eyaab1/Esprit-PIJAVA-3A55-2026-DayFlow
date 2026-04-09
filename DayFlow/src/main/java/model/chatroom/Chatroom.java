package model.chatroom;

import java.time.LocalDateTime;

public class Chatroom {

    private int id;
    private LocalDateTime createdAt;
    private int goalId;
    private String state;

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

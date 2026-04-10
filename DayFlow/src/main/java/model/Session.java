package model;

import java.util.Date;

public class Session {

    // 🔵 CONSTANTES STATUS
    public static final String STATUS_SCHEDULING = "scheduling";
    public static final String STATUS_PROPOSED_BY_USER = "proposed_by_user";
    public static final String STATUS_PROPOSED_BY_COACH = "proposed_by_coach";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";

    // 🔵 ATTRIBUTS
    private int id;
    private int coachingRequestId;
    private String status;
    private Date proposedTimeByUser;
    private Date proposedTimeByCoach;
    private Date scheduledAt;
    private Integer duration;
    private Date createdAt;
    private Date updatedAt;

    // 🔵 CONSTRUCTEUR
    public Session() {
        this.status = STATUS_SCHEDULING;
        this.createdAt = new Date();
    }

    // SETTERS AVEC CONTROLE DE SAISIE

    public void setId(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("ID invalide");
        }
        this.id = id;
    }

    public void setCoachingRequestId(int coachingRequestId) {
        if (coachingRequestId <= 0) {
            throw new IllegalArgumentException("CoachingRequest ID invalide");
        }
        this.coachingRequestId = coachingRequestId;
    }

    public void setStatus(String status) {
        if (!status.equals(STATUS_SCHEDULING) &&
            !status.equals(STATUS_PROPOSED_BY_USER) &&
            !status.equals(STATUS_PROPOSED_BY_COACH) &&
            !status.equals(STATUS_CONFIRMED) &&
            !status.equals(STATUS_COMPLETED) &&
            !status.equals(STATUS_CANCELLED)) {

            throw new IllegalArgumentException("Statut invalide");
        }
        this.status = status;
    }

    public void setProposedTimeByUser(Date proposedTimeByUser) {
        this.proposedTimeByUser = proposedTimeByUser;
    }

    public void setProposedTimeByCoach(Date proposedTimeByCoach) {
        this.proposedTimeByCoach = proposedTimeByCoach;
    }

    public void setScheduledAt(Date scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public void setDuration(Integer duration) {
        if (duration != null && duration <= 0) {
            throw new IllegalArgumentException("Durée doit être positive");
        }
        this.duration = duration;
    }

    public void setCreatedAt(Date createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Date obligatoire");
        }
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    //  GETTERS

    public int getId() {
        return id;
    }

    public int getCoachingRequestId() {
        return coachingRequestId;
    }

    public String getStatus() {
        return status;
    }

    public Date getProposedTimeByUser() {
        return proposedTimeByUser;
    }

    public Date getProposedTimeByCoach() {
        return proposedTimeByCoach;
    }

    public Date getScheduledAt() {
        return scheduledAt;
    }

    public Integer getDuration() {
        return duration;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

}

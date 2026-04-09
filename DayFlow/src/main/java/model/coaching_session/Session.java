package model.coaching_session;

import java.util.Date;

public class Session {

    public static final String STATUS_SCHEDULING = "scheduling";
    public static final String STATUS_PROPOSED_BY_USER = "proposed_by_user";
    public static final String STATUS_PROPOSED_BY_COACH = "proposed_by_coach";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";

    public static final String PRIORITY_LOW = "low";
    public static final String PRIORITY_MEDIUM = "medium";
    public static final String PRIORITY_HIGH = "high";

    public static final String PAYMENT_STATUS_PENDING = "pending";
    public static final String PAYMENT_STATUS_PAID = "paid";

    private int id;
    private int coachingRequestId;
    private String status;
    private Date proposedTimeByUser;
    private Date proposedTimeByCoach;
    private Date scheduledAt;
    private Integer duration;
    private String priority;
    private String objective;
    private Date createdAt;
    private Date updatedAt;
    private Double price;
    private String paymentStatus;

    /** OneToOne propriétaire (FK coaching_request_id) — inverse {@link CoachingRequest#getSession()}. */
    private CoachingRequest coachingRequest;

    public Session() {
        this.status = STATUS_SCHEDULING;
        this.createdAt = new Date();
        this.paymentStatus = PAYMENT_STATUS_PENDING;
    }

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
        if (coachingRequest != null && coachingRequest.getId() != coachingRequestId) {
            this.coachingRequest = null;
        }
    }

    public CoachingRequest getCoachingRequest() {
        return coachingRequest;
    }

    public void setCoachingRequest(CoachingRequest coachingRequest) {
        if (this.coachingRequest == coachingRequest) {
            return;
        }
        CoachingRequest previous = this.coachingRequest;
        this.coachingRequest = coachingRequest;
        if (previous != null && previous.getSession() == this) {
            previous.setSession(null);
        }
        if (coachingRequest != null) {
            if (coachingRequest.getId() > 0) {
                this.coachingRequestId = coachingRequest.getId();
            }
            if (coachingRequest.getSession() != this) {
                coachingRequest.setSession(this);
            }
        }
    }

    public void setStatus(String status) {
        if (!status.equals(STATUS_SCHEDULING)
                && !status.equals(STATUS_PROPOSED_BY_USER)
                && !status.equals(STATUS_PROPOSED_BY_COACH)
                && !status.equals(STATUS_CONFIRMED)
                && !status.equals(STATUS_COMPLETED)
                && !status.equals(STATUS_CANCELLED)) {
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

    public void setPriority(String priority) {
        if (priority == null) {
            this.priority = null;
            return;
        }
        if (!priority.equals(PRIORITY_LOW)
                && !priority.equals(PRIORITY_MEDIUM)
                && !priority.equals(PRIORITY_HIGH)) {
            throw new IllegalArgumentException("Priorité invalide");
        }
        this.priority = priority;
    }

    public void setObjective(String objective) {
        this.objective = objective;
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

    public void setPrice(Double price) {
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Le prix doit être positif ou nul");
        }
        this.price = price;
    }

    public void setPaymentStatus(String paymentStatus) {
        if (paymentStatus == null) {
            this.paymentStatus = null;
            return;
        }
        if (!paymentStatus.equals(PAYMENT_STATUS_PENDING) && !paymentStatus.equals(PAYMENT_STATUS_PAID)) {
            throw new IllegalArgumentException("Statut de paiement invalide");
        }
        this.paymentStatus = paymentStatus;
    }

    /** Heure affichable : {@code scheduledAt}, sinon proposition coach, sinon utilisateur. */
    public Date getDisplayTime() {
        if (scheduledAt != null) {
            return scheduledAt;
        }
        if (proposedTimeByCoach != null) {
            return proposedTimeByCoach;
        }
        return proposedTimeByUser;
    }

    public boolean isPaid() {
        return PAYMENT_STATUS_PAID.equals(paymentStatus);
    }

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

    public String getPriority() {
        return priority;
    }

    public String getObjective() {
        return objective;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public Double getPrice() {
        return price;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }
}

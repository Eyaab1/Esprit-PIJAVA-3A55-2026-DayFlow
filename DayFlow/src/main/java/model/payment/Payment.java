package model.payment;

import enums.PaymentStatus;
import model.coaching_session.CoachingRequest;
import model.user.User;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Entité représentant un paiement pour une séance de coaching.
 */
public class Payment {

    private Integer id;
    private Integer coachingRequestId;
    private Integer userId;
    private Integer coachId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String stripePaymentIntentId;
    private String stripeCheckoutSessionId;
    private Date createdAt;
    private Date updatedAt;
    private Date paidAt;
    private String failureReason;
    private String receiptUrl;

    // Relations
    private CoachingRequest coachingRequest;
    private User user;
    private User coach;

    public Payment() {
        this.status = PaymentStatus.PENDING;
        this.currency = "EUR";
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters et Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCoachingRequestId() {
        return coachingRequestId;
    }

    public void setCoachingRequestId(Integer coachingRequestId) {
        if (coachingRequestId != null && coachingRequestId <= 0) {
            throw new IllegalArgumentException("ID de demande de coaching invalide");
        }
        this.coachingRequestId = coachingRequestId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        if (userId != null && userId <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        this.userId = userId;
    }

    public Integer getCoachId() {
        return coachId;
    }

    public void setCoachId(Integer coachId) {
        if (coachId != null && coachId <= 0) {
            throw new IllegalArgumentException("ID coach invalide");
        }
        this.coachId = coachId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        if (currency != null && currency.length() != 3) {
            throw new IllegalArgumentException("La devise doit être un code ISO 4217 (3 caractères)");
        }
        this.currency = currency != null ? currency.toUpperCase() : "EUR";
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Le statut ne peut pas être null");
        }
        this.status = status;
        this.updatedAt = new Date();
        
        // Si le paiement est réussi, enregistrer la date
        if (status == PaymentStatus.SUCCEEDED && this.paidAt == null) {
            this.paidAt = new Date();
        }
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getStripeCheckoutSessionId() {
        return stripeCheckoutSessionId;
    }

    public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) {
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Date paidAt) {
        this.paidAt = paidAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }

    public CoachingRequest getCoachingRequest() {
        return coachingRequest;
    }

    public void setCoachingRequest(CoachingRequest coachingRequest) {
        this.coachingRequest = coachingRequest;
        if (coachingRequest != null) {
            this.coachingRequestId = coachingRequest.getId();
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null && user.getId() != null) {
            this.userId = user.getId();
        }
    }

    public User getCoach() {
        return coach;
    }

    public void setCoach(User coach) {
        this.coach = coach;
        if (coach != null && coach.getId() != null) {
            this.coachId = coach.getId();
        }
    }

    // Méthodes utilitaires

    /**
     * Vérifie si le paiement peut être annulé.
     *
     * @return true si le paiement peut être annulé
     */
    public boolean canBeCancelled() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }

    /**
     * Vérifie si le paiement peut être remboursé.
     *
     * @return true si le paiement peut être remboursé
     */
    public boolean canBeRefunded() {
        return status == PaymentStatus.SUCCEEDED;
    }

    /**
     * Retourne le montant formaté avec la devise.
     *
     * @return le montant formaté
     */
    public String getFormattedAmount() {
        if (amount == null) {
            return "0.00 " + currency;
        }
        return String.format("%.2f %s", amount, currency);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", coachingRequestId=" + coachingRequestId +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}

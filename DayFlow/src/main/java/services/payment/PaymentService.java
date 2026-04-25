package services.payment;

import enums.PaymentStatus;
import model.coaching_session.CoachingRequest;
import model.payment.Payment;
import model.user.User;
import services.CRUD;
import services.coaching_session_module.CoachingRequestService;
import utils.DbConnexion;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service de gestion des paiements pour les séances de coaching.
 * Prêt pour l'intégration Stripe.
 */
public class PaymentService implements CRUD<Payment, Integer> {

    private final Connection cnx;
    private final CoachingRequestService coachingRequestService;

    public PaymentService() {
        this.cnx = DbConnexion.getInstance().getCnx();
        this.coachingRequestService = new CoachingRequestService();
    }

    @Override
    public void create(Payment entity) throws SQLException {
        insert(entity);
    }

    @Override
    public void insert(Payment payment) throws SQLException {
        String sql = """
                INSERT INTO payment (
                    coaching_request_id, user_id, coach_id, amount, currency, status,
                    stripe_payment_intent_id, stripe_checkout_session_id,
                    created_at, updated_at, paid_at, failure_reason, receipt_url
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            ps.setInt(idx++, payment.getCoachingRequestId());
            ps.setInt(idx++, payment.getUserId());
            ps.setInt(idx++, payment.getCoachId());
            ps.setBigDecimal(idx++, payment.getAmount());
            ps.setString(idx++, payment.getCurrency());
            ps.setString(idx++, payment.getStatus().getValue());
            ps.setString(idx++, payment.getStripePaymentIntentId());
            ps.setString(idx++, payment.getStripeCheckoutSessionId());
            ps.setTimestamp(idx++, toTimestamp(payment.getCreatedAt()));
            ps.setTimestamp(idx++, toTimestamp(payment.getUpdatedAt()));
            ps.setTimestamp(idx++, toTimestamp(payment.getPaidAt()));
            ps.setString(idx++, payment.getFailureReason());
            ps.setString(idx++, payment.getReceiptUrl());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    payment.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Payment payment) throws SQLException {
        String sql = """
                UPDATE payment SET
                    amount = ?, currency = ?, status = ?,
                    stripe_payment_intent_id = ?, stripe_checkout_session_id = ?,
                    updated_at = ?, paid_at = ?, failure_reason = ?, receipt_url = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            int idx = 1;
            ps.setBigDecimal(idx++, payment.getAmount());
            ps.setString(idx++, payment.getCurrency());
            ps.setString(idx++, payment.getStatus().getValue());
            ps.setString(idx++, payment.getStripePaymentIntentId());
            ps.setString(idx++, payment.getStripeCheckoutSessionId());
            ps.setTimestamp(idx++, new Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(idx++, toTimestamp(payment.getPaidAt()));
            ps.setString(idx++, payment.getFailureReason());
            ps.setString(idx++, payment.getReceiptUrl());
            ps.setInt(idx, payment.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM payment WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Trouve un paiement par son ID.
     */
    public Optional<Payment> findById(int id) throws SQLException {
        String sql = "SELECT * FROM payment WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Trouve un paiement par l'ID de la demande de coaching.
     */
    public Optional<Payment> findByCoachingRequestId(int coachingRequestId) throws SQLException {
        String sql = "SELECT * FROM payment WHERE coaching_request_id = ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coachingRequestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Trouve un paiement par l'ID de session Stripe Checkout.
     */
    public Optional<Payment> findByStripeCheckoutSessionId(String sessionId) throws SQLException {
        String sql = "SELECT * FROM payment WHERE stripe_checkout_session_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Trouve tous les paiements d'un utilisateur.
     */
    public List<Payment> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM payment WHERE user_id = ? ORDER BY created_at DESC";
        List<Payment> payments = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRow(rs));
                }
            }
        }
        return payments;
    }

    /**
     * Trouve tous les paiements d'un coach.
     */
    public List<Payment> findByCoachId(int coachId) throws SQLException {
        String sql = "SELECT * FROM payment WHERE coach_id = ? ORDER BY created_at DESC";
        List<Payment> payments = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coachId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRow(rs));
                }
            }
        }
        return payments;
    }

    /**
     * Crée un paiement pour une demande de coaching acceptée.
     *
     * @param coachingRequest la demande de coaching
     * @param amount          le montant à payer
     * @return le paiement créé
     * @throws SQLException           si erreur base de données
     * @throws IllegalStateException  si la demande n'est pas acceptée
     */
    public Payment createPaymentForRequest(CoachingRequest coachingRequest, BigDecimal amount) throws SQLException {
        if (!CoachingRequest.STATUS_ACCEPTED.equals(coachingRequest.getStatus())) {
            throw new IllegalStateException("La demande doit être acceptée pour créer un paiement");
        }

        // Vérifier qu'un paiement n'existe pas déjà
        Optional<Payment> existing = findByCoachingRequestId(coachingRequest.getId());
        if (existing.isPresent()) {
            throw new IllegalStateException("Un paiement existe déjà pour cette demande");
        }

        Payment payment = new Payment();
        payment.setCoachingRequestId(coachingRequest.getId());
        payment.setUserId(coachingRequest.getUserId());
        payment.setCoachId(coachingRequest.getCoachId());
        payment.setAmount(amount);
        payment.setCurrency("EUR");
        payment.setStatus(PaymentStatus.PENDING);

        insert(payment);
        return payment;
    }

    /**
     * Initie un paiement Stripe (à implémenter avec la clé Stripe).
     * Pour l'instant, retourne juste une URL de simulation.
     *
     * @param payment le paiement à traiter
     * @return l'URL de checkout Stripe (simulée pour l'instant)
     * @throws SQLException si erreur base de données
     */
    public String initiateStripeCheckout(Payment payment) throws SQLException {
        // TODO: Implémenter l'intégration Stripe réelle
        // Pour l'instant, on simule la création d'une session Stripe
        
        String simulatedSessionId = "cs_test_" + System.currentTimeMillis();
        payment.setStripeCheckoutSessionId(simulatedSessionId);
        payment.setStatus(PaymentStatus.PROCESSING);
        update(payment);

        // URL de simulation - à remplacer par la vraie URL Stripe
        return "https://checkout.stripe.com/pay/" + simulatedSessionId;
    }

    /**
     * Marque un paiement comme réussi et met à jour la demande de coaching.
     *
     * @param paymentId           l'ID du paiement
     * @param stripePaymentIntentId l'ID du PaymentIntent Stripe
     * @param receiptUrl          l'URL du reçu
     * @throws SQLException si erreur base de données
     */
    public void markPaymentAsSucceeded(int paymentId, String stripePaymentIntentId, String receiptUrl) throws SQLException {
        Optional<Payment> optPayment = findById(paymentId);
        if (optPayment.isEmpty()) {
            throw new IllegalArgumentException("Paiement introuvable");
        }

        Payment payment = optPayment.get();
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setStripePaymentIntentId(stripePaymentIntentId);
        payment.setReceiptUrl(receiptUrl);
        payment.setPaidAt(new java.util.Date());
        update(payment);

        // Mettre à jour le statut de la demande de coaching
        coachingRequestService.updateStatus(payment.getCoachingRequestId(), CoachingRequest.STATUS_PAID);
    }

    /**
     * Marque un paiement comme échoué.
     *
     * @param paymentId     l'ID du paiement
     * @param failureReason la raison de l'échec
     * @throws SQLException si erreur base de données
     */
    public void markPaymentAsFailed(int paymentId, String failureReason) throws SQLException {
        Optional<Payment> optPayment = findById(paymentId);
        if (optPayment.isEmpty()) {
            throw new IllegalArgumentException("Paiement introuvable");
        }

        Payment payment = optPayment.get();
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        update(payment);
    }

    /**
     * Annule un paiement en attente.
     *
     * @param paymentId l'ID du paiement
     * @throws SQLException          si erreur base de données
     * @throws IllegalStateException si le paiement ne peut pas être annulé
     */
    public void cancelPayment(int paymentId) throws SQLException {
        Optional<Payment> optPayment = findById(paymentId);
        if (optPayment.isEmpty()) {
            throw new IllegalArgumentException("Paiement introuvable");
        }

        Payment payment = optPayment.get();
        if (!payment.canBeCancelled()) {
            throw new IllegalStateException("Ce paiement ne peut pas être annulé");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        update(payment);
    }

    /**
     * Vérifie si une demande de coaching a un paiement réussi.
     *
     * @param coachingRequestId l'ID de la demande
     * @return true si un paiement réussi existe
     * @throws SQLException si erreur base de données
     */
    public boolean hasSuccessfulPayment(int coachingRequestId) throws SQLException {
        Optional<Payment> payment = findByCoachingRequestId(coachingRequestId);
        return payment.isPresent() && payment.get().getStatus().isSuccessful();
    }

    /**
     * Calcule le montant total des paiements réussis pour un coach.
     *
     * @param coachId l'ID du coach
     * @return le montant total
     * @throws SQLException si erreur base de données
     */
    public BigDecimal calculateTotalEarnings(int coachId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(amount), 0) as total
                FROM payment
                WHERE coach_id = ? AND status = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coachId);
            ps.setString(2, PaymentStatus.SUCCEEDED.getValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Compte le nombre de paiements par statut pour un utilisateur.
     *
     * @param userId l'ID de l'utilisateur
     * @param status le statut
     * @return le nombre de paiements
     * @throws SQLException si erreur base de données
     */
    public int countByUserAndStatus(int userId, PaymentStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payment WHERE user_id = ? AND status = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, status.getValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    // Méthodes utilitaires privées

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setId(rs.getInt("id"));
        payment.setCoachingRequestId(rs.getInt("coaching_request_id"));
        payment.setUserId(rs.getInt("user_id"));
        payment.setCoachId(rs.getInt("coach_id"));
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setCurrency(rs.getString("currency"));
        payment.setStatus(PaymentStatus.fromValue(rs.getString("status")));
        payment.setStripePaymentIntentId(rs.getString("stripe_payment_intent_id"));
        payment.setStripeCheckoutSessionId(rs.getString("stripe_checkout_session_id"));
        payment.setCreatedAt(toDate(rs.getTimestamp("created_at")));
        payment.setUpdatedAt(toDate(rs.getTimestamp("updated_at")));
        payment.setPaidAt(toDate(rs.getTimestamp("paid_at")));
        payment.setFailureReason(rs.getString("failure_reason"));
        payment.setReceiptUrl(rs.getString("receipt_url"));
        return payment;
    }

    private static Timestamp toTimestamp(java.util.Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

    private static java.util.Date toDate(Timestamp timestamp) {
        return timestamp == null ? null : new java.util.Date(timestamp.getTime());
    }
}

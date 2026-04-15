package services.coaching_session;

import model.coaching_session.CoachingRequest;
import model.coaching_session.Session;
import model.user.User;
import services.account.UserService;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;

/**
 * Cas d’usage alignés sur {@code CoachingRequestController} et {@code SessionController} Symfony
 * (hors sécurité CSRF, notifications, contexte démo).
 */
public class CoachingWorkflowService {

    private final CoachingRequestService coachingRequests = new CoachingRequestService();
    private final SessionService sessions = new SessionService();
    private final UserService users = new UserService();

    /**
     * Accepte une demande : statut {@code accepted}, création de la ligne {@code session} (transaction).
     * Retourne l’id de session créée ou déjà présente.
     * <p>Idempotent : une seule session par demande (UNIQUE {@code coaching_request_id}). Si une ligne
     * existe déjà (Symfony, double clic), on synchronise le statut et on renvoie l’id sans réinsérer.
     */
    public int acceptCoachingRequest(int requestId, int coachUserId) throws SQLException {
        Optional<CoachingRequest> opt = coachingRequests.findById(requestId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Demande introuvable.");
        }
        CoachingRequest cr = opt.get();
        if (cr.getCoachId() != coachUserId) {
            throw new IllegalStateException("Non autorisé.");
        }

        Session existing = sessions.findByCoachingRequestId(cr.getId());
        if (existing != null) {
            if (CoachingRequest.STATUS_PENDING.equals(cr.getStatus())) {
                cr.setStatus(CoachingRequest.STATUS_ACCEPTED);
                coachingRequests.update(cr);
            }
            return existing.getId();
        }

        if (!CoachingRequest.STATUS_PENDING.equals(cr.getStatus())) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }

        cr.setStatus(CoachingRequest.STATUS_ACCEPTED);

        Connection cnx = DbConnexion.getConnection();
        boolean prev = cnx.getAutoCommit();
        try {
            cnx.setAutoCommit(false);
            coachingRequests.update(cr);
            Session s = new Session();
            s.setCoachingRequestId(cr.getId());
            sessions.insert(s);
            cnx.commit();
            return s.getId();
        } catch (SQLException e) {
            cnx.rollback();
            /* Autre transaction a inséré la session entre-temps (double clic). */
            if ("23505".equals(e.getSQLState())) {
                Session race = sessions.findByCoachingRequestId(cr.getId());
                if (race != null) {
                    Optional<CoachingRequest> again = coachingRequests.findById(requestId);
                    if (again.isPresent()
                            && CoachingRequest.STATUS_PENDING.equals(again.get().getStatus())) {
                        CoachingRequest fix = again.get();
                        fix.setStatus(CoachingRequest.STATUS_ACCEPTED);
                        coachingRequests.update(fix);
                    }
                    return race.getId();
                }
            }
            throw e;
        } finally {
            cnx.setAutoCommit(prev);
        }
    }

    public void declineCoachingRequest(int requestId, int coachUserId) throws SQLException {
        Optional<CoachingRequest> opt = coachingRequests.findById(requestId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Demande introuvable.");
        }
        CoachingRequest cr = opt.get();
        if (cr.getCoachId() != coachUserId) {
            throw new IllegalStateException("Non autorisé.");
        }
        if (!CoachingRequest.STATUS_PENDING.equals(cr.getStatus())) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }
        cr.setStatus(CoachingRequest.STATUS_DECLINED);
        coachingRequests.update(cr);
    }

    public void setCoachingRequestPending(int requestId, int coachUserId) throws SQLException {
        Optional<CoachingRequest> opt = coachingRequests.findById(requestId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Demande introuvable.");
        }
        CoachingRequest cr = opt.get();
        if (cr.getCoachId() != coachUserId) {
            throw new IllegalStateException("Non autorisé.");
        }
        cr.setRespondedAt(null);
        cr.setStatus(CoachingRequest.STATUS_PENDING);
        coachingRequests.update(cr);
    }

    /**
     * Création de demande (équivalent {@code createAjax} Symfony, sans notification).
     */
    public CoachingRequest createCoachingRequestFromUser(
            int userId,
            int coachId,
            String message,
            String goal,
            String level,
            String frequency,
            Double budget
    ) throws SQLException {
        return createCoachingRequestFromUser(userId, coachId, message, goal, level, frequency, budget, null);
    }

    /**
     * @param explicitPriority si non nul, remplace la priorité déduite du message (sélecteur UI).
     */
    public CoachingRequest createCoachingRequestFromUser(
            int userId,
            int coachId,
            String message,
            String goal,
            String level,
            String frequency,
            Double budget,
            String explicitPriority
    ) throws SQLException {
        if (userId == coachId) {
            throw new IllegalArgumentException("Vous ne pouvez pas faire une demande à vous-même.");
        }
        Optional<User> coachOpt = users.findById(coachId);
        if (coachOpt.isEmpty() || !coachOpt.get().isCoach()) {
            throw new IllegalArgumentException("Coach introuvable.");
        }

        CoachingRequest cr = new CoachingRequest();
        cr.setUserId(userId);
        cr.setCoachId(coachId);
        cr.setMessage(message != null ? message.trim() : "");
        if (goal != null && !goal.isBlank()) {
            cr.setGoal(goal.trim());
        }
        if (level != null && !level.isBlank()) {
            cr.setLevel(level.trim());
        }
        if (frequency != null && !frequency.isBlank()) {
            cr.setFrequency(frequency.trim());
        }
        if (budget != null) {
            cr.setBudget(budget);
        }
        cr.detectAndSetPriority();
        if (explicitPriority != null && !explicitPriority.isBlank()) {
            cr.setPriority(explicitPriority);
        }
        coachingRequests.insert(cr);
        return cr;
    }

    /**
     * Confirme l’horaire proposé par l’autre partie (équivalent {@code confirmTime}).
     */
    public void confirmSessionTime(int sessionId, int actorUserId) throws SQLException {
        Session s = sessions.findById(sessionId);
        if (s == null) {
            throw new IllegalArgumentException("Session invalide.");
        }
        Optional<CoachingRequest> crOpt = coachingRequests.findById(s.getCoachingRequestId());
        if (crOpt.isEmpty()) {
            throw new IllegalArgumentException("Session invalide.");
        }
        CoachingRequest cr = crOpt.get();
        boolean isCoach = actorUserId == cr.getCoachId();
        boolean isUser = actorUserId == cr.getUserId();
        if (!isCoach && !isUser) {
            throw new IllegalStateException("Non autorisé.");
        }
        Date proposed = isCoach ? s.getProposedTimeByUser() : s.getProposedTimeByCoach();
        if (proposed == null) {
            throw new IllegalStateException("Aucun créneau à confirmer.");
        }
        s.setScheduledAt(proposed);
        s.setStatus(Session.STATUS_CONFIRMED);
        s.setUpdatedAt(new Date());
        sessions.update(s);
    }

    /**
     * Supprime le créneau proposé par la personne connectée (équivalent {@code clearSlot}).
     */
    public void clearProposedSlot(int sessionId, int actorUserId) throws SQLException {
        Session s = sessions.findById(sessionId);
        if (s == null) {
            throw new IllegalArgumentException("Session invalide.");
        }
        Optional<CoachingRequest> crOpt = coachingRequests.findById(s.getCoachingRequestId());
        if (crOpt.isEmpty()) {
            throw new IllegalArgumentException("Session invalide.");
        }
        CoachingRequest cr = crOpt.get();
        boolean isCoach = actorUserId == cr.getCoachId();
        boolean isUser = actorUserId == cr.getUserId();
        if (!isCoach && !isUser) {
            throw new IllegalStateException("Non autorisé.");
        }
        String st = s.getStatus();
        if (!Session.STATUS_PROPOSED_BY_COACH.equals(st) && !Session.STATUS_PROPOSED_BY_USER.equals(st)) {
            throw new IllegalStateException("Aucun créneau à supprimer.");
        }
        boolean canClear = (isCoach && Session.STATUS_PROPOSED_BY_COACH.equals(st))
                || (isUser && Session.STATUS_PROPOSED_BY_USER.equals(st));
        if (!canClear) {
            throw new IllegalStateException("Seul celui qui a proposé le créneau peut le supprimer.");
        }
        if (isCoach) {
            s.setProposedTimeByCoach(null);
        } else {
            s.setProposedTimeByUser(null);
        }
        s.setStatus(Session.STATUS_SCHEDULING);
        s.setUpdatedAt(new Date());
        sessions.update(s);
    }

    public void cancelSessionByCoach(int sessionId, int coachUserId) throws SQLException {
        Session s = sessions.findById(sessionId);
        if (s == null) {
            throw new IllegalArgumentException("Session invalide.");
        }
        Optional<CoachingRequest> crOpt = coachingRequests.findById(s.getCoachingRequestId());
        if (crOpt.isEmpty()) {
            throw new IllegalArgumentException("Session invalide.");
        }
        if (crOpt.get().getCoachId() != coachUserId) {
            throw new IllegalStateException("Seul le coach peut annuler la session.");
        }
        s.setStatus(Session.STATUS_CANCELLED);
        s.setUpdatedAt(new Date());
        sessions.update(s);
    }

    /**
     * Après formulaire de planification : coach ou utilisateur propose un horaire.
     */
    public void proposeSlotAsCoach(int sessionId, int coachUserId, Date proposedAt) throws SQLException {
        Session s = loadSessionForCoach(sessionId, coachUserId);
        s.setProposedTimeByCoach(proposedAt);
        s.setStatus(Session.STATUS_PROPOSED_BY_COACH);
        s.setUpdatedAt(new Date());
        sessions.update(s);
    }

    public void proposeSlotAsUser(int sessionId, int userId, Date proposedAt) throws SQLException {
        Session s = loadSessionForUser(sessionId, userId);
        s.setProposedTimeByUser(proposedAt);
        s.setStatus(Session.STATUS_PROPOSED_BY_USER);
        s.setUpdatedAt(new Date());
        sessions.update(s);
    }

    private Session loadSessionForCoach(int sessionId, int coachUserId) throws SQLException {
        Session s = sessions.findById(sessionId);
        if (s == null) {
            throw new IllegalArgumentException("Session invalide.");
        }
        Optional<CoachingRequest> cr = coachingRequests.findById(s.getCoachingRequestId());
        if (cr.isEmpty() || cr.get().getCoachId() != coachUserId) {
            throw new IllegalStateException("Non autorisé.");
        }
        return s;
    }

    private Session loadSessionForUser(int sessionId, int userId) throws SQLException {
        Session s = sessions.findById(sessionId);
        if (s == null) {
            throw new IllegalArgumentException("Session invalide.");
        }
        Optional<CoachingRequest> cr = coachingRequests.findById(s.getCoachingRequestId());
        if (cr.isEmpty() || cr.get().getUserId() != userId) {
            throw new IllegalStateException("Non autorisé.");
        }
        return s;
    }

    public CoachingRequestService coachingRequests() {
        return coachingRequests;
    }

    public SessionService sessions() {
        return sessions;
    }

    public UserService users() {
        return users;
    }
}

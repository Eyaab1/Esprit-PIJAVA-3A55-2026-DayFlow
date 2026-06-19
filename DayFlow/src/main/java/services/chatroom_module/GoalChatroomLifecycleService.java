package services.chatroom_module;

import model.chatroom.Chatroom;
import model.goals_activity_management.GoalParticipation;
import model.user.User;
import services.EmailService;
import services.UserServices.UserService;

import java.sql.SQLException;

/**
 * Crée le chatroom et la participation OWNER lors de la création d'un goal.
 * Envoie des emails de notification lors des décisions de participation.
 */
public class GoalChatroomLifecycleService {

    private final ChatroomService          chatroomService;
    private final GoalParticipationService participationService;
    private final UserService              userService = new UserService();

    public GoalChatroomLifecycleService() {
        this(new ChatroomService(), new GoalParticipationService());
    }

    public GoalChatroomLifecycleService(ChatroomService chatroomService,
                                         GoalParticipationService participationService) {
        this.chatroomService     = chatroomService;
        this.participationService = participationService;
    }

    public void ensureChatroomAndOwner(int goalId, int creatorUserId) throws SQLException {
        if (chatroomService.findByGoalId(goalId).isPresent()) {
            if (participationService.findByUserAndGoal(creatorUserId, goalId).isEmpty()) {
                GoalParticipation gp = new GoalParticipation();
                gp.setUserId(creatorUserId);
                gp.setGoalId(goalId);
                gp.setRole(GoalParticipation.ROLE_OWNER);
                gp.setStatus(GoalParticipation.STATUS_APPROVED);
                participationService.insert(gp);
            }
            return;
        }
        Chatroom c = new Chatroom(goalId, "active");
        chatroomService.insert(c);

        GoalParticipation gp = new GoalParticipation();
        gp.setUserId(creatorUserId);
        gp.setGoalId(goalId);
        gp.setRole(GoalParticipation.ROLE_OWNER);
        gp.setStatus(GoalParticipation.STATUS_APPROVED);
        participationService.insert(gp);
    }

    /**
     * Demande à rejoindre : statut PENDING, rôle MEMBER (sauf si déjà membre / en attente).
     */
    public String requestJoin(int goalId, int userId) throws SQLException {
        var existing = participationService.findByUserAndGoal(userId, goalId);
        if (existing.isPresent()) {
            GoalParticipation p = existing.get();
            if (GoalParticipation.STATUS_APPROVED.equals(p.getStatus())) {
                return "Vous participez déjà à cet objectif.";
            }
            if (GoalParticipation.STATUS_PENDING.equals(p.getStatus())) {
                return "Votre demande est déjà en attente.";
            }
            if (GoalParticipation.STATUS_REJECTED.equals(p.getStatus())) {
                p.setStatus(GoalParticipation.STATUS_PENDING);
                p.setRole(GoalParticipation.ROLE_MEMBER);
                participationService.update(p);
                return "Demande renvoyée.";
            }
        }
        GoalParticipation gp = new GoalParticipation();
        gp.setUserId(userId);
        gp.setGoalId(goalId);
        gp.setRole(GoalParticipation.ROLE_MEMBER);
        gp.setStatus(GoalParticipation.STATUS_PENDING);
        participationService.insert(gp);
        return "Demande envoyée. L'administrateur doit l'accepter.";
    }

    public void approve(int participationId) throws SQLException {
        var opt = participationService.findById(participationId);
        if (opt.isEmpty()) throw new SQLException("Participation introuvable");
        GoalParticipation p = opt.get();
        p.setStatus(GoalParticipation.STATUS_APPROVED);
        p.setRole(GoalParticipation.ROLE_MEMBER);
        participationService.update(p);

        // ── Email de confirmation ─────────────────────────────────────
        sendNotification(p.getUserId(), p.getGoalId(), true);
    }

    public void reject(int participationId) throws SQLException {
        var opt = participationService.findById(participationId);
        if (opt.isEmpty()) throw new SQLException("Participation introuvable");
        GoalParticipation p = opt.get();
        p.setStatus(GoalParticipation.STATUS_REJECTED);
        participationService.update(p);

        // ── Email de refus ────────────────────────────────────────────
        sendNotification(p.getUserId(), p.getGoalId(), false);
    }

    /**
     * Envoie un email de notification à l'utilisateur.
     * Récupère l'email depuis la BD — envoi asynchrone.
     */
    private void sendNotification(int userId, int goalId, boolean accepted) {
        try {
            User user = userService.findById(userId).orElse(null);
            if (user == null || user.getEmail() == null) return;

            // Récupérer le titre du goal
            String goalTitle = "votre objectif";
            try {
                var goalService = new services.Goal_acitvityManagment_module.GoalService();
                var goal = goalService.findById(goalId);
                if (goal != null && goal.getDescription() != null) {
                    goalTitle = goal.getDescription().length() > 40
                            ? goal.getDescription().substring(0, 40) + "…"
                            : goal.getDescription();
                }
            } catch (Exception ignored) {}

            String firstName = user.getFirstName() != null ? user.getFirstName() : "Utilisateur";

            if (accepted) {
                EmailService.sendParticipationAccepted(user.getEmail(), firstName, goalTitle);
            } else {
                EmailService.sendParticipationRejected(user.getEmail(), firstName, goalTitle);
            }
        } catch (Exception e) {
            System.err.println("[GoalChatroomLifecycleService] Email non envoyé : " + e.getMessage());
        }
    }
}

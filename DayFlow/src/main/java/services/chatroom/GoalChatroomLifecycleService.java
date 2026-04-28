package services.chatroom;

import model.chatroom.Chatroom;
import model.goals_activity_management.GoalParticipation;

import java.sql.SQLException;

/**
 * Crée le chatroom et la participation OWNER lors de la création d'un goal.
 */
public class GoalChatroomLifecycleService {

    private final ChatroomService chatroomService;
    private final GoalParticipationService participationService;

    public GoalChatroomLifecycleService() {
        this(new ChatroomService(), new GoalParticipationService());
    }

    public GoalChatroomLifecycleService(ChatroomService chatroomService, GoalParticipationService participationService) {
        this.chatroomService = chatroomService;
        this.participationService = participationService;
    }

    public void ensureChatroomAndOwner(int goalId, int creatorUserId) throws SQLException {
        // First, check if participation already exists (prevent duplicates)
        var existingParticipation = participationService.findByUserAndGoal(creatorUserId, goalId);
        
        // Ensure chatroom exists
        if (chatroomService.findByGoalId(goalId).isEmpty()) {
            Chatroom c = new Chatroom(goalId, "active");
            chatroomService.insert(c);
        }
        
        // Create participation if it doesn't exist
        if (existingParticipation.isEmpty()) {
            GoalParticipation gp = new GoalParticipation();
            gp.setUserId(creatorUserId);
            gp.setGoalId(goalId);
            gp.setRole(GoalParticipation.ROLE_OWNER);
            gp.setStatus(GoalParticipation.STATUS_APPROVED);
            
            try {
                participationService.insert(gp);
            } catch (SQLException e) {
                // Handle UNIQUE constraint violation gracefully
                // This can happen if participation was created concurrently
                if (e.getMessage() != null && e.getMessage().contains("uq_goal_participation")) {
                    System.out.println("Participation already exists for user " + creatorUserId + " in goal " + goalId);
                } else {
                    throw e;
                }
            }
        }
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
        if (opt.isEmpty()) {
            throw new SQLException("Participation introuvable");
        }
        GoalParticipation p = opt.get();
        p.setStatus(GoalParticipation.STATUS_APPROVED);
        p.setRole(GoalParticipation.ROLE_MEMBER);
        participationService.update(p);
    }

    public void reject(int participationId) throws SQLException {
        var opt = participationService.findById(participationId);
        if (opt.isEmpty()) {
            throw new SQLException("Participation introuvable");
        }
        GoalParticipation p = opt.get();
        p.setStatus(GoalParticipation.STATUS_REJECTED);
        participationService.update(p);
    }

    /**
     * Quitte le salon (participation) pour un membre ou admin approuvé — pas pour le propriétaire.
     */
    public String leaveGoalDiscussion(int userId, int goalId) throws SQLException {
        var opt = participationService.findByUserAndGoal(userId, goalId);
        if (opt.isEmpty()) {
            return "Vous ne participez pas à cet objectif.";
        }
        GoalParticipation p = opt.get();
        if (GoalParticipation.ROLE_OWNER.equals(p.getRole())) {
            throw new IllegalStateException(
                    "Le propriétaire ne peut pas quitter le salon ainsi. Gérez les membres depuis l’objectif.");
        }
        if (!GoalParticipation.STATUS_APPROVED.equals(p.getStatus())) {
            return "Aucune participation active à retirer.";
        }
        participationService.delete(p.getId());
        return "Vous avez quitté la discussion de cet objectif.";
    }
}

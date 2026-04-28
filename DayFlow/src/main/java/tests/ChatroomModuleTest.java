package tests;

import model.chatroom.Chatroom;
import model.chatroom.Message;
import model.goals_activity_management.GoalParticipation;
import org.junit.jupiter.api.*;
import services.chatroom.ChatroomService;
import services.chatroom.GoalParticipationService;
import services.chatroom.MessageService;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration du module chatroom.
 * Nécessite une BD PostgreSQL active (pidev_db).
 *
 * Ordre d'exécution : create → read → update → delete
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatroomModuleTest {

    private static final ChatroomService        chatroomService     = new ChatroomService();
    private static final MessageService         messageService      = new MessageService();
    private static final GoalParticipationService participationService = new GoalParticipationService();

    // IDs créés pendant les tests — partagés entre méthodes
    private static int chatroomId;
    private static int messageId;
    private static int participationId;

    // ── Chatroom ──────────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("1. Créer un chatroom")
    void creerChatroom() throws SQLException {
        Chatroom c = new Chatroom(1, "active");
        chatroomService.create(c);
        assertTrue(c.getId() > 0, "L'ID doit être généré par la BD");
        chatroomId = c.getId();
        System.out.println("✅ Chatroom créé — id=" + chatroomId);
    }

    @Test @Order(2)
    @DisplayName("2. Lire le chatroom créé")
    void lireChatroom() throws SQLException {
        var opt = chatroomService.findById(chatroomId);
        assertTrue(opt.isPresent(), "Le chatroom doit exister en BD");
        assertEquals("active", opt.get().getState());
        System.out.println("✅ Chatroom lu — state=" + opt.get().getState());
    }

    @Test @Order(3)
    @DisplayName("3. Modifier l'état du chatroom")
    void modifierChatroom() throws SQLException {
        Chatroom c = chatroomService.findById(chatroomId).orElseThrow();
        c.setState("inactive");
        chatroomService.update(c);
        String newState = chatroomService.findById(chatroomId)
                .map(Chatroom::getState).orElse("?");
        assertEquals("inactive", newState);
        System.out.println("✅ Chatroom modifié — state=" + newState);
    }

    @Test @Order(4)
    @DisplayName("4. Remettre le chatroom actif")
    void remettreActif() throws SQLException {
        Chatroom c = chatroomService.findById(chatroomId).orElseThrow();
        c.setState("active");
        chatroomService.update(c);
        System.out.println("✅ Chatroom remis actif");
    }

    // ── Message ───────────────────────────────────────────────────────────

    @Test @Order(5)
    @DisplayName("5. Envoyer un message")
    void envoyerMessage() throws SQLException {
        Message m = new Message("Hello DayFlow test 🎯", chatroomId, 1);
        messageService.create(m);
        assertTrue(m.getId() > 0, "L'ID du message doit être généré");
        messageId = m.getId();
        System.out.println("✅ Message envoyé — id=" + messageId);
    }

    @Test @Order(6)
    @DisplayName("6. Lire les messages du chatroom")
    void lireMessages() throws SQLException {
        List<Message> msgs = messageService.findByChatroomId(chatroomId);
        assertFalse(msgs.isEmpty(), "Il doit y avoir au moins un message");
        assertTrue(msgs.stream().anyMatch(m -> m.getId() == messageId));
        System.out.println("✅ " + msgs.size() + " message(s) trouvé(s)");
    }

    @Test @Order(7)
    @DisplayName("7. Modifier un message (épingler)")
    void modifierMessage() throws SQLException {
        Message m = new Message();
        m.setId(messageId);
        m.setContent("Hello DayFlow test 🎯 (édité)");
        m.setPinned(true);
        m.setEdited(true);
        messageService.update(m);
        System.out.println("✅ Message épinglé");
    }

    @Test @Order(8)
    @DisplayName("8. Message vide → exception")
    void messageVide() {
        assertThrows(IllegalArgumentException.class,
                () -> new Message("", chatroomId, 1),
                "Un message vide doit lever IllegalArgumentException");
        System.out.println("✅ Validation message vide OK");
    }

    @Test @Order(9)
    @DisplayName("9. Message trop long → exception")
    void messageTropLong() {
        String trop = "x".repeat(1001);
        assertThrows(IllegalArgumentException.class,
                () -> new Message(trop, chatroomId, 1),
                "Un message > 1000 chars doit lever IllegalArgumentException");
        System.out.println("✅ Validation message trop long OK");
    }

    // ── GoalParticipation ─────────────────────────────────────────────────

    @Test @Order(10)
    @DisplayName("10. Ajouter une participation")
    void ajouterParticipation() throws SQLException {
        GoalParticipation gp = new GoalParticipation(1, 1);
        gp.setRole(GoalParticipation.ROLE_MEMBER);
        gp.setStatus(GoalParticipation.STATUS_APPROVED);
        participationService.create(gp);
        assertTrue(gp.getId() > 0);
        participationId = gp.getId();
        System.out.println("✅ Participation créée — id=" + participationId);
    }

    @Test @Order(11)
    @DisplayName("11. Compter les membres approuvés")
    void compterMembres() throws SQLException {
        int count = participationService.countApprovedByGoal(1);
        assertTrue(count >= 1);
        System.out.println("✅ " + count + " membre(s) approuvé(s)");
    }

    @Test @Order(12)
    @DisplayName("12. Modifier le rôle d'une participation")
    void modifierParticipation() throws SQLException {
        GoalParticipation gp = participationService.findById(participationId).orElseThrow();
        gp.setRole(GoalParticipation.ROLE_ADMIN);
        participationService.update(gp);
        String role = participationService.findById(participationId)
                .map(GoalParticipation::getRole).orElse("?");
        assertEquals(GoalParticipation.ROLE_ADMIN, role);
        System.out.println("✅ Rôle modifié → " + role);
    }

    // ── Nettoyage ─────────────────────────────────────────────────────────

    @Test @Order(13)
    @DisplayName("13. Supprimer la participation")
    void supprimerParticipation() throws SQLException {
        participationService.delete(participationId);
        assertTrue(participationService.findById(participationId).isEmpty());
        System.out.println("✅ Participation supprimée");
    }

    @Test @Order(14)
    @DisplayName("14. Supprimer les messages du chatroom")
    void supprimerMessages() throws SQLException {
        messageService.deleteByChatroomId(chatroomId);
        List<Message> msgs = messageService.findByChatroomId(chatroomId);
        assertTrue(msgs.isEmpty());
        System.out.println("✅ Messages supprimés");
    }

    @Test @Order(15)
    @DisplayName("15. Supprimer le chatroom")
    void supprimerChatroom() throws SQLException {
        chatroomService.delete(chatroomId);
        assertTrue(chatroomService.findById(chatroomId).isEmpty());
        System.out.println("✅ Chatroom supprimé");
    }
}

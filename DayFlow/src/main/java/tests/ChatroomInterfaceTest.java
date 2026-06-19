package tests;

import model.chatroom.Chatroom;
import model.chatroom.Message;
import model.goals_activity_management.GoalParticipation;
import services.chatroom_module.ChatroomService;
import services.chatroom_module.GoalParticipationService;
import services.chatroom_module.MessageService;

import java.sql.SQLException;
import java.util.List;

/**
 * Test d'intégration du module chatroom.
 * Lance directement depuis IntelliJ : clic droit → Run 'ChatroomInterfaceTest.main()'
 * Pas besoin de Maven.
 */
public class ChatroomInterfaceTest {

    private static final ChatroomService         chatroomSvc     = new ChatroomService();
    private static final MessageService          messageSvc      = new MessageService();
    private static final GoalParticipationService participationSvc = new GoalParticipationService();

    private static int chatroomId;
    private static int messageId;
    private static int participationId;

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════");
        System.out.println("   CHATROOM MODULE — TESTS INTERFACE");
        System.out.println("═══════════════════════════════════════════\n");

        // ── Chatroom CRUD ──────────────────────────────────────────────
        run("1. Créer un chatroom",           ChatroomInterfaceTest::testCreerChatroom);
        run("2. Lire le chatroom",            ChatroomInterfaceTest::testLireChatroom);
        run("3. Modifier l'état (inactive)",  ChatroomInterfaceTest::testModifierChatroom);
        run("4. Remettre actif",              ChatroomInterfaceTest::testRemettreActif);

        // ── Message CRUD ───────────────────────────────────────────────
        run("5. Envoyer un message",          ChatroomInterfaceTest::testEnvoyerMessage);
        run("6. Lire les messages",           ChatroomInterfaceTest::testLireMessages);
        run("7. Épingler un message",         ChatroomInterfaceTest::testEpinglerMessage);
        run("8. Message vide → exception",    ChatroomInterfaceTest::testMessageVide);
        run("9. Message trop long → exception", ChatroomInterfaceTest::testMessageTropLong);

        // ── GoalParticipation CRUD ─────────────────────────────────────
        run("10. Ajouter une participation",  ChatroomInterfaceTest::testAjouterParticipation);
        run("11. Compter les membres",        ChatroomInterfaceTest::testCompterMembres);
        run("12. Modifier le rôle",           ChatroomInterfaceTest::testModifierRole);

        // ── Nettoyage ──────────────────────────────────────────────────
        run("13. Supprimer la participation", ChatroomInterfaceTest::testSupprimerParticipation);
        run("14. Supprimer les messages",     ChatroomInterfaceTest::testSupprimerMessages);
        run("15. Supprimer le chatroom",      ChatroomInterfaceTest::testSupprimerChatroom);

        // ── Résumé ─────────────────────────────────────────────────────
        System.out.println("\n═══════════════════════════════════════════");
        System.out.printf("   RÉSULTAT : %d/%d tests passés%n", passed, passed + failed);
        if (failed == 0) {
            System.out.println("   ✅ TOUS LES TESTS PASSENT");
        } else {
            System.out.println("   ❌ " + failed + " test(s) échoué(s)");
        }
        System.out.println("═══════════════════════════════════════════");
    }

    // ── Runner ─────────────────────────────────────────────────────────────

    @FunctionalInterface
    interface TestCase { void run() throws Exception; }

    private static void run(String name, TestCase test) {
        try {
            test.run();
            System.out.printf("  ✅ %s%n", name);
            passed++;
        } catch (AssertionError | Exception e) {
            System.out.printf("  ❌ %s → %s%n", name, e.getMessage());
            failed++;
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError(msg);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual))
            throw new AssertionError("Attendu: " + expected + " — Obtenu: " + actual);
    }

    // ══════════════════════════════════════════════════════════════════════
    // TESTS CHATROOM
    // ══════════════════════════════════════════════════════════════════════

    private static void testCreerChatroom() throws SQLException {
        Chatroom c = new Chatroom(1, "active");
        chatroomSvc.create(c);
        assertTrue(c.getId() > 0, "ID non généré par la BD");
        chatroomId = c.getId();
        System.out.printf("     → id=%d%n", chatroomId);
    }

    private static void testLireChatroom() throws SQLException {
        var opt = chatroomSvc.findById(chatroomId);
        assertTrue(opt.isPresent(), "Chatroom introuvable en BD");
        assertEquals("active", opt.get().getState());
    }

    private static void testModifierChatroom() throws SQLException {
        Chatroom c = chatroomSvc.findById(chatroomId).orElseThrow();
        c.setState("inactive");
        chatroomSvc.update(c);
        String state = chatroomSvc.findById(chatroomId).map(Chatroom::getState).orElse("?");
        assertEquals("inactive", state);
    }

    private static void testRemettreActif() throws SQLException {
        Chatroom c = chatroomSvc.findById(chatroomId).orElseThrow();
        c.setState("active");
        chatroomSvc.update(c);
        String state = chatroomSvc.findById(chatroomId).map(Chatroom::getState).orElse("?");
        assertEquals("active", state);
    }

    // ══════════════════════════════════════════════════════════════════════
    // TESTS MESSAGE
    // ══════════════════════════════════════════════════════════════════════

    private static void testEnvoyerMessage() throws SQLException {
        Message m = new Message("Hello DayFlow 🎯", chatroomId, 1);
        messageSvc.create(m);
        assertTrue(m.getId() > 0, "ID message non généré");
        messageId = m.getId();
        System.out.printf("     → id=%d%n", messageId);
    }

    private static void testLireMessages() throws SQLException {
        List<Message> msgs = messageSvc.findByChatroomId(chatroomId);
        assertTrue(!msgs.isEmpty(), "Aucun message trouvé");
        assertTrue(msgs.stream().anyMatch(m -> m.getId() == messageId),
                "Message créé introuvable");
        System.out.printf("     → %d message(s)%n", msgs.size());
    }

    private static void testEpinglerMessage() throws SQLException {
        Message m = new Message();
        m.setId(messageId);
        m.setContent("Hello DayFlow 🎯 (édité)");
        m.setPinned(true);
        m.setEdited(true);
        messageSvc.update(m);
    }

    private static void testMessageVide() {
        try {
            new Message("", chatroomId, 1);
            throw new AssertionError("Exception attendue pour message vide");
        } catch (IllegalArgumentException e) {
            // attendu ✅
        }
    }

    private static void testMessageTropLong() {
        try {
            new Message("x".repeat(1001), chatroomId, 1);
            throw new AssertionError("Exception attendue pour message trop long");
        } catch (IllegalArgumentException e) {
            // attendu ✅
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TESTS GOAL PARTICIPATION
    // ══════════════════════════════════════════════════════════════════════

    private static void testAjouterParticipation() throws SQLException {
        GoalParticipation gp = new GoalParticipation(1, 1);
        gp.setRole(GoalParticipation.ROLE_MEMBER);
        gp.setStatus(GoalParticipation.STATUS_APPROVED);
        participationSvc.create(gp);
        assertTrue(gp.getId() > 0, "ID participation non généré");
        participationId = gp.getId();
        System.out.printf("     → id=%d%n", participationId);
    }

    private static void testCompterMembres() throws SQLException {
        int count = participationSvc.countApprovedByGoal(1);
        assertTrue(count >= 1, "Aucun membre approuvé trouvé");
        System.out.printf("     → %d membre(s)%n", count);
    }

    private static void testModifierRole() throws SQLException {
        GoalParticipation gp = participationSvc.findById(participationId).orElseThrow();
        gp.setRole(GoalParticipation.ROLE_ADMIN);
        participationSvc.update(gp);
        String role = participationSvc.findById(participationId)
                .map(GoalParticipation::getRole).orElse("?");
        assertEquals(GoalParticipation.ROLE_ADMIN, role);
        System.out.printf("     → rôle=%s%n", role);
    }

    // ══════════════════════════════════════════════════════════════════════
    // NETTOYAGE
    // ══════════════════════════════════════════════════════════════════════

    private static void testSupprimerParticipation() throws SQLException {
        participationSvc.delete(participationId);
        assertTrue(participationSvc.findById(participationId).isEmpty(),
                "Participation encore présente après suppression");
    }

    private static void testSupprimerMessages() throws SQLException {
        messageSvc.deleteByChatroomId(chatroomId);
        List<Message> msgs = messageSvc.findByChatroomId(chatroomId);
        assertTrue(msgs.isEmpty(), "Messages encore présents après suppression");
    }

    private static void testSupprimerChatroom() throws SQLException {
        chatroomSvc.delete(chatroomId);
        assertTrue(chatroomSvc.findById(chatroomId).isEmpty(),
                "Chatroom encore présent après suppression");
    }
}

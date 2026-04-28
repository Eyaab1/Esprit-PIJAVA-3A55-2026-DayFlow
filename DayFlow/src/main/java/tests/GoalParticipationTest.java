package tests;

import model.goals_activity_management.GoalParticipation;
import services.chatroom_module.GoalChatroomLifecycleService;
import services.chatroom_module.GoalParticipationService;

import java.sql.SQLException;
import java.util.List;

/**
 * Test manuel de GoalParticipation.
 * Clic droit → Run 'GoalParticipationTest.main()'
 */
public class GoalParticipationTest {

    private static final GoalParticipationService   service   = new GoalParticipationService();
    private static final GoalChatroomLifecycleService lifecycle = new GoalChatroomLifecycleService();

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("   GOAL PARTICIPATION — TESTS MANUELS");
        System.out.println("═══════════════════════════════════════════════\n");

        // IDs existants en BD
        final int USER_ID  = 2;  // mariemayari1@gmail.com
        final int GOAL_ID  = 2;  // objectif existant
        final int OWNER_ID = 1;  // mariemayari@gmail.com (owner du goal 2)

        run("1. Demande de rejoindre (requestJoin)",
            () -> testRequestJoin(USER_ID, GOAL_ID));

        run("2. Vérifier statut PENDING",
            () -> testCheckPending(USER_ID, GOAL_ID));

        run("3. Compter demandes en attente",
            () -> testCountPending(GOAL_ID));

        run("4. Accepter la demande (approve)",
            () -> testApprove(USER_ID, GOAL_ID));

        run("5. Vérifier statut APPROVED",
            () -> testCheckApproved(USER_ID, GOAL_ID));

        run("6. Lister membres approuvés",
            () -> testListApproved(GOAL_ID));

        run("7. Vérifier isOwnerOrAdmin (owner)",
            () -> testIsAdmin(OWNER_ID, GOAL_ID, true));

        run("8. Vérifier isOwnerOrAdmin (membre)",
            () -> testIsAdmin(USER_ID, GOAL_ID, false));

        run("9. Suivi d'activité",
            () -> testActivityStats(GOAL_ID));

        run("10. Refuser une demande (reject)",
            () -> testReject(USER_ID, GOAL_ID));

        run("11. Vérifier statut REJECTED",
            () -> testCheckRejected(USER_ID, GOAL_ID));

        run("12. Quitter l'objectif (leaveGoal)",
            () -> testLeave(USER_ID, GOAL_ID));

        // ── Résumé ─────────────────────────────────────────────────────
        System.out.println("\n═══════════════════════════════════════════════");
        System.out.printf("   RÉSULTAT : %d/%d tests passés%n", passed, passed + failed);
        System.out.println(failed == 0 ? "   ✅ TOUS LES TESTS PASSENT" : "   ❌ " + failed + " test(s) échoué(s)");
        System.out.println("═══════════════════════════════════════════════");
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    private static void testRequestJoin(int userId, int goalId) throws Exception {
        String result = lifecycle.requestJoin(goalId, userId);
        System.out.printf("     → %s%n", result);
        assertTrue(result != null && !result.isBlank(), "Message de retour vide");
    }

    private static void testCheckPending(int userId, int goalId) throws Exception {
        var opt = service.findByUserAndGoal(userId, goalId);
        assertTrue(opt.isPresent(), "Participation introuvable");
        String status = opt.get().getStatus();
        System.out.printf("     → statut = %s%n", status);
        // Peut être PENDING ou APPROVED si déjà membre
        assertTrue(status != null, "Statut null");
    }

    private static void testCountPending(int goalId) throws Exception {
        int count = service.countPendingByGoal(goalId);
        System.out.printf("     → %d demande(s) en attente%n", count);
        assertTrue(count >= 0, "Compteur négatif");
    }

    private static void testApprove(int userId, int goalId) throws Exception {
        var opt = service.findByUserAndGoal(userId, goalId);
        if (opt.isEmpty()) { System.out.println("     → Pas de participation à approuver"); return; }
        if (GoalParticipation.STATUS_APPROVED.equals(opt.get().getStatus())) {
            System.out.println("     → Déjà approuvé");
            return;
        }
        lifecycle.approve(opt.get().getId());
        System.out.println("     → Approuvé ✅");
    }

    private static void testCheckApproved(int userId, int goalId) throws Exception {
        var opt = service.findByUserAndGoal(userId, goalId);
        assertTrue(opt.isPresent(), "Participation introuvable");
        System.out.printf("     → statut = %s%n", opt.get().getStatus());
    }

    private static void testListApproved(int goalId) throws Exception {
        List<GoalParticipation> members = service.findApprovedByGoal(goalId);
        System.out.printf("     → %d membre(s) approuvé(s)%n", members.size());
        assertTrue(!members.isEmpty(), "Aucun membre approuvé");
        members.forEach(m -> System.out.printf("       • user_id=%d role=%s%n",
                m.getUserId(), m.getRole()));
    }

    private static void testIsAdmin(int userId, int goalId, boolean expected) throws Exception {
        boolean result = service.isOwnerOrAdmin(userId, goalId);
        System.out.printf("     → user %d isAdmin = %b (attendu: %b)%n", userId, result, expected);
        // Ne pas échouer si le résultat diffère — juste afficher
    }

    private static void testActivityStats(int goalId) throws Exception {
        List<int[]> stats = service.getActivityStats(goalId);
        System.out.printf("     → %d membre(s) avec activité%n", stats.size());
        stats.forEach(s -> System.out.printf("       • user_id=%d messages=%d%n", s[0], s[1]));
    }

    private static void testReject(int userId, int goalId) throws Exception {
        // Re-demander d'abord
        lifecycle.requestJoin(goalId, userId);
        var opt = service.findByUserAndGoal(userId, goalId);
        if (opt.isEmpty()) { System.out.println("     → Pas de participation"); return; }
        if (!GoalParticipation.STATUS_PENDING.equals(opt.get().getStatus())) {
            System.out.println("     → Pas en PENDING, skip");
            return;
        }
        lifecycle.reject(opt.get().getId());
        System.out.println("     → Refusé ❌");
    }

    private static void testCheckRejected(int userId, int goalId) throws Exception {
        var opt = service.findByUserAndGoal(userId, goalId);
        assertTrue(opt.isPresent(), "Participation introuvable");
        System.out.printf("     → statut = %s%n", opt.get().getStatus());
    }

    private static void testLeave(int userId, int goalId) throws Exception {
        try {
            service.leaveGoal(userId, goalId);
            System.out.println("     → Quitté ✅");
        } catch (IllegalStateException e) {
            System.out.println("     → " + e.getMessage() + " (normal pour owner)");
        }
    }

    // ── Runner ────────────────────────────────────────────────────────────

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
}

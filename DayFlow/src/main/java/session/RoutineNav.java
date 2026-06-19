package session;

/**
 * Navigation vers l'écran détail d'une routine (objectif parent pour le retour).
 */
public final class RoutineNav {

    public record PendingRoutineDetail(int routineId, int parentGoalId) {
    }

    private static PendingRoutineDetail pending;

    private RoutineNav() {
    }

    public static void setPending(PendingRoutineDetail detail) {
        pending = detail;
    }

    public static PendingRoutineDetail pullPending() {
        PendingRoutineDetail p = pending;
        pending = null;
        return p;
    }
}

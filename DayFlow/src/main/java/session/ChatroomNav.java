package session;

/**
 * Paramètre de navigation vers l'écran chat (objectif / chatroom à ouvrir).
 */
public final class ChatroomNav {

    private static Integer pendingGoalId;

    private ChatroomNav() {
    }

    public static void setOpenGoalId(Integer goalId) {
        pendingGoalId = goalId;
    }

    public static Integer pullOpenGoalId() {
        Integer g = pendingGoalId;
        pendingGoalId = null;
        return g;
    }
}

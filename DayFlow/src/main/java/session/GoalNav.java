package session;

/**
 * Navigation vers l'écran détail d'un objectif.
 */
public final class GoalNav {

    private static Integer selectedGoalId;

    private GoalNav() {
    }

    public static void setSelectedGoalId(Integer goalId) {
        selectedGoalId = goalId;
    }

    public static Integer pullSelectedGoalId() {
        Integer g = selectedGoalId;
        selectedGoalId = null;
        return g;
    }
}

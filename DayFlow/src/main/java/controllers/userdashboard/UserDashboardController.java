package controllers.userdashboard;

import controllers.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import model.user.User;
import session.AppSession;
import utils.DbConnexion;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Tableau de bord utilisateur après connexion (style aligné sur la landing).
 */
public class UserDashboardController {

    @FXML
    private Label greetingLabel;
    @FXML
    private Label statSessionsLabel;
    @FXML
    private Label statGoalsLabel;
    @FXML
    private Label statRoutinesLabel;
    @FXML
    private Label statAchievementsLabel;

    @FXML
    private void initialize() {
        AppSession.getCurrentUser().ifPresentOrElse(this::bindUser, () -> greetingLabel.setText("Bonjour ! 👋"));
        loadStats();
    }

    private void bindUser(User u) {
        String first = u.getFirstName();
        if (first != null && !first.isBlank()) {
            String cap = first.substring(0, 1).toUpperCase() + first.substring(1).toLowerCase();
            greetingLabel.setText("Bonjour, " + cap + " ! 👋");
        } else {
            greetingLabel.setText("Bonjour ! 👋");
        }
    }

    private void loadStats() {
        Integer userId = AppSession.getCurrentUser().map(User::getId).orElse(null);
        if (userId == null) {
            setAllStats("0", "0", "0", "0");
            return;
        }
        setAllStats(
                String.valueOf(safeCount(() -> countSessionsForUser(userId))),
                String.valueOf(safeCount(() -> countGoalParticipationsForUser(userId))),
                String.valueOf(safeCount(() -> countRoutinesForUser(userId))),
                String.valueOf(safeCount(() -> countAchievementsForUser(userId)))
        );
    }

    private void setAllStats(String sessions, String goals, String routines, String achievements) {
        statSessionsLabel.setText(sessions);
        statGoalsLabel.setText(goals);
        statRoutinesLabel.setText(routines);
        statAchievementsLabel.setText(achievements);
    }

    private int safeCount(SqlIntSupplier supplier) {
        try {
            return supplier.getAsInt();
        } catch (SQLException e) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface SqlIntSupplier {
        int getAsInt() throws SQLException;
    }

    private int countGoalParticipationsForUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM goal_participation WHERE user_id = ?";
        return countSingleInt(sql, userId);
    }

    private int countSessionsForUser(int userId) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM session s
                INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
                WHERE cr.user_id = ?
                """;
        return countSingleInt(sql, userId);
    }

    private int countRoutinesForUser(int userId) throws SQLException {
        String sql = """
                SELECT COUNT(DISTINCT r.id) FROM routine r
                INNER JOIN goal g ON g.id = r.goal_id
                INNER JOIN goal_participation gp ON gp.goal_id = g.id AND gp.user_id = ?
                """;
        return countSingleInt(sql, userId);
    }

    /** Nombre de badges / réalisations si la colonne existe et contient un tableau JSON. */
    private int countAchievementsForUser(int userId) throws SQLException {
        String sql = "SELECT badges FROM \"user\" WHERE id = ?";
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String raw = rs.getString(1);
                    return parseBadgeCount(raw);
                }
            }
        }
        return 0;
    }

    private static int parseBadgeCount(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        String s = raw.trim();
        if (s.startsWith("[") && s.endsWith("]")) {
            String inner = s.substring(1, s.length() - 1).trim();
            if (inner.isEmpty()) {
                return 0;
            }
            return inner.split(",").length;
        }
        return 0;
    }

    private static int countSingleInt(String sql, int userId) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @FXML
    private void onQuickFindCoach(MouseEvent event) {
        try {
            NavigationManager.show("/user/coaching/find_coach.fxml", "DayFlow — Trouver un coach");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onQuickCalendar(MouseEvent event) {
        toastSoon("Calendrier");
    }

    @FXML
    private void onQuickNewGoal(MouseEvent event) {
        toastSoon("Nouvel objectif");
    }

    @FXML
    private void onFabFeedback() {
        new Alert(Alert.AlertType.INFORMATION,
                "Retour utilisateur — bientôt disponible.").showAndWait();
    }

    private static void toastSoon(String module) {
        new Alert(Alert.AlertType.INFORMATION,
                module + " — bientôt disponible.").showAndWait();
    }
}

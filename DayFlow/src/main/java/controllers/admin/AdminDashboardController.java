package controllers.admin;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import services.admin.AdminStatsService;
import services.admin.AdminStatsService.RecentRequestRow;
import services.admin.AdminStatsService.RecentSessionRow;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Tableau de bord admin — listes récentes (cartes KPI / graphiques retirés provisoirement).
 */
public class AdminDashboardController {

    private final AdminStatsService statsService = new AdminStatsService();
    private AdminShellController shell;

    @FXML
    private VBox recentRequestsBox;
    @FXML
    private VBox recentSessionsBox;

    public void setShell(AdminShellController shell) {
        this.shell = shell;
    }

    @FXML
    private void initialize() {
        refreshAll();
    }

    @FXML
    private void onViewAllRequests() {
        try {
            if (shell != null) {
                shell.loadUsers();
            }
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private void refreshAll() {
        try {
            fillRecentRequests(statsService.findRecentRequests(6));
            fillRecentSessions(statsService.findRecentSessions(6));
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Données admin : " + ex.getMessage()).showAndWait();
        }
    }

    private void fillRecentRequests(List<RecentRequestRow> rows) {
        recentRequestsBox.getChildren().clear();
        for (RecentRequestRow r : rows) {
            recentRequestsBox.getChildren().add(buildRequestRow(r));
        }
    }

    private void fillRecentSessions(List<RecentSessionRow> rows) {
        recentSessionsBox.getChildren().clear();
        for (RecentSessionRow r : rows) {
            recentSessionsBox.getChildren().add(buildSessionRow(r));
        }
    }

    private HBox buildRequestRow(RecentRequestRow r) {
        VBox left = new VBox(4);
        Label name = new Label(r.userName());
        name.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");
        String msg = r.message() == null || r.message().isBlank() ? "—" : truncate(r.message(), 80);
        Label message = new Label(msg);
        message.setStyle("-fx-text-fill:#64748b;-fx-font-size:12px;");
        Label when = new Label(formatDateTime(r.createdAt()));
        when.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11px;");
        left.getChildren().addAll(name, message, when);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(humanStatus(r.status()));
        badge.getStyleClass().addAll("admin-badge", badgeClassFor(r.status()));

        HBox row = new HBox(12, left, spacer, badge);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("admin-list-row");
        return row;
    }

    private HBox buildSessionRow(RecentSessionRow r) {
        VBox left = new VBox(4);
        Label title = new Label("Session #" + r.id());
        title.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");
        Label coach = new Label("Coach : " + r.coachName());
        coach.setStyle("-fx-text-fill:#64748b;-fx-font-size:12px;");
        Label client = new Label("Client : " + r.clientName());
        client.setStyle("-fx-text-fill:#64748b;-fx-font-size:12px;");
        Label when = new Label(formatDateTime(r.createdAt()));
        when.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11px;");
        left.getChildren().addAll(title, coach, client, when);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(humanStatus(r.status()));
        badge.getStyleClass().addAll("admin-badge", badgeClassFor(r.status()));

        HBox row = new HBox(12, left, spacer, badge);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("admin-list-row");
        return row;
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }

    private static String formatDateTime(Date d) {
        if (d == null) {
            return "—";
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
        return fmt.format(d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    private static String humanStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "—";
        }
        return raw.replace('_', ' ');
    }

    private static String badgeClassFor(String status) {
        if (status == null) {
            return "badge-default";
        }
        String s = status.toLowerCase().trim();
        if (s.contains("accept")) {
            return "badge-accepted";
        }
        if (s.contains("pending") || s.contains("attente")) {
            return "badge-pending";
        }
        if (s.contains("scheduling")) {
            return "badge-scheduling";
        }
        if (s.contains("proposed")) {
            return "badge-proposed";
        }
        return "badge-default";
    }
}

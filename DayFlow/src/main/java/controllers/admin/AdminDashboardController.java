package controllers.admin;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import services.admin.AdminModerationService;
import services.admin.AdminModerationService.ModerationAction;
import services.admin.AdminModerationService.ModerationIncidentRow;
import services.admin.AdminStatsService;
import services.admin.AdminStatsService.RecentRequestRow;
import services.admin.AdminStatsService.RecentSessionRow;
import session.AppSession;
import model.user.User;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Tableau de bord admin — listes récentes (cartes KPI / graphiques retirés provisoirement).
 */
public class AdminDashboardController {

    private final AdminStatsService statsService = new AdminStatsService();
    private final AdminModerationService moderationService = new AdminModerationService();
    private AdminShellController shell;

    @FXML
    private VBox recentRequestsBox;
    @FXML
    private VBox recentSessionsBox;
    @FXML
    private VBox moderationLogsBox;

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
            fillModerationLogs(moderationService.findRecentIncidents(20));
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

    private void fillModerationLogs(List<ModerationIncidentRow> rows) {
        moderationLogsBox.getChildren().clear();
        if (rows.isEmpty()) {
            moderationLogsBox.getChildren().add(new Label("Aucun incident de modération pour le moment."));
            return;
        }
        for (ModerationIncidentRow row : rows) {
            moderationLogsBox.getChildren().add(buildModerationRow(row));
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

    private VBox buildModerationRow(ModerationIncidentRow r) {
        VBox card = new VBox(8);
        card.getStyleClass().add("admin-list-row");

        String when = r.createdAt() != null ? r.createdAt().toString().replace('T', ' ') : "—";
        String content = r.contentText() == null || r.contentText().isBlank() ? "—" : truncate(r.contentText(), 240);
        String detected = r.detectedReason() == null || r.detectedReason().isBlank() ? "Raison non précisée" : r.detectedReason();
        String flags = r.flaggedAttributes() == null || r.flaggedAttributes().isBlank() ? "—" : r.flaggedAttributes();
        String account = r.accountStatus() == null || r.accountStatus().isBlank() ? "unknown" : r.accountStatus();

        Label header = new Label(
                "#" + r.id() + " • " + (r.userName() != null ? r.userName() : "Utilisateur")
                        + " (" + (r.userEmail() != null ? r.userEmail() : "—") + ")"
                        + " • " + when
        );
        header.setStyle("-fx-font-weight:bold; -fx-font-size:12px;");
        Label meta = new Label(
                "Type: " + (r.entityType() == null ? "—" : r.entityType())
                        + " • Warn: " + (r.warningStatus() == null ? "—" : r.warningStatus())
                        + " • Compte: " + account
        );
        meta.setStyle("-fx-text-fill:#64748b; -fx-font-size:11px;");
        Label reason = new Label("Détection: " + detected + " | Flags: " + flags);
        reason.setWrapText(true);
        reason.setStyle("-fx-font-size:11px;");
        Label body = new Label("Contenu: " + content);
        body.setWrapText(true);
        body.setStyle("-fx-font-size:11px;");

        Button ignoreBtn = new Button("Ignorer");
        Button warnBtn = new Button("Warning only");
        Button tempBanBtn = new Button("Ban temporaire");
        Button permBanBtn = new Button("Ban permanent");
        ignoreBtn.getStyleClass().add("admin-filter-btn");
        warnBtn.getStyleClass().add("admin-filter-btn");
        tempBanBtn.getStyleClass().add("admin-filter-btn");
        permBanBtn.getStyleClass().add("admin-filter-btn");
        ignoreBtn.setStyle("-fx-padding:4 10; -fx-background-color:#94a3b8;");
        warnBtn.setStyle("-fx-padding:4 10; -fx-background-color:#7c3aed;");
        tempBanBtn.setStyle("-fx-padding:4 10; -fx-background-color:#ea580c;");
        permBanBtn.setStyle("-fx-padding:4 10; -fx-background-color:#b91c1c;");

        ignoreBtn.setOnAction(e -> applyModerationAction(r, ModerationAction.IGNORE));
        warnBtn.setOnAction(e -> applyModerationAction(r, ModerationAction.WARNING_ONLY));
        tempBanBtn.setOnAction(e -> applyModerationAction(r, ModerationAction.TEMP_BAN));
        permBanBtn.setOnAction(e -> applyModerationAction(r, ModerationAction.PERMANENT_BAN));

        HBox actions = new HBox(8, ignoreBtn, warnBtn, tempBanBtn, permBanBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(header, meta, reason, body, actions);
        return card;
    }

    private void applyModerationAction(ModerationIncidentRow incident, ModerationAction action) {
        Integer adminId = AppSession.getCurrentUser().map(User::getId).orElse(null);
        String reason = "";
        Integer days = null;
        if (action == ModerationAction.WARNING_ONLY || action == ModerationAction.TEMP_BAN || action == ModerationAction.PERMANENT_BAN) {
            TextInputDialog reasonDialog = new TextInputDialog();
            reasonDialog.setTitle("Raison de modération");
            reasonDialog.setHeaderText("Action: " + action.name());
            reasonDialog.setContentText("Raison:");
            Optional<String> reasonResult = reasonDialog.showAndWait();
            if (reasonResult.isEmpty()) {
                return;
            }
            reason = reasonResult.get();
        }
        if (action == ModerationAction.TEMP_BAN) {
            TextInputDialog daysDialog = new TextInputDialog("3");
            daysDialog.setTitle("Durée du ban");
            daysDialog.setHeaderText("Nombre de jours de suspension");
            daysDialog.setContentText("Jours:");
            Optional<String> daysResult = daysDialog.showAndWait();
            if (daysResult.isEmpty()) {
                return;
            }
            try {
                days = Integer.parseInt(daysResult.get().trim());
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Nombre de jours invalide.").showAndWait();
                return;
            }
        }
        if ((action == ModerationAction.PERMANENT_BAN || action == ModerationAction.TEMP_BAN) && incident.userId() == null) {
            new Alert(Alert.AlertType.WARNING, "Utilisateur introuvable pour appliquer un ban.").showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer l'action " + action.name() + " ?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> confirmResult = confirm.showAndWait();
        if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.YES) {
            return;
        }
        try {
            moderationService.applyAction(
                    incident.id(),
                    incident.userId(),
                    adminId,
                    incident.userEmail(),
                    incident.userName(),
                    action,
                    reason,
                    days
            );
            refreshAll();
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Action modération impossible : " + ex.getMessage()).showAndWait();
        }
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

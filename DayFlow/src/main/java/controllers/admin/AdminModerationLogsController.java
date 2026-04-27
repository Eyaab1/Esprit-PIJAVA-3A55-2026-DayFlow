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
import model.user.User;
import services.admin.AdminModerationService;
import services.admin.AdminModerationService.ModerationAction;
import services.admin.AdminModerationService.ModerationIncidentRow;
import session.AppSession;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminModerationLogsController {

    private final AdminModerationService moderationService = new AdminModerationService();

    @FXML
    private VBox moderationLogsBox;

    @FXML
    private void initialize() {
        refresh();
    }

    private void refresh() {
        moderationLogsBox.getChildren().clear();
        try {
            List<ModerationIncidentRow> rows = moderationService.findRecentIncidents(100);
            if (rows.isEmpty()) {
                moderationLogsBox.getChildren().add(new Label("Aucun incident de modération pour le moment."));
                return;
            }
            for (ModerationIncidentRow row : rows) {
                moderationLogsBox.getChildren().add(buildModerationRow(row));
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Chargement des logs de modération impossible : " + e.getMessage()).showAndWait();
        }
    }

    private VBox buildModerationRow(ModerationIncidentRow r) {
        VBox card = new VBox(8);
        card.getStyleClass().add("admin-list-row");

        String when = r.createdAt() != null ? r.createdAt().toString().replace('T', ' ') : "—";
        String content = r.contentText() == null || r.contentText().isBlank() ? "—" : truncate(r.contentText(), 280);
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
                        + " • Incident: " + (r.incidentStatus() == null ? "—" : r.incidentStatus())
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
        Button warnBtn = new Button("Warning");
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(8, ignoreBtn, warnBtn, tempBanBtn, permBanBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(header, meta, reason, body, actions, spacer);
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
            refresh();
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
}

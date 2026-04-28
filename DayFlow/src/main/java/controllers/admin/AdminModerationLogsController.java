package controllers.admin;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.admin.ModerationActionService;
import services.admin.ModerationLogFileService;
import services.admin.ModerationLogFileService.ModerationLogEntry;
import session.AppSession;
import model.user.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AdminModerationLogsController {

    private static final double COL_TIMESTAMP = 160;
    private static final double COL_EMAIL = 180;
    private static final double COL_TYPE = 120;
    private static final double COL_CONTENT = 250;
    private static final double COL_ATTRIBUTE = 140;
    private static final double COL_SCORE = 100;
    private static final double COL_ACTION = 80;

    private final ModerationLogFileService logFileService = new ModerationLogFileService();
    private final ModerationActionService actionService = new ModerationActionService();

    @FXML
    private VBox moderationRowsBox;

    @FXML
    private ScrollPane moderationRowsScrollPane;
    
    @FXML
    private javafx.scene.control.ComboBox<String> statusFilterCombo;
    
    private String currentStatusFilter = "Tous";

    @FXML
    private void initialize() {
        // Initialize status filter combo
        if (statusFilterCombo != null) {
            statusFilterCombo.getItems().addAll("Tous", "NOT_VIEWED", "VIEWED", "ACTION_DONE");
            statusFilterCombo.setValue("Tous");
            statusFilterCombo.setOnAction(e -> {
                currentStatusFilter = statusFilterCombo.getValue();
                refresh();
            });
        }
        refresh();
    }

    private void refresh() {
        moderationRowsBox.getChildren().clear();
        try {
            List<ModerationLogEntry> logs = logFileService.readRecentLogsWithFilter(100, currentStatusFilter);
            if (logs.isEmpty()) {
                Label emptyLabel = new Label("Aucun incident de modération pour le moment.");
                emptyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-padding: 40;");
                moderationRowsBox.getChildren().add(emptyLabel);
                return;
            }
            for (ModerationLogEntry log : logs) {
                moderationRowsBox.getChildren().add(buildModerationRow(log));
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Chargement des logs de modération impossible : " + e.getMessage()).showAndWait();
        }
    }

    private HBox buildModerationRow(ModerationLogEntry log) {
        Label timestamp = new Label(log.timestamp() != null ? log.timestamp().replace('T', ' ') : "—");
        setFixedWidth(timestamp, COL_TIMESTAMP);
        timestamp.setStyle("-fx-font-size: 12px;");

        Label email = new Label(log.userEmail() != null ? log.userEmail() : "—");
        setFixedWidth(email, COL_EMAIL);
        email.setStyle("-fx-font-size: 12px;");

        Label entityType = new Label(log.entityType() != null ? log.entityType() : "—");
        setFixedWidth(entityType, COL_TYPE);
        entityType.setStyle("-fx-font-size: 12px;");

        Label content = new Label(log.contentPreview() != null ? truncate(log.contentPreview(), 50) : "—");
        setFixedWidth(content, COL_CONTENT);
        content.setStyle("-fx-font-size: 12px;");
        content.setWrapText(true);

        Label attribute = new Label(log.highestAttribute() != null ? log.highestAttribute() : "—");
        setFixedWidth(attribute, COL_ATTRIBUTE);
        attribute.setStyle("-fx-font-size: 12px; -fx-text-fill: #7c3aed; -fx-font-weight: bold;");

        Label score = new Label(log.highestScore() != null ? String.format("%.2f", log.highestScore()) : "—");
        setFixedWidth(score, COL_SCORE);
        score.setStyle("-fx-font-size: 12px; -fx-text-fill: #dc2626; -fx-font-weight: bold;");
        
        // Status badge
        Label statusBadge = createStatusBadge(log.getStatusOrDefault());
        setFixedWidth(statusBadge, 120);

        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER_LEFT);
        setFixedWidth(actionBox, COL_ACTION);

        Button viewBtn = new Button("View");
        viewBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 8;");
        viewBtn.setOnMouseEntered(e -> viewBtn.setStyle("-fx-background-color: #6d28d9; -fx-text-fill: white; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 8;"));
        viewBtn.setOnMouseExited(e -> viewBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 8;"));
        viewBtn.setOnAction(e -> {
            // Update status to VIEWED when clicking View
            updateLogStatus(log, "VIEWED");
            showDetailsModal(log);
        });

        actionBox.getChildren().add(viewBtn);

        HBox row = new HBox(12, timestamp, email, entityType, content, attribute, score, statusBadge, actionBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 10, 12, 10));
        row.getStyleClass().add("admin-post-row");
        return row;
    }
    
    private Label createStatusBadge(String status) {
        Label badge = new Label();
        badge.setAlignment(Pos.CENTER);
        badge.setStyle("-fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        switch (status) {
            case "NOT_VIEWED":
                badge.setText("Non vu");
                badge.setStyle(badge.getStyle() + " -fx-background-color: #fef3c7; -fx-text-fill: #92400e;");
                break;
            case "VIEWED":
                badge.setText("Vu");
                badge.setStyle(badge.getStyle() + " -fx-background-color: #dbeafe; -fx-text-fill: #1e40af;");
                break;
            case "ACTION_DONE":
                badge.setText("Action effectuée");
                badge.setStyle(badge.getStyle() + " -fx-background-color: #d1fae5; -fx-text-fill: #065f46;");
                break;
            default:
                badge.setText("Non vu");
                badge.setStyle(badge.getStyle() + " -fx-background-color: #fef3c7; -fx-text-fill: #92400e;");
        }
        
        return badge;
    }
    
    private void updateLogStatus(ModerationLogEntry log, String newStatus) {
        try {
            logFileService.updateLogStatus(log.timestamp(), newStatus);
        } catch (IOException e) {
            System.err.println("Failed to update log status: " + e.getMessage());
        }
    }

    private void showBanDialog(ModerationLogEntry log, ModerationActionService.BanType banType) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Ban Duration");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        Label title = new Label(banType == ModerationActionService.BanType.POSTING_BAN ? 
            "Ban from Posting" : "Ban Account");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label instruction = new Label("Enter ban duration in days:");
        instruction.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        Spinner<Integer> daysSpinner = new Spinner<>();
        daysSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 7));
        daysSpinner.setPrefWidth(100);
        daysSpinner.setStyle("-fx-font-size: 12px;");

        HBox spinnerBox = new HBox(daysSpinner);
        spinnerBox.setAlignment(Pos.CENTER_LEFT);

        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        Button confirmBtn = new Button("Confirm");
        confirmBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-padding: 10 24; -fx-cursor: hand; -fx-font-weight: bold;");
        confirmBtn.setOnAction(e -> {
            int days = daysSpinner.getValue();
            applyBan(log, banType, days);
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b; -fx-padding: 10 24; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        buttonBox.getChildren().addAll(confirmBtn, cancelBtn);
        content.getChildren().addAll(title, instruction, spinnerBox, buttonBox);

        Scene scene = new Scene(content, 350, 200);
        dialog.setScene(scene);
        dialog.show();
    }

    private void applyBan(ModerationLogEntry log, ModerationActionService.BanType banType, int days) {
        try {
            Integer adminId = AppSession.getCurrentUser().map(User::getId).orElse(null);
            String reason = "Violation des règles de modération (contenu toxique/profane)";

            if (banType == ModerationActionService.BanType.POSTING_BAN) {
                actionService.applyPostingBan(0, log.userId(), adminId, log.userEmail(), 
                    log.userEmail(), days, reason);
                new Alert(Alert.AlertType.INFORMATION, "User banned from posting for " + days + " days.").showAndWait();
            } else {
                actionService.applyAccountBan(0, log.userId(), adminId, log.userEmail(), 
                    log.userEmail(), days, reason);
                new Alert(Alert.AlertType.INFORMATION, "User account banned for " + days + " days.").showAndWait();
            }
            
            // Update log status to ACTION_DONE
            updateLogStatus(log, "ACTION_DONE");
            
            refresh();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Ban action failed: " + e.getMessage()).showAndWait();
        }
    }

    private static void setFixedWidth(Region region, double width) {
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }

    private void showDetailsModal(ModerationLogEntry log) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Moderation Log Details");

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: white;");

        Label title = new Label("Moderation Incident Details");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        VBox details = new VBox(12);
        details.setStyle("-fx-padding: 16; -fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        Label basicInfoTitle = new Label("Basic Information");
        basicInfoTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-padding: 0 0 8 0;");
        details.getChildren().add(basicInfoTitle);

        details.getChildren().addAll(
                createDetailRow("Timestamp:", log.timestamp()),
                createDetailRow("User ID:", log.userId() != null ? log.userId().toString() : "—"),
                createDetailRow("User Email:", log.userEmail()),
                createDetailRow("Entity Type:", log.entityType()),
                createDetailRow("Source:", log.source()),
                createDetailRow("Analyzed At:", log.analyzedAt())
        );

        Label contentTitle = new Label("Content");
        contentTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-padding: 12 0 8 0;");
        details.getChildren().add(contentTitle);

        Label contentValue = new Label(log.contentPreview() != null ? log.contentPreview() : "—");
        contentValue.setWrapText(true);
        contentValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155; -fx-padding: 8; -fx-background-color: white; -fx-background-radius: 4; -fx-border-color: #cbd5e1; -fx-border-radius: 4;");
        details.getChildren().add(contentValue);

        Label detectionTitle = new Label("Detection Results");
        detectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-padding: 12 0 8 0;");
        details.getChildren().add(detectionTitle);

        details.getChildren().addAll(
                createDetailRow("Highest Attribute:", log.highestAttribute()),
                createDetailRow("Highest Score:", log.highestScore() != null ? String.format("%.4f", log.highestScore()) : "—"),
                createDetailRow("Threshold Used:", log.thresholdUsed() != null ? String.format("%.2f", log.thresholdUsed()) : "—")
        );

        if (log.flaggedAttributes() != null && !log.flaggedAttributes().isEmpty()) {
            Label flagsLabel = new Label("Flagged Attributes");
            flagsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-padding: 12 0 8 0;");
            details.getChildren().add(flagsLabel);

            Label flagsValue = new Label(String.join(", ", log.flaggedAttributes()));
            flagsValue.setWrapText(true);
            flagsValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #fef2f2; -fx-background-radius: 4; -fx-border-color: #fecaca; -fx-border-radius: 4;");
            details.getChildren().add(flagsValue);
        }

        if (log.toxicityScores() != null && !log.toxicityScores().isEmpty()) {
            Label scoresLabel = new Label("Toxicity Scores");
            scoresLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-padding: 12 0 8 0;");
            details.getChildren().add(scoresLabel);

            VBox scoresBox = new VBox(6);
            scoresBox.setStyle("-fx-padding: 8; -fx-background-color: white; -fx-background-radius: 4; -fx-border-color: #cbd5e1; -fx-border-radius: 4;");
            
            for (Map.Entry<String, Double> entry : log.toxicityScores().entrySet()) {
                HBox scoreRow = new HBox(12);
                scoreRow.setAlignment(Pos.CENTER_LEFT);
                
                Label attrLabel = new Label(entry.getKey() + ":");
                attrLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569; -fx-min-width: 160;");
                
                Label scoreValue = new Label(String.format("%.4f", entry.getValue()));
                scoreValue.setStyle("-fx-font-size: 12px; -fx-font-family: monospace; -fx-text-fill: #1e293b;");
                
                scoreRow.getChildren().addAll(attrLabel, scoreValue);
                scoresBox.getChildren().add(scoreRow);
            }
            details.getChildren().add(scoresBox);
        }

        ScrollPane scrollPane = new ScrollPane(details);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white;");
        scrollPane.setMaxHeight(500);

        // Action buttons at the bottom
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(16, 0, 0, 0));

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 10 32; -fx-background-color: #64748b; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
        closeBtn.setOnAction(e -> modal.close());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-padding: 10 32; -fx-background-color: #475569; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-padding: 10 32; -fx-background-color: #64748b; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;"));

        Button banPostingBtn = new Button("Ban from Posting");
        banPostingBtn.setStyle("-fx-padding: 10 32; -fx-background-color: #ea580c; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
        banPostingBtn.setOnAction(e -> {
            modal.close();
            showBanDialog(log, ModerationActionService.BanType.POSTING_BAN);
        });
        banPostingBtn.setOnMouseEntered(e -> banPostingBtn.setStyle("-fx-padding: 10 32; -fx-background-color: #c2410c; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;"));
        banPostingBtn.setOnMouseExited(e -> banPostingBtn.setStyle("-fx-padding: 10 32; -fx-background-color: #ea580c; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;"));

        Button banAccountBtn = new Button("Ban Account");
        banAccountBtn.setStyle("-fx-padding: 10 32; -fx-background-color: #b91c1c; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
        banAccountBtn.setOnAction(e -> {
            modal.close();
            showBanDialog(log, ModerationActionService.BanType.ACCOUNT_BAN);
        });
        banAccountBtn.setOnMouseEntered(e -> banAccountBtn.setStyle("-fx-padding: 10 32; -fx-background-color: #7f1d1d; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;"));
        banAccountBtn.setOnMouseExited(e -> banAccountBtn.setStyle("-fx-padding: 10 32; -fx-background-color: #b91c1c; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;"));

        buttonBox.getChildren().addAll(closeBtn, banPostingBtn, banAccountBtn);

        content.getChildren().addAll(title, scrollPane, buttonBox);

        Scene scene = new Scene(content, 700, 650);
        modal.setScene(scene);
        modal.show();
    }

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #475569; -fx-min-width: 140;");

        Label valueNode = new Label(value != null ? value : "—");
        valueNode.setWrapText(true);
        valueNode.setStyle("-fx-font-size: 12px; -fx-text-fill: #1e293b;");

        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }
}

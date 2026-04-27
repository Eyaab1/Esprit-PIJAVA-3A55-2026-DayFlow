package controllers.admin;

import enums.ReclamationStatus;
import enums.ReclamationType;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.reclamation.Reclamation;
import model.reclamation.Response;
import services.ai.GroqAIService;
import services.reclamation_services.ReclamationService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Interface admin pour gérer les réclamations : recherche, filtres, réponses.
 */
public class AdminReclamationsController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);
    private static final int PAGE_SIZE = 50;

    private final ReclamationService reclamationService = new ReclamationService();
    private final GroqAIService groqAIService = new GroqAIService();

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<ReclamationStatus> statusFilter;
    @FXML
    private ComboBox<ReclamationType> typeFilter;
    @FXML
    private VBox reclamationsListPane;
    @FXML
    private Label totalCountLabel;

    @FXML
    private void initialize() {
        setupFilters();
        onFilter();
    }

    private void setupFilters() {
        // Status filter
        statusFilter.getItems().add(null); // "Tous"
        statusFilter.getItems().addAll(ReclamationStatus.values());
        statusFilter.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(ReclamationStatus s) {
                return s == null ? "Tous les statuts" : statusLabelFr(s);
            }

            @Override
            public ReclamationStatus fromString(String s) {
                return null;
            }
        });
        statusFilter.getSelectionModel().select(0);

        // Type filter
        typeFilter.getItems().add(null); // "Tous"
        typeFilter.getItems().addAll(ReclamationType.values());
        typeFilter.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(ReclamationType t) {
                return t == null ? "Tous les types" : typeLabelFr(t);
            }

            @Override
            public ReclamationType fromString(String s) {
                return null;
            }
        });
        typeFilter.getSelectionModel().select(0);
    }

    @FXML
    private void onFilter() {
        String search = searchField != null && searchField.getText() != null
                ? searchField.getText().trim()
                : "";
        ReclamationStatus status = statusFilter != null ? statusFilter.getValue() : null;
        ReclamationType type = typeFilter != null ? typeFilter.getValue() : null;

        try {
            List<Reclamation> list = reclamationService.findForAdmin(status, type, search, PAGE_SIZE, 0);
            int total = reclamationService.countForAdmin(status, type, search);
            renderReclamations(list);
            totalCountLabel.setText(total + " réclamation" + (total > 1 ? "s" : ""));
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur de chargement : " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onClearFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        if (statusFilter != null) {
            statusFilter.getSelectionModel().select(0);
        }
        if (typeFilter != null) {
            typeFilter.getSelectionModel().select(0);
        }
        onFilter();
    }

    private void renderReclamations(List<Reclamation> list) {
        reclamationsListPane.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("Aucune réclamation trouvée.");
            empty.setStyle("-fx-text-fill:#64748b;-fx-font-size:14px;-fx-padding:20 0;");
            reclamationsListPane.getChildren().add(empty);
            return;
        }
        for (Reclamation r : list) {
            reclamationsListPane.getChildren().add(buildReclamationRow(r));
        }
    }

    private HBox buildReclamationRow(Reclamation r) {
        VBox left = new VBox(6);

        // Title with ID
        Label title = new Label("Réclamation #" + r.getId());
        title.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:#1e1b4b;");

        // User info (if available)
        String userInfo = r.getUserId() != null ? "Utilisateur ID: " + r.getUserId() : "Utilisateur inconnu";
        Label user = new Label("👤 " + userInfo);
        user.setStyle("-fx-text-fill:#64748b;-fx-font-size:12px;");

        // Content preview
        String contentPreview = r.getContent() != null
                ? truncate(stripHtmlForDisplay(r.getContent()), 120)
                : "—";
        Label content = new Label(contentPreview);
        content.setStyle("-fx-text-fill:#475569;-fx-font-size:13px;");
        content.setWrapText(true);

        // Date
        String when = r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "—";
        Label date = new Label("📅 " + when);
        date.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11px;");

        left.getChildren().addAll(title, user, content, date);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right side: badges and actions
        VBox right = new VBox(8);
        right.setAlignment(Pos.TOP_RIGHT);

        // Status badge
        Label statusBadge = new Label(statusLabelFr(r.getStatus()));
        statusBadge.getStyleClass().addAll("admin-badge", statusStyleClass(r.getStatus()));

        // Type badge
        Label typeBadge = new Label(typeLabelFr(r.getType()));
        typeBadge.getStyleClass().addAll("admin-badge", "badge-default");

        // Action buttons
        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button viewBtn = new Button("👁 Voir");
        viewBtn.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-font-size:11px;" +
                "-fx-padding:6 12;-fx-background-radius:8px;-fx-cursor:hand;");
        viewBtn.setOnAction(e -> showReclamationDetail(r.getId()));

        Button replyBtn = new Button("✉ Répondre");
        replyBtn.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-size:11px;" +
                "-fx-padding:6 12;-fx-background-radius:8px;-fx-cursor:hand;");
        replyBtn.setOnAction(e -> showReplyDialog(r.getId()));

        actions.getChildren().addAll(viewBtn, replyBtn);

        right.getChildren().addAll(statusBadge, typeBadge, actions);

        HBox row = new HBox(16, left, spacer, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 12, 14, 12));
        row.getStyleClass().add("admin-list-row");
        return row;
    }

    private void showReclamationDetail(int reclamationId) {
        try {
            Optional<Reclamation> opt = reclamationService.findByIdWithResponses(reclamationId);
            if (opt.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Réclamation introuvable.").showAndWait();
                return;
            }
            Reclamation rec = opt.get();

            StringBuilder sb = new StringBuilder();
            sb.append("═══ INFORMATIONS ═══\n\n");
            sb.append("ID: ").append(rec.getId()).append("\n");
            sb.append("Type: ").append(typeLabelFr(rec.getType())).append("\n");
            sb.append("Statut: ").append(statusLabelFr(rec.getStatus())).append("\n");
            sb.append("Utilisateur ID: ").append(rec.getUserId() != null ? rec.getUserId() : "—").append("\n");
            sb.append("Date: ").append(rec.getCreatedAt() != null ? rec.getCreatedAt().format(DATE_FMT) : "—").append("\n");
            
            // Show photo path if exists
            if (rec.getPhotoPath() != null && !rec.getPhotoPath().isBlank()) {
                sb.append("Preuve jointe: ").append(rec.getPhotoPath()).append("\n");
            }
            sb.append("\n");

            sb.append("═══ CONTENU ═══\n\n");
            sb.append(stripHtmlForDisplay(rec.getContent())).append("\n\n");

            sb.append("═══ RÉPONSES (").append(rec.getResponses().size()).append(") ═══\n\n");
            List<Response> responses = rec.getResponses();
            if (responses.isEmpty()) {
                sb.append("(Aucune réponse pour l'instant.)\n");
            } else {
                int i = 1;
                for (Response resp : responses) {
                    sb.append("─── Réponse ").append(i++).append(" ───\n");
                    if (resp.getCreatedAt() != null) {
                        sb.append("Date: ").append(resp.getCreatedAt().format(DATE_FMT)).append("\n");
                    }
                    sb.append(stripHtmlForDisplay(resp.getContent())).append("\n\n");
                }
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Détail de la réclamation");
            alert.setHeaderText("Réclamation #" + rec.getId());
            
            VBox content = new VBox(10);
            
            TextArea area = new TextArea(sb.toString());
            area.setEditable(false);
            area.setWrapText(true);
            area.setPrefRowCount(18);
            area.setMaxWidth(Double.MAX_VALUE);
            
            content.getChildren().add(area);
            
            // Show image if exists
            if (rec.getPhotoPath() != null && !rec.getPhotoPath().isBlank()) {
                try {
                    java.io.File imageFile = new java.io.File(rec.getPhotoPath());
                    if (imageFile.exists()) {
                        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
                        javafx.scene.image.Image image = new javafx.scene.image.Image(imageFile.toURI().toString());
                        imageView.setImage(image);
                        imageView.setFitWidth(400);
                        imageView.setPreserveRatio(true);
                        
                        Label imageLabel = new Label("📎 Preuve jointe :");
                        imageLabel.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");
                        
                        content.getChildren().addAll(imageLabel, imageView);
                    }
                } catch (Exception e) {
                    Label errorLabel = new Label("⚠ Impossible de charger l'image");
                    errorLabel.setStyle("-fx-text-fill:#dc2626;");
                    content.getChildren().add(errorLabel);
                }
            }
            
            alert.getDialogPane().setContent(content);
            alert.getDialogPane().setPrefWidth(650);
            alert.showAndWait();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void showReplyDialog(int reclamationId) {
        try {
            Optional<Reclamation> opt = reclamationService.findById(reclamationId);
            if (opt.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Réclamation introuvable.").showAndWait();
                return;
            }
            Reclamation reclamation = opt.get();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Répondre à la réclamation");
            dialog.setHeaderText("Réclamation #" + reclamationId);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            TextArea replyArea = new TextArea();
            replyArea.setPromptText("Votre réponse (min. 5 caractères)...");
            replyArea.setPrefRowCount(8);
            replyArea.setWrapText(true);

            // AI Suggestion Button
            Button aiSuggestBtn = new Button("✨ Suggérer une réponse (IA)");
            aiSuggestBtn.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-font-weight:bold;" +
                    "-fx-padding:8 16;-fx-background-radius:8px;-fx-cursor:hand;");
            
            Label aiStatusLabel = new Label("");
            aiStatusLabel.setStyle("-fx-text-fill:#64748b;-fx-font-size:12px;-fx-font-style:italic;");

            aiSuggestBtn.setOnAction(e -> {
                aiSuggestBtn.setDisable(true);
                aiStatusLabel.setText("⏳ Génération en cours...");
                aiStatusLabel.setStyle("-fx-text-fill:#7c3aed;-fx-font-size:12px;-fx-font-style:italic;");
                
                // Run in background thread to avoid blocking UI
                new Thread(() -> {
                    try {
                        String suggestion = groqAIService.generateResponseSuggestion(reclamation);
                        // Update UI on JavaFX thread
                        javafx.application.Platform.runLater(() -> {
                            replyArea.setText(suggestion);
                            aiStatusLabel.setText("✅ Suggestion générée ! Vous pouvez la modifier.");
                            aiStatusLabel.setStyle("-fx-text-fill:#16a34a;-fx-font-size:12px;-fx-font-style:italic;");
                            aiSuggestBtn.setDisable(false);
                        });
                    } catch (IOException | InterruptedException ex) {
                        javafx.application.Platform.runLater(() -> {
                            aiStatusLabel.setText("❌ Erreur : " + ex.getMessage());
                            aiStatusLabel.setStyle("-fx-text-fill:#dc2626;-fx-font-size:12px;-fx-font-style:italic;");
                            aiSuggestBtn.setDisable(false);
                        });
                    }
                }).start();
            });

            // Clear button
            Button clearBtn = new Button("🗑 Effacer");
            clearBtn.setStyle("-fx-background-color:#e9d5ff;-fx-text-fill:#5b21b6;-fx-font-weight:bold;" +
                    "-fx-padding:8 16;-fx-background-radius:8px;-fx-cursor:hand;");
            clearBtn.setOnAction(e -> {
                replyArea.clear();
                aiStatusLabel.setText("");
            });

            HBox aiButtonsBox = new HBox(10, aiSuggestBtn, clearBtn);
            aiButtonsBox.setAlignment(Pos.CENTER_LEFT);

            VBox content = new VBox(10);
            content.setPadding(new Insets(10));
            content.getChildren().addAll(
                    new Label("Réponse :"),
                    aiButtonsBox,
                    aiStatusLabel,
                    replyArea,
                    new Label("Note : Le statut sera automatiquement changé à « Répondu ».")
            );
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefWidth(550);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }

            String replyContent = replyArea.getText() != null ? replyArea.getText().trim() : "";
            if (replyContent.length() < 5) {
                new Alert(Alert.AlertType.WARNING, "La réponse doit contenir au moins 5 caractères.").showAndWait();
                return;
            }

            reclamationService.addAdminReply(reclamationId, replyContent, null);
            new Alert(Alert.AlertType.INFORMATION, "Réponse envoyée avec succès.").showAndWait();
            onFilter(); // Refresh list

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private static String statusLabelFr(ReclamationStatus s) {
        if (s == null) {
            return "—";
        }
        return switch (s) {
            case PENDING -> "En attente";
            case IN_PROGRESS -> "En cours";
            case ANSWERED -> "Répondu";
            case RESOLVED -> "Résolu";
            case REJECTED -> "Rejeté";
        };
    }

    private static String statusStyleClass(ReclamationStatus s) {
        if (s == null) {
            return "badge-default";
        }
        return switch (s) {
            case PENDING -> "badge-pending";
            case IN_PROGRESS -> "badge-scheduling";
            case ANSWERED -> "badge-accepted";
            case RESOLVED -> "badge-accepted";
            case REJECTED -> "badge-default";
        };
    }

    private static String typeLabelFr(ReclamationType t) {
        if (t == null) {
            return "—";
        }
        return switch (t) {
            case ACCOUNT -> "Compte";
            case BUG -> "Bug";
            case COACHING -> "Coaching";
            case PAYMENT -> "Paiement";
            case OTHER -> "Autre";
        };
    }

    private static String stripHtmlForDisplay(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw
                .replaceAll("(?i)<\\s*br\\s*/?>", "\n")
                .replaceAll("(?i)</p>\\s*", "\n")
                .replaceAll("(?i)<\\s*p[^>]*>", "");
        s = s.replaceAll("<[^>]+>", "");
        s = s.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"");
        return s.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }
}

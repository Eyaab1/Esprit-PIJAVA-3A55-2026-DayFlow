package controllers.reclamation;

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
import services.reclamation.ReclamationService;
import session.AppSession;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Liste des réclamations de l'utilisateur connecté.
 */
public class MesReclamationsController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);

    private final ReclamationService reclamationService = new ReclamationService();

    @FXML
    private VBox listContainer;
    @FXML
    private Button newReclamationBtn;

    @FXML
    private void initialize() {
        refreshList();
    }

    private void refreshList() {
        listContainer.getChildren().clear();
        Optional<Integer> uid = AppSession.getCurrentUser().map(u -> u.getId());
        if (uid.isEmpty()) {
            Label empty = new Label("Connectez-vous pour voir vos réclamations.");
            empty.getStyleClass().add("reclam-empty");
            listContainer.getChildren().add(empty);
            newReclamationBtn.setDisable(true);
            return;
        }
        newReclamationBtn.setDisable(false);
        try {
            List<Reclamation> list = reclamationService.findByUserId(uid.get(), 200, 0);
            if (list.isEmpty()) {
                Label empty = new Label("Vous n'avez pas encore de réclamation. Utilisez « Nouvelle réclamation ».");
                empty.getStyleClass().add("reclam-empty");
                empty.setWrapText(true);
                listContainer.getChildren().add(empty);
                return;
            }
            for (Reclamation r : list) {
                listContainer.getChildren().add(buildItem(r));
            }
        } catch (SQLException e) {
            Label err = new Label("Erreur de chargement : " + e.getMessage());
            err.getStyleClass().add("reclam-empty");
            listContainer.getChildren().add(err);
        }
    }

    private VBox buildItem(Reclamation r) {
        VBox card = new VBox(10);
        card.getStyleClass().add("reclam-item");

        Label statusBadge = new Label(statusLabelFr(r.getStatus()));
        statusBadge.getStyleClass().addAll("badge-status", statusStyleClass(r.getStatus()));

        Label typeBadge = new Label(typeLabelFr(r.getType()));
        typeBadge.getStyleClass().add("badge-type");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button detailBtn = new Button("👁  Voir détails");
        detailBtn.getStyleClass().add("btn-detail");
        detailBtn.setOnAction(e -> showDetail(r.getId()));

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getChildren().addAll(statusBadge, typeBadge, spacer, detailBtn);
        top.getStyleClass().add("reclam-item-top");

        String text = r.getContent() != null ? stripHtmlForDisplay(r.getContent()) : "";
        Label desc = new Label(text);
        desc.setWrapText(true);
        desc.getStyleClass().add("reclam-desc");

        String when = r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "";
        Label meta = new Label("📅  " + when);
        meta.getStyleClass().add("reclam-meta");

        card.getChildren().addAll(top, desc, meta);
        return card;
    }

    private void showDetail(int reclamationId) {
        try {
            Integer uid = AppSession.getCurrentUser().map(u -> u.getId()).orElse(null);
            if (uid == null || !reclamationService.belongsToUser(reclamationId, uid)) {
                new Alert(Alert.AlertType.WARNING, "Accès non autorisé.").showAndWait();
                return;
            }
            Optional<Reclamation> opt = reclamationService.findByIdWithResponses(reclamationId);
            if (opt.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Réclamation introuvable.").showAndWait();
                return;
            }
            Reclamation rec = opt.get();
            StringBuilder sb = new StringBuilder();
            sb.append(stripHtmlForDisplay(rec.getContent())).append("\n\n———\n\n");
            List<Response> responses = rec.getResponses();
            if (responses.isEmpty()) {
                sb.append("(Aucune réponse pour l'instant.)");
            } else {
                int i = 1;
                for (Response resp : responses) {
                    sb.append("Réponse ").append(i++).append(" — ");
                    if (resp.getCreatedAt() != null) {
                        sb.append(resp.getCreatedAt().format(DATE_FMT));
                    }
                    sb.append("\n");
                    sb.append(stripHtmlForDisplay(resp.getContent())).append("\n\n");
                }
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Détail de la réclamation");
            alert.setHeaderText("Réclamation #" + rec.getId());
            TextArea area = new TextArea(sb.toString());
            area.setEditable(false);
            area.setWrapText(true);
            area.setPrefRowCount(14);
            area.setMaxWidth(Double.MAX_VALUE);
            alert.getDialogPane().setContent(area);
            alert.getDialogPane().setPrefWidth(520);
            alert.showAndWait();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onNouvelleReclamation() {
        AppSession.getCurrentUser().map(u -> u.getId()).ifPresentOrElse(uid -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Nouvelle réclamation");
            dialog.setHeaderText("Décrivez votre demande (min. 10 caractères)");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            ComboBox<ReclamationType> typeCombo = new ComboBox<>();
            typeCombo.getItems().addAll(ReclamationType.values());
            typeCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(ReclamationType t) {
                    return t == null ? "" : typeLabelFr(t);
                }

                @Override
                public ReclamationType fromString(String s) {
                    return null;
                }
            });
            typeCombo.getSelectionModel().select(ReclamationType.OTHER);

            TextArea contentArea = new TextArea();
            contentArea.setPromptText("Votre message…");
            contentArea.setPrefRowCount(6);
            contentArea.setWrapText(true);

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(12);
            grid.setPadding(new Insets(10));
            grid.add(new Label("Type :"), 0, 0);
            grid.add(typeCombo, 1, 0);
            grid.add(new Label("Message :"), 0, 1);
            grid.add(contentArea, 1, 1);
            dialog.getDialogPane().setContent(grid);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
            try {
                Reclamation r = new Reclamation();
                r.setContent(contentArea.getText() != null ? contentArea.getText().trim() : "");
                r.setType(typeCombo.getValue());
                r.setUserId(uid);
                reclamationService.createForUserWithAutoAck(r, null);
                refreshList();
                new Alert(Alert.AlertType.INFORMATION, "Réclamation enregistrée.").showAndWait();
            } catch (IllegalArgumentException ex) {
                new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
            } catch (SQLException ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        }, () -> new Alert(Alert.AlertType.WARNING, "Session expirée.").showAndWait());
    }

    private static String statusLabelFr(ReclamationStatus s) {
        if (s == null) {
            return "";
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
            return "pending";
        }
        return switch (s) {
            case PENDING -> "pending";
            case IN_PROGRESS -> "in_progress";
            case ANSWERED -> "answered";
            case RESOLVED -> "resolved";
            case REJECTED -> "rejected";
        };
    }

    private static String typeLabelFr(ReclamationType t) {
        if (t == null) {
            return "";
        }
        return switch (t) {
            case ACCOUNT -> "Compte";
            case BUG -> "Bug";
            case COACHING -> "Coaching";
            case PAYMENT -> "Paiement";
            case OTHER -> "Autre";
        };
    }

    /** Contenu pouvant contenir du HTML (Symfony / éditeur riche). */
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
}

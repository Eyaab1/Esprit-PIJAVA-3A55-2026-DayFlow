package controllers.reclamation;

import enums.ReclamationStatus;
import enums.ReclamationType;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.reclamation.Reclamation;
import model.reclamation.Response;
import services.ai.PerspectiveAPIService;

import services.reclamation.ReclamationService;
import session.AppSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;


public class MesReclamationsController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);

    private final ReclamationService reclamationService = new ReclamationService();
    private final PerspectiveAPIService moderationService = new PerspectiveAPIService();

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

            // Image upload section
            final File[] selectedImageFile = {null};
            ImageView imagePreview = new ImageView();
            imagePreview.setFitWidth(150);
            imagePreview.setFitHeight(150);
            imagePreview.setPreserveRatio(true);
            imagePreview.setVisible(false);

            Button chooseImageBtn = new Button("📎 Joindre une preuve (image)");
            chooseImageBtn.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-padding:8 16;-fx-background-radius:8px;-fx-cursor:hand;");
            
            Label imageLabel = new Label("Aucune image sélectionnée");
            imageLabel.setStyle("-fx-text-fill:#64748b;-fx-font-size:12px;");

            Button removeImageBtn = new Button("✖ Supprimer");
            removeImageBtn.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;-fx-padding:6 12;-fx-background-radius:6px;-fx-cursor:hand;");
            removeImageBtn.setVisible(false);

            chooseImageBtn.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Choisir une image");
                fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                    new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
                );
                File file = fileChooser.showOpenDialog(dialog.getOwner());
                if (file != null) {
                    // Check file size (max 5MB)
                    if (file.length() > 5 * 1024 * 1024) {
                        new Alert(Alert.AlertType.WARNING, "L'image est trop grande (max 5 MB).").showAndWait();
                        return;
                    }
                    selectedImageFile[0] = file;
                    imageLabel.setText(file.getName() + " (" + formatFileSize(file.length()) + ")");
                    imageLabel.setStyle("-fx-text-fill:#16a34a;-fx-font-size:12px;-fx-font-weight:bold;");
                    
                    // Show preview
                    try {
                        Image image = new Image(file.toURI().toString());
                        imagePreview.setImage(image);
                        imagePreview.setVisible(true);
                        removeImageBtn.setVisible(true);
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.WARNING, "Impossible de charger l'image.").showAndWait();
                    }
                }
            });

            removeImageBtn.setOnAction(e -> {
                selectedImageFile[0] = null;
                imageLabel.setText("Aucune image sélectionnée");
                imageLabel.setStyle("-fx-text-fill:#64748b;-fx-font-size:12px;");
                imagePreview.setVisible(false);
                removeImageBtn.setVisible(false);
            });

            HBox imageButtonsBox = new HBox(10, chooseImageBtn, removeImageBtn);
            imageButtonsBox.setAlignment(Pos.CENTER_LEFT);

            VBox imageBox = new VBox(8, imageButtonsBox, imageLabel, imagePreview);
            imageBox.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(12);
            grid.setPadding(new Insets(10));
            grid.add(new Label("Type :"), 0, 0);
            grid.add(typeCombo, 1, 0);
            grid.add(new Label("Message :"), 0, 1);
            grid.add(contentArea, 1, 1);
            grid.add(new Label("Preuve :"), 0, 2);
            grid.add(imageBox, 1, 2);
            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().setPrefWidth(600);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
            try {
                String content = contentArea.getText() != null ? contentArea.getText().trim() : "";
                
                // Content moderation check
                if (moderationService.isConfigured()) {
                    try {
                        PerspectiveAPIService.ModerationResult moderationResult = 
                                moderationService.analyzeText(content);
                        
                        if (moderationResult.isHarmful()) {
                            Alert warningAlert = new Alert(Alert.AlertType.WARNING);
                            warningAlert.setTitle("Contenu inapproprié détecté");
                            warningAlert.setHeaderText("Votre message contient du contenu potentiellement inapproprié");
                            warningAlert.setContentText(
                                    moderationResult.getReason() + "\n\n" +
                                    "Score de toxicité : " + String.format("%.0f%%", moderationResult.getMaxScore() * 100) + "\n\n" +
                                    "Veuillez reformuler votre message de manière respectueuse."
                            );
                            
                            ButtonType editButton = new ButtonType("Modifier", ButtonBar.ButtonData.OK_DONE);
                            ButtonType submitAnywayButton = new ButtonType("Soumettre quand même", ButtonBar.ButtonData.OTHER);
                            ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
                            
                            warningAlert.getButtonTypes().setAll(editButton, submitAnywayButton, cancelButton);
                            
                            Optional<ButtonType> moderationChoice = warningAlert.showAndWait();
                            if (moderationChoice.isEmpty() || moderationChoice.get() == cancelButton) {
                                return; // Cancel submission
                            }
                            if (moderationChoice.get() == editButton) {
                                return; // Go back to edit
                            }
                            // If "Submit anyway", continue with submission
                        }
                    } catch (Exception e) {
                        System.err.println("Moderation check failed: " + e.getMessage());
                        // Continue with submission if moderation fails
                    }
                }
                
                Reclamation r = new Reclamation();
                r.setContent(content);
                r.setType(typeCombo.getValue());
                r.setUserId(uid);
                
                // Handle image upload
                if (selectedImageFile[0] != null) {
                    String savedPath = saveReclamationImage(selectedImageFile[0]);
                    r.setPhotoPath(savedPath);
                }
                
                reclamationService.createForUserWithAutoAck(r, null);
                refreshList();
                new Alert(Alert.AlertType.INFORMATION, "Réclamation enregistrée avec succès.").showAndWait();
            } catch (IllegalArgumentException ex) {
                new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
            } catch (SQLException | IOException ex) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + ex.getMessage()).showAndWait();
            }
        }, () -> new Alert(Alert.AlertType.WARNING, "Session expirée.").showAndWait());
    }

    /**
     * Saves the uploaded image to the uploads directory and returns the relative path.
     */
    private String saveReclamationImage(File imageFile) throws IOException {
        // Create uploads directory if it doesn't exist
        Path uploadsDir = Paths.get("uploads", "reclamations");
        Files.createDirectories(uploadsDir);
        
        // Generate unique filename
        String extension = getFileExtension(imageFile.getName());
        String uniqueFileName = "reclamation_" + UUID.randomUUID().toString() + extension;
        Path targetPath = uploadsDir.resolve(uniqueFileName);
        
        // Copy file to uploads directory
        Files.copy(imageFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path for database storage
        return "uploads/reclamations/" + uniqueFileName;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
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

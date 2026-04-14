package controllers.coach;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.coaching_session.CoachingRequest;
import model.coaching_session.Session;
import model.user.User;
import services.UserServices.UserService;
import services.coaching_session_module.SessionService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class AddSessionController implements Initializable {

    // Informations client
    @FXML private Label clientInfoLabel;
    @FXML private Label requestInfoLabel;

    // Formulaire
    @FXML private DatePicker sessionDatePicker;
    @FXML private TextField sessionTimeField;
    @FXML private ComboBox<Integer> durationCombo;
    @FXML private TextField objectiveField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField priceField;

    // Boutons
    @FXML private Button cancelBtn;
    @FXML private Button createBtn;

    // Services
    private final SessionService sessionService;
    private final UserService userService;

    // Données
    private CoachingRequest coachingRequest;
    private Runnable onSaved;

    public AddSessionController() {
        this.sessionService = new SessionService();
        this.userService = new UserService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupForm();
        setupButtons();
    }

    private void setupForm() {
        // Remplir les durées
        durationCombo.setItems(FXCollections.observableArrayList(
                30, 45, 60, 90, 120
        ));
        durationCombo.setValue(60);

        // Date par défaut: demain
        sessionDatePicker.setValue(LocalDate.now().plusDays(1));
    }

    private void setupButtons() {
        cancelBtn.setOnAction(event -> handleCancel());
        createBtn.setOnAction(event -> handleCreate());
    }

    /**
     * Définit la demande de coaching pour laquelle créer une session.
     */
    public void setCoachingRequest(CoachingRequest request) {
        this.coachingRequest = request;
        loadClientInfo();
    }

    public void setRequest(CoachingRequest request) {
        setCoachingRequest(request);
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    private void loadClientInfo() {
        if (coachingRequest == null) return;

        try {
            Optional<User> user = userService.findById(coachingRequest.getUserId());
            if (user.isPresent()) {
                User client = user.get();
                clientInfoLabel.setText("Client: " + client.getFirstName() + " " + client.getLastName() +
                        " (" + client.getEmail() + ")");
            } else {
                clientInfoLabel.setText("Client: User #" + coachingRequest.getUserId());
            }

            String message = coachingRequest.getMessage();
            if (message == null || message.isBlank()) {
                requestInfoLabel.setText("Message: (aucun)");
            } else {
                requestInfoLabel.setText("Message: " + (message.length() > 100 ? message.substring(0, 97) + "..." : message));
            }

            // Pré-remplir l'objectif si disponible
            if (coachingRequest.getGoal() != null && !coachingRequest.getGoal().isEmpty()) {
                objectiveField.setText(coachingRequest.getGoal());
            }

        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les informations du client");
            e.printStackTrace();
        }
    }

    private void handleCreate() {
        try {
            if (coachingRequest == null) {
                showWarning("Aucune demande de coaching liée à cette session.");
                return;
            }

            // Validation
            if (sessionDatePicker.getValue() == null) {
                showWarning("Veuillez sélectionner une date");
                return;
            }

            if (sessionTimeField.getText() == null || sessionTimeField.getText().trim().isEmpty()) {
                showWarning("Veuillez saisir une heure");
                return;
            }

            if (durationCombo.getValue() == null) {
                showWarning("Veuillez sélectionner une durée");
                return;
            }

            // Parser la date et l'heure
            LocalDate date = sessionDatePicker.getValue();
            String timeStr = sessionTimeField.getText().trim();
            LocalTime time;

            try {
                String[] parts = timeStr.split(":");
                if (parts.length != 2) {
                    showWarning("Format d'heure invalide. Utilisez HH:MM (ex: 14:30)");
                    return;
                }
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                time = LocalTime.of(hour, minute);
            } catch (Exception e) {
                showWarning("Format d'heure invalide. Utilisez HH:MM (ex: 14:30)");
                return;
            }

            LocalDateTime dateTime = LocalDateTime.of(date, time);
            Date scheduledAt = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());

            Session session = sessionService.findByCoachingRequestId(coachingRequest.getId());
            boolean isNew = false;
            if (session == null) {
                session = new Session();
                session.setCoachingRequestId(coachingRequest.getId());
                isNew = true;
            }
            System.out.println("[AddSessionController] save session requestId=" + coachingRequest.getId() + ", isNew=" + isNew);
            session.setScheduledAt(scheduledAt);
            session.setDuration(durationCombo.getValue());
            session.setStatus(Session.STATUS_CONFIRMED);

            if (objectiveField.getText() != null && !objectiveField.getText().trim().isEmpty()) {
                session.setObjective(objectiveField.getText().trim());
            }

            if (priceField.getText() != null && !priceField.getText().trim().isEmpty()) {
                try {
                    double price = Double.parseDouble(priceField.getText().trim());
                    session.setPrice(price);
                } catch (NumberFormatException e) {
                    showWarning("Prix invalide");
                    return;
                }
            }

            session.setPaymentStatus(Session.PAYMENT_STATUS_PENDING);
            session.setUpdatedAt(new Date());
            if (isNew) {
                session.setCreatedAt(new Date());
            }

            if (isNew) {
                sessionService.addSession(session);
            } else {
                sessionService.updateSession(session);
            }

            showSuccess(isNew ? "Session créée avec succès" : "Session mise à jour avec succès");
            closeCurrentWindow();

        } catch (SQLException e) {
            showError("Erreur lors de la création", e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleCancel() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler");
        confirm.setHeaderText("Annuler la création de session ?");
        confirm.setContentText("Les données saisies seront perdues.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            closeCurrentWindow();
        }
    }

    private void closeCurrentWindow() {
        if (onSaved != null) {
            onSaved.run();
        }
        Stage stage = (Stage) createBtn.getScene().getWindow();
        stage.close();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

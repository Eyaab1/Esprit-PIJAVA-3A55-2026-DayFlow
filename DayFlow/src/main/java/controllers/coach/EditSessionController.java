package controllers.coach;

import controllers.navigation.NavigationManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import model.coaching_session.Session;
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

public class EditSessionController implements Initializable {

    @FXML private Label sessionInfoLabel;
    @FXML private DatePicker sessionDatePicker;
    @FXML private TextField sessionTimeField;
    @FXML private ComboBox<Integer> durationCombo;
    @FXML private TextField objectiveField;
    @FXML private TextField priceField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    private final SessionService sessionService;
    private Session session;

    public EditSessionController() {
        this.sessionService = new SessionService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupForm();
        setupButtons();
    }

    private void setupForm() {
        durationCombo.setItems(FXCollections.observableArrayList(30, 45, 60, 90, 120));
        statusCombo.setItems(FXCollections.observableArrayList(
                Session.STATUS_CONFIRMED,
                Session.STATUS_COMPLETED,
                Session.STATUS_CANCELLED
        ));
    }

    private void setupButtons() {
        cancelBtn.setOnAction(event -> returnToSessions());
        saveBtn.setOnAction(event -> handleSave());
    }

    public void setSession(Session session) {
        this.session = session;
        loadSessionData();
    }

    private void loadSessionData() {
        if (session == null) return;

        sessionInfoLabel.setText("Session #" + session.getId());

        if (session.getScheduledAt() != null) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    session.getScheduledAt().toInstant(),
                    ZoneId.systemDefault()
            );
            sessionDatePicker.setValue(dateTime.toLocalDate());
            sessionTimeField.setText(String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute()));
        }

        if (session.getDuration() != null) {
            durationCombo.setValue(session.getDuration());
        }

        if (session.getObjective() != null) {
            objectiveField.setText(session.getObjective());
        }

        if (session.getPrice() != null) {
            priceField.setText(String.valueOf(session.getPrice()));
        }

        if (session.getStatus() != null) {
            statusCombo.setValue(session.getStatus());
        }
    }

    private void handleSave() {
        try {
            if (sessionDatePicker.getValue() == null) {
                showWarning("Veuillez sélectionner une date");
                return;
            }

            if (sessionDatePicker.getValue().isBefore(LocalDate.now())) {
                showWarning("La date ne peut pas être dans le passé.");
                return;
            }

            if (sessionTimeField.getText() == null || sessionTimeField.getText().trim().isEmpty()) {
                showWarning("Veuillez saisir une heure");
                return;
            }

            LocalDate date = sessionDatePicker.getValue();
            String timeStr = sessionTimeField.getText().trim();
            LocalTime time;

            try {
                String[] parts = timeStr.split(":");
                time = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            } catch (Exception e) {
                showWarning("Format d'heure invalide. Utilisez HH:MM");
                return;
            }

            LocalDateTime dateTime = LocalDateTime.of(date, time);
            if (dateTime.isBefore(LocalDateTime.now())) {
                showWarning("Le créneau (date + heure) ne doit pas être dans le passé.");
                return;
            }
            session.setScheduledAt(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));

            if (durationCombo.getValue() == null || durationCombo.getValue() <= 0) {
                showWarning("La durée doit être supérieure à 0.");
                return;
            }
            session.setDuration(durationCombo.getValue());

            if (objectiveField.getText() == null || objectiveField.getText().trim().isEmpty()) {
                showWarning("L'objectif est obligatoire.");
                return;
            }
            session.setObjective(objectiveField.getText().trim());

            if (priceField.getText() == null || priceField.getText().trim().isEmpty()) {
                showWarning("Le prix est obligatoire.");
                return;
            }
            double price = Double.parseDouble(priceField.getText().trim());
            if (price <= 0) {
                showWarning("Le prix doit être supérieur à 0.");
                return;
            }
            session.setPrice(price);

            if (statusCombo.getValue() != null) {
                session.setStatus(statusCombo.getValue());
            }

            sessionService.updateSession(session);
            showSuccess("Session mise à jour avec succès");
            returnToSessions();

        } catch (SQLException e) {
            showError("Erreur lors de la mise à jour", e.getMessage());
        } catch (NumberFormatException e) {
            showWarning("Prix invalide");
        }
    }

    private void returnToSessions() {
        try {
            NavigationManager.show("/user/coaching_session/mes_sessions.fxml", "Mes sessions");
        } catch (IOException e) {
            showError("Erreur de navigation", "Impossible de retourner à la liste");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            showError("Erreur de navigation", e.getMessage());
        }
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

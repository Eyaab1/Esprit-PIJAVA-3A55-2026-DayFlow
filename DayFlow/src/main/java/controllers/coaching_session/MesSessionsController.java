package controllers.coaching_session;

import controllers.navigation.NavigationManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.coaching_session.CoachingRequest;
import model.coaching_session.Session;
import model.user.User;
import services.account.UserService;
import services.coaching_session.CoachingRequestService;
import services.coaching_session.EvaluationService;
import services.coaching_session.ProgressService;
import services.coaching_session.SessionService;
import session.AppSession;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MesSessionsController implements Initializable {

    @FXML
    private TableView<Session> tableSessions;
    @FXML
    private TableColumn<Session, String> userColumn;
    @FXML
    private TableColumn<Session, String> dateColumn;
    @FXML
    private TableColumn<Session, String> timeColumn;
    @FXML
    private TableColumn<Session, String> durationColumn;
    @FXML
    private TableColumn<Session, String> descriptionColumn;
    @FXML
    private Label selectionLabel;

    private final SessionService sessionService = new SessionService();
    private final CoachingRequestService requestService = new CoachingRequestService();
    private final UserService userService = new UserService();
    private final EvaluationService evaluationService = new EvaluationService();
    private final ProgressService progressService = new ProgressService();
    private final ObservableList<Session> sessionRows = FXCollections.observableArrayList();

    private Session selectedSession;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadSessions();
    }

    private void setupTable() {
        userColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveUserName(cell.getValue())));
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue())));
        timeColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatTime(cell.getValue())));
        durationColumn.setCellValueFactory(cell -> {
            Integer duration = cell.getValue().getDuration();
            return new SimpleStringProperty(duration == null ? "-" : duration + " min");
        });
        descriptionColumn.setCellValueFactory(cell -> {
            String objective = cell.getValue().getObjective();
            return new SimpleStringProperty(objective == null || objective.isBlank() ? "-" : objective);
        });

        tableSessions.setItems(sessionRows);
        tableSessions.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedSession = newVal;
            if (newVal == null) {
                selectionLabel.setText("Aucune session sélectionnée");
            } else {
                selectionLabel.setText("Session #" + newVal.getId() + " sélectionnée");
            }
        });
    }

    private void loadSessions() {
        Integer coachId = AppSession.getCurrentUser().map(User::getId).orElse(null);
        if (coachId == null) {
            System.out.println("[MesSessionsController] coachId absent dans AppSession");
            sessionRows.clear();
            selectionLabel.setText("Aucune session pour le moment");
            return;
        }
        System.out.println("[MesSessionsController] chargement sessions pour coachId=" + coachId);
        try {
            List<Session> sessions = sessionService.getSessionsByCoach(coachId);
            sessionRows.setAll(sessions);
            System.out.println("[MesSessionsController] sessions chargées=" + sessions.size());
            selectionLabel.setText(sessions.isEmpty()
                    ? "Aucune session pour le moment"
                    : "Sessions chargées : " + sessions.size());
        } catch (SQLException e) {
            showError("Erreur de chargement", e.getMessage());
        }
    }

    @FXML
    private void refreshSessions() {
        loadSessions();
    }

    @FXML
    private void handleDelete() {
        if (selectedSession == null) {
            showWarning("Sélectionnez une session à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la session");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                sessionService.deleteSession(selectedSession.getId());
                selectedSession = null;
                loadSessions();
            } catch (SQLException e) {
                showError("Suppression impossible", e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedSession == null) {
            showWarning("Sélectionnez une session à modifier.");
            return;
        }
        try {
            EditSessionController controller = NavigationManager.showAndGetController(
                    "/user/coaching_session/edit_session.fxml",
                    "Modifier la session"
            );
            controller.setSession(selectedSession);
        } catch (IOException e) {
            showError("Navigation impossible", "Impossible d'ouvrir le formulaire de modification.");
        } catch (IllegalStateException e) {
            showError("Navigation impossible", e.getMessage());
        }
    }

    @FXML
    private void handleSubmitFeedback() {
        if (selectedSession == null) {
            showWarning("Sélectionnez une session.");
            return;
        }
        if (!Session.STATUS_COMPLETED.equals(selectedSession.getStatus())) {
            showWarning("Le feedback est disponible uniquement pour une session terminée.");
            return;
        }
        try {
            Optional<ProgressTrackingDialogs.UserFeedbackInput> inputOpt = ProgressTrackingDialogs.showUserFeedbackDialog();
            if (inputOpt.isEmpty()) {
                return;
            }
            ProgressTrackingDialogs.UserFeedbackInput input = inputOpt.get();
            evaluationService.submitUserFeedback(
                    selectedSession.getId(),
                    input.coachRating(),
                    input.feedback(),
                    input.comment()
            );
            progressService.processCompletedSession(selectedSession.getId());
            showSuccess("Feedback enregistré.");
        } catch (NumberFormatException e) {
            showWarning("La note doit être un nombre valide.");
        } catch (Exception e) {
            showError("Erreur feedback", e.getMessage());
        }
    }

    @FXML
    private void handleShowProgressReport() {
        if (selectedSession == null) {
            showWarning("Sélectionnez une session.");
            return;
        }
        try {
            ProgressTrackingDialogs.showProgressReportDialog(
                    progressService.generateProgressReport(selectedSession.getCoachingRequestId())
            );
        } catch (Exception e) {
            showError("Erreur rapport", e.getMessage());
        }
    }

    @FXML
    private void goToMesDemandes(ActionEvent event) { // event conservé pour compatibilité FXML
        try {
            NavigationManager.show("/user/coaching_session/mes_demandes.fxml", "Mes demandes");
        } catch (IOException e) {
            showError("Navigation impossible", "Impossible de revenir vers Mes demandes.");
        } catch (IllegalStateException e) {
            showError("Navigation impossible", e.getMessage());
        }
    }

    private String resolveUserName(Session session) {
        try {
            Optional<CoachingRequest> requestOpt = requestService.findById(session.getCoachingRequestId());
            if (requestOpt.isEmpty()) {
                return "Utilisateur inconnu";
            }
            Optional<User> userOpt = userService.findById(requestOpt.get().getUserId());
            return userOpt.map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                    .filter(name -> !name.isBlank())
                    .orElse("Utilisateur #" + requestOpt.get().getUserId());
        } catch (SQLException e) {
            return "Utilisateur inconnu";
        }
    }

    private static String formatDate(Session session) {
        if (session.getDisplayTime() == null) {
            return "-";
        }
        return new SimpleDateFormat("dd/MM/yyyy").format(session.getDisplayTime());
    }

    private static String formatTime(Session session) {
        if (session.getDisplayTime() == null) {
            return "-";
        }
        return new SimpleDateFormat("HH:mm").format(session.getDisplayTime());
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
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

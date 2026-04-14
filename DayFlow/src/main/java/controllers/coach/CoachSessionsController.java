package controllers.coach;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import services.coaching_session_module.CoachingRequestService;
import services.coaching_session_module.SessionService;
import session.AppSession;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CoachSessionsController implements Initializable {

    // Labels statistiques
    @FXML private Label totalSessionsLabel;
    @FXML private Label scheduledLabel;
    @FXML private Label completedLabel;

    // Barre d'actions
    @FXML private Button refreshBtn;
    @FXML private Label selectionLabel;

    // TableView
    @FXML private TableView<Session> sessionsTable;
    @FXML private TableColumn<Session, String> idColumn;
    @FXML private TableColumn<Session, String> clientColumn;
    @FXML private TableColumn<Session, String> dateColumn;
    @FXML private TableColumn<Session, String> timeColumn;
    @FXML private TableColumn<Session, String> durationColumn;
    @FXML private TableColumn<Session, String> objectiveColumn;
    @FXML private TableColumn<Session, String> statusColumn;
    @FXML private TableColumn<Session, String> priceColumn;

    // Boutons
    @FXML private Button modifyBtn;
    @FXML private Button deleteBtn;
    @FXML private Button completeBtn;

    // Services
    private final SessionService sessionService;
    private final CoachingRequestService requestService;
    private final UserService userService;

    // Données
    private final ObservableList<Session> sessionsList;
    private Session selectedSession;
    private int currentCoachId;

    public CoachSessionsController() {
        this.sessionService = new SessionService();
        this.requestService = new CoachingRequestService();
        this.userService = new UserService();
        this.sessionsList = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Récupérer l'ID du coach connecté
        currentCoachId = AppSession.getCurrentUser()
                .map(User::getId)
                .orElse(1);

        setupTableView();
        setupButtons();
        loadSessions();
        updateStatistics();
    }

    private void setupTableView() {
        // Configuration des colonnes
        idColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getId()))
        );

        clientColumn.setCellValueFactory(cellData -> {
            Session session = cellData.getValue();
            try {
                CoachingRequest request = requestService.findById(session.getCoachingRequestId()).orElse(null);
                if (request != null) {
                    Optional<User> user = userService.findById(request.getUserId());
                    if (user.isPresent()) {
                        User u = user.get();
                        return new SimpleStringProperty(u.getFirstName() + " " + u.getLastName());
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new SimpleStringProperty("Client inconnu");
        });

        dateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getScheduledAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                return new SimpleStringProperty(sdf.format(cellData.getValue().getScheduledAt()));
            }
            return new SimpleStringProperty("-");
        });

        timeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getScheduledAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                return new SimpleStringProperty(sdf.format(cellData.getValue().getScheduledAt()));
            }
            return new SimpleStringProperty("-");
        });

        durationColumn.setCellValueFactory(cellData -> {
            Integer duration = cellData.getValue().getDuration();
            return new SimpleStringProperty(duration != null ? duration + " min" : "-");
        });

        objectiveColumn.setCellValueFactory(cellData -> {
            String objective = cellData.getValue().getObjective();
            if (objective != null && !objective.isEmpty()) {
                return new SimpleStringProperty(objective.length() > 40 ? objective.substring(0, 37) + "..." : objective);
            }
            return new SimpleStringProperty("-");
        });

        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            return new SimpleStringProperty(formatStatus(status));
        });

        priceColumn.setCellValueFactory(cellData -> {
            Double price = cellData.getValue().getPrice();
            return new SimpleStringProperty(price != null ? String.format("%.2f €", price) : "-");
        });

        // Lier les données
        sessionsTable.setItems(sessionsList);

        // Gérer la sélection
        sessionsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    selectedSession = newValue;
                    updateSelectionLabel();
                }
        );
    }

    private void setupButtons() {
        refreshBtn.setOnAction(event -> {
            loadSessions();
            updateStatistics();
        });

        modifyBtn.setOnAction(event -> handleModify());
        deleteBtn.setOnAction(event -> handleDelete());
        completeBtn.setOnAction(event -> handleComplete());
    }

    private void loadSessions() {
        try {
            List<Session> sessions = sessionService.getSessionsByCoach(currentCoachId);
            sessionsList.clear();
            sessionsList.addAll(sessions);
        } catch (SQLException e) {
            showError("Erreur lors du chargement des sessions", e.getMessage());
        }
    }

    private void updateStatistics() {
        int total = sessionsList.size();
        long scheduled = sessionsList.stream()
                .filter(s -> Session.STATUS_CONFIRMED.equals(s.getStatus()))
                .count();
        long completed = sessionsList.stream()
                .filter(s -> Session.STATUS_COMPLETED.equals(s.getStatus()))
                .count();

        totalSessionsLabel.setText(String.valueOf(total));
        scheduledLabel.setText(String.valueOf(scheduled));
        completedLabel.setText(String.valueOf(completed));
    }

    private void updateSelectionLabel() {
        if (selectedSession == null) {
            selectionLabel.setText("Aucune session sélectionnée");
        } else {
            try {
                CoachingRequest request = requestService.findById(selectedSession.getCoachingRequestId()).orElse(null);
                if (request != null) {
                    Optional<User> user = userService.findById(request.getUserId());
                    String clientName = user.map(u -> u.getFirstName() + " " + u.getLastName())
                            .orElse("Client inconnu");
                    selectionLabel.setText("Session #" + selectedSession.getId() + " - " + clientName);
                } else {
                    selectionLabel.setText("Session #" + selectedSession.getId());
                }
            } catch (SQLException e) {
                selectionLabel.setText("Session #" + selectedSession.getId());
            }
        }
    }

    private void handleModify() {
        if (selectedSession == null) {
            showWarning("Veuillez sélectionner une session à modifier");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/edit_session.fxml"));
            Parent root = loader.load();

            EditSessionController controller = loader.getController();
            controller.setSession(selectedSession);

            Stage stage = (Stage) modifyBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Modifier la session");

        } catch (IOException e) {
            showError("Erreur de navigation", "Impossible de charger le formulaire de modification");
            e.printStackTrace();
        }
    }

    private void handleDelete() {
        if (selectedSession == null) {
            showWarning("Veuillez sélectionner une session à supprimer");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la session");
        confirm.setHeaderText("Supprimer cette session ?");
        confirm.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                sessionService.deleteSession(selectedSession.getId());
                showSuccess("Session supprimée avec succès");
                loadSessions();
                updateStatistics();
                selectedSession = null;
                updateSelectionLabel();
            } catch (SQLException e) {
                showError("Erreur lors de la suppression", e.getMessage());
            }
        }
    }

    private void handleComplete() {
        if (selectedSession == null) {
            showWarning("Veuillez sélectionner une session");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Marquer comme terminée");
        confirm.setHeaderText("Marquer cette session comme terminée ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                selectedSession.setStatus(Session.STATUS_COMPLETED);
                sessionService.updateSession(selectedSession);
                showSuccess("Session marquée comme terminée");
                loadSessions();
                updateStatistics();
            } catch (SQLException e) {
                showError("Erreur lors de la mise à jour", e.getMessage());
            }
        }
    }

    private String formatStatus(String status) {
        return switch (status) {
            case Session.STATUS_CONFIRMED -> "Confirmée";
            case Session.STATUS_COMPLETED -> "Terminée";
            case Session.STATUS_CANCELLED -> "Annulée";
            default -> status;
        };
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

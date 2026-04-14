package controllers.coach;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.coaching_session.CoachingRequest;
import model.coaching_session.Session;
import model.user.User;
import services.UserServices.UserService;
import services.coaching_session_module.CoachingRequestService;
import services.coaching_session_module.CoachingWorkflowService;
import services.coaching_session_module.SessionService;
import session.AppSession;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CoachRequestsController implements Initializable {

    // Labels statistiques
    @FXML private Label pendingCountLabel;
    @FXML private Label acceptedCountLabel;
    @FXML private Label declinedCountLabel;
    @FXML private Label todaySessionsLabel;
    @FXML private Label conversionRateLabel;
    @FXML private Label paginationLabel;

    // Filtres
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> priorityFilterCombo;
    @FXML private DatePicker dateFromPicker;
    @FXML private DatePicker dateToPicker;
    @FXML private Button applyFiltersBtn;
    @FXML private Button refreshBtn;

    // TableView
    @FXML private TableView<CoachingRequest> requestsTable;
    @FXML private TableColumn<CoachingRequest, String> idColumn;
    @FXML private TableColumn<CoachingRequest, String> userColumn;
    @FXML private TableColumn<CoachingRequest, String> messageColumn;
    @FXML private TableColumn<CoachingRequest, String> priorityColumn;
    @FXML private TableColumn<CoachingRequest, String> statusColumn;
    @FXML private TableColumn<CoachingRequest, String> dateColumn;
    @FXML private TableColumn<CoachingRequest, Void> actionsColumn;

    // Services
    private final CoachingRequestService requestService;
    private final CoachingWorkflowService workflowService;
    private final SessionService sessionService;
    private final UserService userService;

    // Données
    private final ObservableList<CoachingRequest> requestsList;
    private final ObservableList<CoachingRequest> filteredList;
    private int currentCoachId;

    public CoachRequestsController() {
        this.requestService = new CoachingRequestService();
        this.workflowService = new CoachingWorkflowService();
        this.sessionService = new SessionService();
        this.userService = new UserService();
        this.requestsList = FXCollections.observableArrayList();
        this.filteredList = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Récupérer l'ID du coach connecté
        currentCoachId = AppSession.getCurrentUser()
                .map(User::getId)
                .orElse(1); // TODO: Gérer le cas où l'utilisateur n'est pas connecté

        setupFilters();
        setupTableView();
        setupButtons();
        loadRequests();
        updateStatistics();
    }

    private void setupFilters() {
        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "Tous",
                "En attente",
                "Acceptée",
                "Refusée"
        ));
        statusFilterCombo.setValue("Tous");
        
        priorityFilterCombo.setItems(FXCollections.observableArrayList(
                "Toutes",
                "Normal",
                "Moyen",
                "Urgent"
        ));
        priorityFilterCombo.setValue("Toutes");
        
        // Recherche en temps réel
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }
    }

    private void setupTableView() {
        // Configuration des colonnes
        idColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getId()))
        );

        userColumn.setCellValueFactory(cellData -> {
            CoachingRequest request = cellData.getValue();
            try {
                Optional<User> user = userService.findById(request.getUserId());
                if (user.isPresent()) {
                    User u = user.get();
                    return new SimpleStringProperty(u.getFirstName() + " " + u.getLastName());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new SimpleStringProperty("User #" + request.getUserId());
        });

        messageColumn.setCellValueFactory(cellData -> {
            String message = cellData.getValue().getMessage();
            return new SimpleStringProperty(message.length() > 50 ? message.substring(0, 47) + "..." : message);
        });

        priorityColumn.setCellValueFactory(cellData -> {
            String priority = cellData.getValue().getPriority();
            return new SimpleStringProperty(formatPriority(priority));
        });

        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            return new SimpleStringProperty(formatStatus(status));
        });

        dateColumn.setCellValueFactory(cellData -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return new SimpleStringProperty(sdf.format(cellData.getValue().getCreatedAt()));
        });

        // Colonne Actions avec boutons
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button acceptBtn = new Button("Accepter");
            private final Button refuseBtn = new Button("Refuser");
            private final Button createSessionBtn = new Button("Créer session");
            private final HBox container = new HBox(5);

            {
                acceptBtn.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 5 10; -fx-cursor: hand;");
                refuseBtn.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 5 10; -fx-cursor: hand;");
                createSessionBtn.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 5 10; -fx-cursor: hand;");

                container.setAlignment(Pos.CENTER);

                acceptBtn.setOnAction(event -> {
                    CoachingRequest request = getTableView().getItems().get(getIndex());
                    handleAccept(request);
                });

                refuseBtn.setOnAction(event -> {
                    CoachingRequest request = getTableView().getItems().get(getIndex());
                    handleRefuse(request);
                });

                createSessionBtn.setOnAction(event -> {
                    CoachingRequest request = getTableView().getItems().get(getIndex());
                    handleCreateSession(request);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CoachingRequest request = getTableView().getItems().get(getIndex());
                    container.getChildren().clear();

                    if (CoachingRequest.STATUS_PENDING.equals(request.getStatus())) {
                        container.getChildren().addAll(acceptBtn, refuseBtn);
                    } else if (CoachingRequest.STATUS_ACCEPTED.equals(request.getStatus())) {
                        container.getChildren().add(createSessionBtn);
                    }

                    setGraphic(container);
                }
            }
        });

        // Style des lignes selon la priorité
        requestsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(CoachingRequest request, boolean empty) {
                super.updateItem(request, empty);
                if (empty || request == null) {
                    setStyle("");
                } else {
                    switch (request.getPriority()) {
                        case CoachingRequest.PRIORITY_URGENT ->
                                setStyle("-fx-background-color: #ffebee;");
                        case CoachingRequest.PRIORITY_MEDIUM ->
                                setStyle("-fx-background-color: #fff3e0;");
                        default -> setStyle("");
                    }
                }
            }
        });

        requestsTable.setItems(filteredList);
    }

    private void setupButtons() {
        refreshBtn.setOnAction(event -> {
            loadRequests();
            updateStatistics();
        });
        
        if (applyFiltersBtn != null) {
            applyFiltersBtn.setOnAction(event -> applyFilters());
        }
    }

    private void loadRequests() {
        try {
            List<CoachingRequest> requests = requestService.getRequestsByCoach(currentCoachId);
            requestsList.clear();
            requestsList.addAll(requests);
            applyFilters();
        } catch (SQLException e) {
            showError("Erreur lors du chargement des demandes", e.getMessage());
        }
    }

    private void applyFilters() {
        String statusValue = statusFilterCombo.getValue();
        String priorityValue = priorityFilterCombo != null ? priorityFilterCombo.getValue() : "Toutes";
        String searchText = searchField != null ? searchField.getText().toLowerCase().trim() : "";

        List<CoachingRequest> filtered = requestsList.stream()
                .filter(request -> {
                    // Filtre recherche
                    if (!searchText.isEmpty()) {
                        try {
                            Optional<User> user = userService.findById(request.getUserId());
                            if (user.isPresent()) {
                                String fullName = (user.get().getFirstName() + " " + user.get().getLastName()).toLowerCase();
                                String email = user.get().getEmail() != null ? user.get().getEmail().toLowerCase() : "";
                                String message = request.getMessage().toLowerCase();
                                
                                if (!fullName.contains(searchText) && !email.contains(searchText) && !message.contains(searchText)) {
                                    return false;
                                }
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    
                    // Filtre statut
                    if (statusValue != null && !statusValue.equals("Tous")) {
                        String status = switch (statusValue) {
                            case "En attente" -> CoachingRequest.STATUS_PENDING;
                            case "Acceptée" -> CoachingRequest.STATUS_ACCEPTED;
                            case "Refusée" -> CoachingRequest.STATUS_DECLINED;
                            default -> null;
                        };
                        if (status != null && !request.getStatus().equals(status)) {
                            return false;
                        }
                    }
                    
                    // Filtre priorité
                    if (priorityValue != null && !priorityValue.equals("Toutes")) {
                        String priority = switch (priorityValue) {
                            case "Normal" -> CoachingRequest.PRIORITY_NORMAL;
                            case "Moyen" -> CoachingRequest.PRIORITY_MEDIUM;
                            case "Urgent" -> CoachingRequest.PRIORITY_URGENT;
                            default -> null;
                        };
                        if (priority != null && !request.getPriority().equals(priority)) {
                            return false;
                        }
                    }
                    
                    // Filtre date
                    if (dateFromPicker != null && dateFromPicker.getValue() != null) {
                        java.time.LocalDate fromDate = dateFromPicker.getValue();
                        java.time.LocalDate requestDate = new java.sql.Date(request.getCreatedAt().getTime()).toLocalDate();
                        if (requestDate.isBefore(fromDate)) {
                            return false;
                        }
                    }
                    
                    if (dateToPicker != null && dateToPicker.getValue() != null) {
                        java.time.LocalDate toDate = dateToPicker.getValue();
                        java.time.LocalDate requestDate = new java.sql.Date(request.getCreatedAt().getTime()).toLocalDate();
                        if (requestDate.isAfter(toDate)) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());

        filteredList.clear();
        filteredList.addAll(filtered);
        
        // Mettre à jour le label de pagination
        if (paginationLabel != null) {
            paginationLabel.setText("Affichage de " + filtered.size() + " demande(s)");
        }
    }

    private void updateStatistics() {
        long pending = requestsList.stream()
                .filter(r -> CoachingRequest.STATUS_PENDING.equals(r.getStatus()))
                .count();
        long accepted = requestsList.stream()
                .filter(r -> CoachingRequest.STATUS_ACCEPTED.equals(r.getStatus()))
                .count();
        long declined = requestsList.stream()
                .filter(r -> CoachingRequest.STATUS_DECLINED.equals(r.getStatus()))
                .count();
        
        int total = requestsList.size();
        double conversionRate = total > 0 ? (accepted * 100.0 / total) : 0.0;

        pendingCountLabel.setText(String.valueOf(pending));
        acceptedCountLabel.setText(String.valueOf(accepted));
        declinedCountLabel.setText(String.valueOf(declined));
        
        if (conversionRateLabel != null) {
            conversionRateLabel.setText(String.format("%.1f%%", conversionRate));
        }
        
        // TODO: Récupérer le nombre de sessions aujourd'hui depuis SessionService
        if (todaySessionsLabel != null) {
            todaySessionsLabel.setText("0");
        }
        
        if (paginationLabel != null) {
            paginationLabel.setText("Affichage de " + filteredList.size() + " demande(s)");
        }
    }

    private void handleAccept(CoachingRequest request) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Accepter la demande");
        confirm.setHeaderText("Accepter cette demande de coaching ?");
        confirm.setContentText("Une session sera créée, puis vous pourrez compléter ses détails.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                System.out.println("[CoachRequestsController] accept requestId=" + request.getId() + ", coachId=" + currentCoachId);
                workflowService.acceptCoachingRequest(request.getId(), currentCoachId);
                Session existing = sessionService.findByCoachingRequestId(request.getId());
                if (existing == null) {
                    Session session = new Session();
                    session.setCoachingRequestId(request.getId());
                    session.setScheduledAt(new Date());
                    session.setDuration(60);
                    session.setObjective("Coaching session");
                    session.setStatus(Session.STATUS_CONFIRMED);
                    sessionService.addSession(session);
                    requestService.updateStatus(request.getId(), CoachingRequest.STATUS_ACCEPTED);
                    System.out.println("Session created for coach: " + currentCoachId);
                } else {
                    System.out.println("Session already exists for coach: " + currentCoachId + " (sessionId=" + existing.getId() + ")");
                }
                loadRequests();
                updateStatistics();
                openAddSessionForm(request);
            } catch (SQLException e) {
                showError("Erreur lors de l'acceptation", e.getMessage());
            } catch (IllegalStateException | IllegalArgumentException e) {
                showError("Action impossible", e.getMessage());
            }
        }
    }

    private void handleRefuse(CoachingRequest request) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Refuser la demande");
        confirm.setHeaderText("Refuser cette demande de coaching ?");
        confirm.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                requestService.updateStatus(request.getId(), CoachingRequest.STATUS_DECLINED);
                showSuccess("Demande refusée");
                loadRequests();
                updateStatistics();
            } catch (SQLException e) {
                showError("Erreur lors du refus", e.getMessage());
            }
        }
    }

    private void handleCreateSession(CoachingRequest request) {
        openAddSessionForm(request);
    }

    private void openAddSessionForm(CoachingRequest request) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/add_session.fxml"));
            Parent root = loader.load();

            AddSessionController controller = loader.getController();
            controller.setRequest(request);
            controller.setOnSaved(() -> {
                loadRequests();
                updateStatistics();
            });

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Créer une session");
            stage.show();

        } catch (IOException e) {
            showError("Erreur de navigation", "Impossible de charger le formulaire de session");
            e.printStackTrace();
        }
    }

    private String formatPriority(String priority) {
        return switch (priority) {
            case CoachingRequest.PRIORITY_URGENT -> "Urgent";
            case CoachingRequest.PRIORITY_MEDIUM -> "Moyen";
            case CoachingRequest.PRIORITY_NORMAL -> "Normal";
            default -> priority;
        };
    }

    private String formatStatus(String status) {
        return switch (status) {
            case CoachingRequest.STATUS_PENDING -> "En attente";
            case CoachingRequest.STATUS_ACCEPTED -> "Acceptée";
            case CoachingRequest.STATUS_DECLINED -> "Refusée";
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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

package controllers;

import controllers.navigation.NavigationManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.coaching_session.CoachingRequest;
import model.user.User;
import services.UserServices.UserService;
import services.coaching_session_module.CoachingRequestService;
import session.AppSession;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MesDemandesController implements Initializable {

    // Labels statistiques
    @FXML private Label totalLabel;
    @FXML private Label pendingLabel;
    @FXML private Label acceptedLabel;
    @FXML private Label declinedLabel;

    // Recherche et filtres
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> priorityFilter;
    @FXML private Button filterBtn;

    // TableView
    @FXML private TableView<CoachingRequest> tableRequests;
    @FXML private TableColumn<CoachingRequest, String> idColumn;
    @FXML private TableColumn<CoachingRequest, String> coachColumn;
    @FXML private TableColumn<CoachingRequest, String> messageColumn;
    @FXML private TableColumn<CoachingRequest, String> prioriteColumn;
    @FXML private TableColumn<CoachingRequest, String> statusColumn;
    @FXML private TableColumn<CoachingRequest, String> dateColumn;

    // Boutons
    @FXML private Button newRequestBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Button refreshBtn;

    // Label sélection
    @FXML private Label selectionLabel;

    // Services
    private final CoachingRequestService requestService;
    private final UserService userService;

    // Données
    private final ObservableList<CoachingRequest> requestsList;
    private final ObservableList<CoachingRequest> filteredList;
    private CoachingRequest selectedRequest;
    private int currentUserId = 1; // TODO: Récupérer l'utilisateur connecté

    public MesDemandesController() {
        this.requestService = new CoachingRequestService();
        this.userService = new UserService();
        this.requestsList = FXCollections.observableArrayList();
        this.filteredList = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilters();
        setupTableView();
        setupButtons();
        loadRequests();
        updateStatistics();
    }

    private void setupFilters() {
        // Remplir les filtres
        statusFilter.setItems(FXCollections.observableArrayList(
                "Tous",
                CoachingRequest.STATUS_PENDING,
                CoachingRequest.STATUS_ACCEPTED,
                CoachingRequest.STATUS_DECLINED,
                CoachingRequest.STATUS_PAID,
                CoachingRequest.STATUS_CONFIRMED,
                CoachingRequest.STATUS_COMPLETED,
                CoachingRequest.STATUS_CANCELLED
        ));
        statusFilter.setValue("Tous");

        priorityFilter.setItems(FXCollections.observableArrayList(
                "Toutes",
                CoachingRequest.PRIORITY_NORMAL,
                CoachingRequest.PRIORITY_MEDIUM,
                CoachingRequest.PRIORITY_URGENT
        ));
        priorityFilter.setValue("Toutes");

        // Listener pour la recherche en temps réel
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void setupTableView() {
        // Configuration des colonnes
        idColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getId()))
        );

        coachColumn.setCellValueFactory(cellData -> {
            CoachingRequest request = cellData.getValue();
            try {
                Optional<User> coach = userService.findById(request.getCoachId());
                if (coach.isPresent()) {
                    User c = coach.get();
                    return new SimpleStringProperty(c.getFirstName() + " " + c.getLastName());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new SimpleStringProperty("Coach #" + request.getCoachId());
        });

        messageColumn.setCellValueFactory(cellData -> {
            String message = cellData.getValue().getMessage();
            return new SimpleStringProperty(message.length() > 50 ? message.substring(0, 47) + "..." : message);
        });

        prioriteColumn.setCellValueFactory(cellData -> {
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

        // Style des lignes selon la priorité
        tableRequests.setRowFactory(tv -> new TableRow<>() {
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

        // Lier les données
        tableRequests.setItems(filteredList);

        // Gérer la sélection
        tableRequests.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    selectedRequest = newValue;
                    updateSelectionLabel();
                }
        );
    }

    private void setupButtons() {
        newRequestBtn.setOnAction(event -> handleNewRequest());
        updateBtn.setOnAction(event -> handleUpdate());
        deleteBtn.setOnAction(event -> handleDelete());
        refreshBtn.setOnAction(event -> {
            loadRequests();
            updateStatistics();
        });
        filterBtn.setOnAction(event -> applyFilters());
    }

    private void loadRequests() {
        try {
            User currentUser = AppSession.getCurrentUser()
                    .filter(u -> u.getId() != null)
                    .orElseGet(() -> {
                        User fallback = new User();
                        fallback.setId(currentUserId);
                        return fallback;
                    });
            List<CoachingRequest> requests = requestService.getRequestsByUser(currentUser);
            requestsList.clear();
            requestsList.addAll(requests);
            applyFilters();
        } catch (SQLException e) {
            showError("Erreur lors du chargement des demandes", e.getMessage());
        }
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();
        String statusValue = statusFilter.getValue();
        String priorityValue = priorityFilter.getValue();

        List<CoachingRequest> filtered = requestsList.stream()
                .filter(request -> {
                    // Filtre recherche
                    if (!searchText.isEmpty()) {
                        String message = request.getMessage().toLowerCase();
                        if (!message.contains(searchText)) {
                            return false;
                        }
                    }

                    // Filtre statut
                    if (statusValue != null && !statusValue.equals("Tous")) {
                        if (!request.getStatus().equals(statusValue)) {
                            return false;
                        }
                    }

                    // Filtre priorité
                    if (priorityValue != null && !priorityValue.equals("Toutes")) {
                        if (!request.getPriority().equals(priorityValue)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        filteredList.clear();
        filteredList.addAll(filtered);
    }

    private void updateStatistics() {
        int total = requestsList.size();
        long pending = requestsList.stream()
                .filter(r -> CoachingRequest.STATUS_PENDING.equals(r.getStatus()))
                .count();
        long accepted = requestsList.stream()
                .filter(r -> CoachingRequest.STATUS_ACCEPTED.equals(r.getStatus()))
                .count();
        long declined = requestsList.stream()
                .filter(r -> CoachingRequest.STATUS_DECLINED.equals(r.getStatus()))
                .count();

        totalLabel.setText(String.valueOf(total));
        pendingLabel.setText(String.valueOf(pending));
        acceptedLabel.setText(String.valueOf(accepted));
        declinedLabel.setText(String.valueOf(declined));
    }

    private void updateSelectionLabel() {
        if (selectedRequest == null) {
            selectionLabel.setText("Aucune demande sélectionnée");
        } else {
            try {
                Optional<User> coach = userService.findById(selectedRequest.getCoachId());
                String coachName = coach.map(c -> c.getFirstName() + " " + c.getLastName())
                        .orElse("Coach #" + selectedRequest.getCoachId());
                selectionLabel.setText("Demande #" + selectedRequest.getId() + " - " + coachName +
                        " - " + formatStatus(selectedRequest.getStatus()));
            } catch (SQLException e) {
                selectionLabel.setText("Demande #" + selectedRequest.getId());
            }
        }
    }

    @FXML
    private void handleNewRequest() {
        try {
            NavigationManager.show("/views/coaching_request.fxml", "Nouvelle demande de coaching");
        } catch (IOException e) {
            showError("Erreur de navigation", "Impossible de charger la page de demande");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            showError("Erreur de navigation", e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedRequest == null) {
            showWarning("Veuillez sélectionner une demande à modifier");
            return;
        }

        try {
            CoachingRequestController controller = NavigationManager.showAndGetController(
                    "/views/coaching_request.fxml",
                    "Modifier la demande"
            );
            controller.loadRequestForUpdate(selectedRequest);
        } catch (IOException e) {
            showError("Erreur de navigation", "Impossible de charger la page de modification");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            showError("Erreur de navigation", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedRequest == null) {
            showWarning("Veuillez sélectionner une demande à supprimer");
            return;
        }

        // Confirmation
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la demande");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette demande ?\n\n" +
                "Demande #" + selectedRequest.getId() + "\n" +
                "Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                requestService.delete(selectedRequest.getId());
                showSuccess("Demande supprimée avec succès");
                loadRequests();
                updateStatistics();
                selectedRequest = null;
                updateSelectionLabel();
            } catch (SQLException e) {
                showError("Erreur lors de la suppression", e.getMessage());
            }
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        try {
            NavigationManager.show("/user/userdashboard/user_dashboard.fxml", "DayFlow — Accueil");
        } catch (IOException | IllegalStateException e) {
            // Fallback si la navigation shell n'est pas initialisée dans ce contexte.
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/userdashboard/user_dashboard.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("DayFlow — Accueil");
                stage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    @FXML
    private void goToSessions(ActionEvent event) {
        try {
            NavigationManager.show("/views/mes_sessions.fxml", "Mes sessions");
        } catch (IOException e) {
            showError("Erreur de navigation", "Impossible d'ouvrir la page des sessions");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            showError("Erreur de navigation", e.getMessage());
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
            case CoachingRequest.STATUS_PAID -> "Payée";
            case CoachingRequest.STATUS_CONFIRMED -> "Confirmée";
            case CoachingRequest.STATUS_COMPLETED -> "Terminée";
            case CoachingRequest.STATUS_CANCELLED -> "Annulée";
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

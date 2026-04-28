package controllers.coaching_session;

import controllers.navigation.NavigationManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import services.account.UserService;
import services.coaching_session.CoachingRequestService;
import session.AppSession;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

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
    @FXML private DatePicker dateFromFilter;
    @FXML private DatePicker dateToFilter;
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
    @FXML private Button payButton;
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
    private final FilteredList<CoachingRequest> filteredList;
    private final Map<Integer, String> searchableTextByRequestId = new HashMap<>();
    private CoachingRequest selectedRequest;
    private int currentUserId = 1; // TODO: Récupérer l'utilisateur connecté

    public MesDemandesController() {
        this.requestService = new CoachingRequestService();
        this.userService = new UserService();
        this.requestsList = FXCollections.observableArrayList();
        this.filteredList = new FilteredList<>(requestsList, r -> true);
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
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        priorityFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (dateFromFilter != null) {
            dateFromFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
        if (dateToFilter != null) {
            dateToFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
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
        SortedList<CoachingRequest> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tableRequests.comparatorProperty());
        tableRequests.setItems(sortedList);

        // Gérer la sélection
        tableRequests.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    selectedRequest = newValue;
                    updateSelectionLabel();
                    updatePayButtonState();
                }
        );
    }

    private void setupButtons() {
        newRequestBtn.setOnAction(event -> handleNewRequest());
        payButton.setOnAction(event -> handlePayment());
        updateBtn.setOnAction(event -> handleUpdate());
        deleteBtn.setOnAction(event -> handleDelete());
        refreshBtn.setOnAction(event -> {
            loadRequests();
            updateStatistics();
        });
        filterBtn.setOnAction(event -> applyFilters());
        
        // Désactiver le bouton de paiement par défaut
        payButton.setDisable(true);
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
            buildSearchCache(requests);
            applyFilters();
        } catch (SQLException e) {
            showError("Erreur lors du chargement des demandes", e.getMessage());
        }
    }

    private void applyFilters() {
        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String statusValue = statusFilter.getValue();
        String priorityValue = priorityFilter.getValue();
        LocalDate from = dateFromFilter != null ? dateFromFilter.getValue() : null;
        LocalDate to = dateToFilter != null ? dateToFilter.getValue() : null;

        filteredList.setPredicate(request -> matchesSearch(request, searchText)
                && matchesStatus(request, statusValue)
                && matchesPriority(request, priorityValue)
                && matchesDateRange(request, from, to));
    }

    private void buildSearchCache(List<CoachingRequest> requests) {
        searchableTextByRequestId.clear();
        for (CoachingRequest request : requests) {
            StringBuilder sb = new StringBuilder();
            sb.append(request.getMessage() != null ? request.getMessage() : "").append(' ');
            try {
                Optional<User> coach = userService.findById(request.getCoachId());
                if (coach.isPresent()) {
                    User c = coach.get();
                    sb.append(c.getFirstName() != null ? c.getFirstName() : "").append(' ');
                    sb.append(c.getLastName() != null ? c.getLastName() : "").append(' ');
                    sb.append(c.getEmail() != null ? c.getEmail() : "");
                }
            } catch (SQLException ignored) {
                // On garde juste le message en cas d'erreur sur la jointure utilisateur.
            }
            searchableTextByRequestId.put(request.getId(), sb.toString().toLowerCase());
        }
    }

    private boolean matchesSearch(CoachingRequest request, String searchText) {
        if (searchText == null || searchText.isBlank()) {
            return true;
        }
        return searchableTextByRequestId.getOrDefault(request.getId(), "").contains(searchText);
    }

    private boolean matchesStatus(CoachingRequest request, String statusValue) {
        if (statusValue == null || "Tous".equals(statusValue)) {
            return true;
        }
        return statusValue.equals(request.getStatus());
    }

    private boolean matchesPriority(CoachingRequest request, String priorityValue) {
        if (priorityValue == null || "Toutes".equals(priorityValue)) {
            return true;
        }
        return priorityValue.equals(request.getPriority());
    }

    private boolean matchesDateRange(CoachingRequest request, LocalDate from, LocalDate to) {
        if (request.getCreatedAt() == null) {
            return from == null && to == null;
        }
        LocalDate createdDate = new java.sql.Date(request.getCreatedAt().getTime()).toLocalDate();
        if (from != null && createdDate.isBefore(from)) {
            return false;
        }
        return to == null || !createdDate.isAfter(to);
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

    /**
     * Met à jour l'état du bouton de paiement selon la demande sélectionnée.
     */
    private void updatePayButtonState() {
        if (selectedRequest == null) {
            payButton.setDisable(true);
            return;
        }

        // Le bouton est actif uniquement si la demande est acceptée
        boolean isAccepted = CoachingRequest.STATUS_ACCEPTED.equals(selectedRequest.getStatus());
        payButton.setDisable(!isAccepted);
    }

    @FXML
    private void handleNewRequest() {
        try {
            NavigationManager.show("/user/coaching_session/coaching_request.fxml", "Nouvelle demande de coaching");
        } catch (IOException e) {
            showError("Erreur de navigation", "Impossible de charger la page de demande");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            showError("Erreur de navigation", e.getMessage());
        }
    }

    @FXML
    private void handlePayment() {
        if (selectedRequest == null) {
            showWarning("Veuillez sélectionner une demande à payer");
            return;
        }

        // Vérifier que la demande est acceptée
        if (!CoachingRequest.STATUS_ACCEPTED.equals(selectedRequest.getStatus())) {
            showWarning("Seules les demandes acceptées peuvent être payées");
            return;
        }

        try {
            // Charger la page de paiement
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/payment/payment.fxml"));
            Parent root = loader.load();
            
            // Récupérer le contrôleur et charger les données
            controllers.payment.PaymentController controller = loader.getController();
            controller.loadPaymentForRequest(selectedRequest);
            
            // Créer une nouvelle fenêtre
            Stage stage = new Stage();
            stage.setTitle("Paiement de la séance");
            stage.setScene(new javafx.scene.Scene(root));
            stage.setResizable(false);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            
            // Rafraîchir la liste après fermeture
            stage.setOnHidden(event -> {
                loadRequests();
                updateStatistics();
            });
            
            stage.showAndWait();
            
        } catch (IOException e) {
            showError("Erreur de navigation", "Impossible de charger la page de paiement");
            e.printStackTrace();
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
                    "/user/coaching_session/coaching_request.fxml",
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
            NavigationManager.show("/user/account/user_dashboard.fxml", "DayFlow — Accueil");
        } catch (IOException | IllegalStateException e) {
            // Fallback si la navigation shell n'est pas initialisée dans ce contexte.
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/account/user_dashboard.fxml"));
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
            NavigationManager.show("/user/coaching_session/mes_sessions.fxml", "Mes sessions");
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

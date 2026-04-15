package controllers.coaching_session;

import controllers.components.CoachCardController;
import controllers.navigation.NavigationManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.coaching_session.CoachingRequest;
import model.user.User;
import session.AppSession;
import services.account.CoachService;
import services.account.UserService;
import services.coaching_session.CoachingRequestService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CoachingRequestController implements Initializable {

    private static final Logger LOG = Logger.getLogger(CoachingRequestController.class.getName());

    // Formulaire
    @FXML private ComboBox<User> coachComboBox;
    @FXML private TextArea messageTextArea;
    @FXML private RadioButton normalRadio;
    @FXML private RadioButton moyenRadio;
    @FXML private RadioButton urgentRadio;
    @FXML private ToggleGroup priorityToggleGroup;
    @FXML private ComboBox<String> objectifComboBox;
    @FXML private ComboBox<String> niveauComboBox;
    @FXML private ComboBox<String> frequenceComboBox;
    @FXML private TextField budgetTextField;
    @FXML private Button submitButton;
    @FXML private FlowPane coachCardsFlow;
    
    // Recherche et filtres
    @FXML private TextField searchTextField;
    @FXML private Button searchButton;
    @FXML private ComboBox<String> specialiteComboBox;
    @FXML private TextField prixMinTextField;
    @FXML private TextField prixMaxTextField;
    @FXML private ComboBox<String> noteComboBox;
    @FXML private ComboBox<String> disponibiliteComboBox;
    @FXML private Button applyFiltersButton;

    // Services
    private final CoachingRequestService requestService;
    private final UserService userService;
    private final CoachService coachService;

    public CoachingRequestController() {
        this.requestService = new CoachingRequestService();
        this.userService = new UserService();
        this.coachService = new CoachService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFormFields();
        setupButton();
        setupSearchAndFilters();
        loadCoaches();
    }
    
    private void setupSearchAndFilters() {
        // Remplir les filtres
        if (specialiteComboBox != null) {
            specialiteComboBox.setItems(FXCollections.observableArrayList(
                    "Toutes", "Méditation", "Diététique", "Yoga", "Boxe", "Nutrition"
            ));
            specialiteComboBox.setValue("Toutes");
        }
        
        if (noteComboBox != null) {
            noteComboBox.setItems(FXCollections.observableArrayList(
                    "Toutes", "4.5+", "4.0+", "3.5+", "3.0+"
            ));
            noteComboBox.setValue("Toutes");
        }
        
        if (disponibiliteComboBox != null) {
            disponibiliteComboBox.setItems(FXCollections.observableArrayList(
                    "Toutes", "Immédiate", "Cette semaine", "Ce mois"
            ));
            disponibiliteComboBox.setValue("Toutes");
        }
        
        // Bouton recherche
        if (searchButton != null) {
            searchButton.setOnAction(event -> handleSearch());
        }
        
        // Bouton appliquer filtres
        if (applyFiltersButton != null) {
            applyFiltersButton.setOnAction(event -> handleApplyFilters());
        }
    }
    
    private void handleSearch() {
        String searchText = searchTextField.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            loadCoaches();
            return;
        }
        
        try {
            List<User> allCoaches = coachService.getAllCoaches();
            String search = searchText.trim().toLowerCase();
            
            List<User> coaches = allCoaches.stream()
                    .filter(c -> {
                        String fullName = (c.getFirstName() + " " + c.getLastName()).toLowerCase();
                        String speciality = c.getSpeciality() != null ? c.getSpeciality().toLowerCase() : "";
                        return fullName.contains(search) || speciality.contains(search);
                    })
                    .collect(java.util.stream.Collectors.toList());
                    
            coachComboBox.setItems(FXCollections.observableArrayList(coaches));
            loadCoachCards(coaches);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Recherche de coachs", e);
            showError("Erreur lors de la recherche", e.getMessage());
        }
    }
    
    private void handleApplyFilters() {
        try {
            List<User> coaches = coachService.getAllCoaches();
            
            // Filtre spécialité
            if (specialiteComboBox != null && specialiteComboBox.getValue() != null 
                    && !specialiteComboBox.getValue().equals("Toutes")) {
                String specialite = specialiteComboBox.getValue();
                coaches = coaches.stream()
                        .filter(c -> c.getSpeciality() != null && c.getSpeciality().contains(specialite))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            // Filtre prix
            if (prixMinTextField != null && !prixMinTextField.getText().trim().isEmpty()) {
                try {
                    double minPrice = Double.parseDouble(prixMinTextField.getText().trim());
                    coaches = coaches.stream()
                            .filter(c -> c.getPricePerSession() != null && c.getPricePerSession() >= minPrice)
                            .collect(java.util.stream.Collectors.toList());
                } catch (NumberFormatException ignored) {}
            }
            
            if (prixMaxTextField != null && !prixMaxTextField.getText().trim().isEmpty()) {
                try {
                    double maxPrice = Double.parseDouble(prixMaxTextField.getText().trim());
                    coaches = coaches.stream()
                            .filter(c -> c.getPricePerSession() != null && c.getPricePerSession() <= maxPrice)
                            .collect(java.util.stream.Collectors.toList());
                } catch (NumberFormatException ignored) {}
            }
            
            // Filtre note
            if (noteComboBox != null && noteComboBox.getValue() != null 
                    && !noteComboBox.getValue().equals("Toutes")) {
                String noteStr = noteComboBox.getValue().replace("+", "");
                try {
                    double minRating = Double.parseDouble(noteStr);
                    coaches = coaches.stream()
                            .filter(c -> c.getRating() != null && c.getRating() >= minRating)
                            .collect(java.util.stream.Collectors.toList());
                } catch (NumberFormatException ignored) {}
            }
            
            coachComboBox.setItems(FXCollections.observableArrayList(coaches));
            loadCoachCards(coaches);
            
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Application des filtres", e);
            showError("Erreur lors du filtrage", e.getMessage());
        }
    }

    private void setupFormFields() {
        // Remplir les ComboBox avec des valeurs
        objectifComboBox.setItems(FXCollections.observableArrayList(
                "Perte de poids", "Prise de masse", "Développement personnel",
                "Gestion du stress", "Amélioration des performances"
        ));

        niveauComboBox.setItems(FXCollections.observableArrayList(
                "Débutant", "Intermédiaire", "Avancé", "Expert"
        ));

        frequenceComboBox.setItems(FXCollections.observableArrayList(
                "1 fois par semaine", "2 fois par semaine", "3 fois par semaine",
                "Quotidien", "Flexible"
        ));

        // Configurer le ComboBox des coachs
        coachComboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getFirstName() + " " + user.getLastName());
                }
            }
        });

        coachComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getFirstName() + " " + user.getLastName());
                }
            }
        });
    }

    private void setupButton() {
        submitButton.setOnAction(event -> handleSubmit());
    }

    private void loadCoaches() {
        try {
            List<User> coaches = coachService.getAllCoaches();
            System.out.println("[CoachingRequest] Nombre de coachs chargés (BD) : " + coaches.size());

            coachComboBox.setItems(FXCollections.observableArrayList(coaches));
            loadCoachCards(coaches);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Chargement des coachs", e);
            showError("Erreur lors du chargement des coachs", e.getMessage());
        }
    }

    private void loadCoachCards(List<User> coaches) {
        if (coachCardsFlow == null) {
            LOG.warning("[CoachingRequest] coachCardsFlow non injecté — cartes non affichées");
            return;
        }

        coachCardsFlow.getChildren().clear();

        URL cardUrl = CoachCardController.class.getResource("/user/coaching_session/components/CoachCard.fxml");
        if (cardUrl == null) {
            LOG.severe("[CoachingRequest] Ressource introuvable : /user/coaching_session/components/CoachCard.fxml");
            return;
        }

        int added = 0;
        for (User coach : coaches) {
            try {
                FXMLLoader loader = new FXMLLoader(cardUrl);
                Node cardRoot = loader.load();
                CoachCardController controller = loader.getController();
                controller.setData(coach);
                coachCardsFlow.getChildren().add(cardRoot);
                added++;
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Impossible de charger une carte coach (id=" + coach.getId() + ")", e);
            }
        }
        System.out.println("[CoachingRequest] Nombre de cartes ajoutées (UI) : " + added);
    }

    @FXML
    private void handleSubmit() {
        try {
            // Validation
            if (coachComboBox.getValue() == null) {
                showWarning("Veuillez sélectionner un coach");
                return;
            }

            if (messageTextArea.getText() == null || messageTextArea.getText().trim().isEmpty()) {
                showWarning("Veuillez saisir un message");
                return;
            }

            Optional<User> sessionUser = AppSession.getCurrentUser();
            Integer uid = sessionUser.map(User::getId).orElse(null);
            if (uid == null || uid <= 0) {
                showWarning("Vous devez être connecté pour envoyer une demande.");
                return;
            }

            // Créer la demande
            CoachingRequest request = new CoachingRequest();
            request.setUserId(uid);
            request.setCoachId(coachComboBox.getValue().getId());
            request.setMessage(messageTextArea.getText().trim());

            // Priorité
            if (urgentRadio.isSelected()) {
                request.setPriority(CoachingRequest.PRIORITY_URGENT);
            } else if (moyenRadio.isSelected()) {
                request.setPriority(CoachingRequest.PRIORITY_MEDIUM);
            } else {
                request.setPriority(CoachingRequest.PRIORITY_NORMAL);
            }

            // Champs optionnels
            if (objectifComboBox.getValue() != null) {
                request.setGoal(objectifComboBox.getValue());
            }

            if (niveauComboBox.getValue() != null) {
                request.setLevel(niveauComboBox.getValue());
            }

            if (frequenceComboBox.getValue() != null) {
                request.setFrequency(frequenceComboBox.getValue());
            }

            if (budgetTextField.getText() != null && !budgetTextField.getText().trim().isEmpty()) {
                try {
                    double budget = Double.parseDouble(budgetTextField.getText().trim().replace("€", "").trim());
                    request.setBudget(budget);
                } catch (NumberFormatException e) {
                    showWarning("Budget invalide");
                    return;
                }
            }

            // Sauvegarder
            requestService.create(request);

            showSuccess("Demande envoyée avec succès");
            clearForm();

        } catch (SQLException e) {
            showError("Erreur lors de l'envoi de la demande", e.getMessage());
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        }
    }

    private void clearForm() {
        coachComboBox.setValue(null);
        messageTextArea.clear();
        normalRadio.setSelected(true);
        objectifComboBox.setValue(null);
        niveauComboBox.setValue(null);
        frequenceComboBox.setValue(null);
        budgetTextField.clear();
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
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Charge une demande existante pour modification
     */
    public void loadRequestForUpdate(CoachingRequest request) {
        try {
            System.out.println("[CoachingRequest] Mode MODIFICATION - Demande #" + request.getId());
            
            // Charger le coach
            Optional<User> coach = userService.findById(request.getCoachId());
            if (coach.isPresent()) {
                coachComboBox.setValue(coach.get());
                System.out.println("[CoachingRequest] Coach chargé: " + coach.get().getFirstName() + " " + coach.get().getLastName());
            }

            // Remplir les champs
            messageTextArea.setText(request.getMessage());

            // Priorité
            switch (request.getPriority()) {
                case CoachingRequest.PRIORITY_URGENT -> urgentRadio.setSelected(true);
                case CoachingRequest.PRIORITY_MEDIUM -> moyenRadio.setSelected(true);
                default -> normalRadio.setSelected(true);
            }

            // Champs optionnels
            if (request.getGoal() != null) {
                objectifComboBox.setValue(request.getGoal());
            }

            if (request.getLevel() != null) {
                niveauComboBox.setValue(request.getLevel());
            }

            if (request.getFrequency() != null) {
                frequenceComboBox.setValue(request.getFrequency());
            }

            if (request.getBudget() != null) {
                budgetTextField.setText(String.valueOf(request.getBudget()));
            }

            // Changer le comportement du bouton pour la mise à jour
            submitButton.setText("Mettre à jour la demande");
            submitButton.setStyle("-fx-background-color: linear-gradient(to right, #fbbf24, #f59e0b); -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 14 28 14 28; -fx-cursor: hand;");
            submitButton.setOnAction(event -> handleUpdateSubmit(request));
            
            // Scroll vers le formulaire après un court délai
            javafx.application.Platform.runLater(() -> {
                if (submitButton.getScene() != null) {
                    submitButton.requestFocus();
                }
            });

        } catch (SQLException e) {
            showError("Erreur lors du chargement", e.getMessage());
        }
    }

    private void handleUpdateSubmit(CoachingRequest request) {
        try {
            // Validation
            if (messageTextArea.getText() == null || messageTextArea.getText().trim().isEmpty()) {
                showWarning("Veuillez saisir un message");
                return;
            }

            // Mettre à jour les champs
            request.setMessage(messageTextArea.getText().trim());

            // Priorité
            if (urgentRadio.isSelected()) {
                request.setPriority(CoachingRequest.PRIORITY_URGENT);
            } else if (moyenRadio.isSelected()) {
                request.setPriority(CoachingRequest.PRIORITY_MEDIUM);
            } else {
                request.setPriority(CoachingRequest.PRIORITY_NORMAL);
            }

            // Champs optionnels
            if (objectifComboBox.getValue() != null) {
                request.setGoal(objectifComboBox.getValue());
            }

            if (niveauComboBox.getValue() != null) {
                request.setLevel(niveauComboBox.getValue());
            }

            if (frequenceComboBox.getValue() != null) {
                request.setFrequency(frequenceComboBox.getValue());
            }

            if (budgetTextField.getText() != null && !budgetTextField.getText().trim().isEmpty()) {
                try {
                    double budget = Double.parseDouble(budgetTextField.getText().trim().replace("€", "").trim());
                    request.setBudget(budget);
                } catch (NumberFormatException e) {
                    showWarning("Budget invalide");
                    return;
                }
            }

            // Sauvegarder
            requestService.update(request);

            showSuccess("Demande mise à jour avec succès");
            
            // Retourner à la page mes demandes
            returnToMesDemandes();

        } catch (SQLException e) {
            showError("Erreur lors de la mise à jour", e.getMessage());
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        }
    }

    private void returnToMesDemandes() {
        try {
            NavigationManager.show("/user/coaching_session/mes_demandes.fxml", "DayFlow — Mes demandes");
        } catch (IOException | IllegalStateException e) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/coaching_session/mes_demandes.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) submitButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Mes demandes");
                stage.show();
            } catch (IOException ex) {
                showError("Erreur de navigation", "Impossible de retourner à la liste");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Navigation vers la page "Mes demandes"
     */
    @FXML
    private void handleMesDemandes(ActionEvent event) {
        navigateTo("/user/coaching_session/mes_demandes.fxml", "DayFlow — Mes demandes", event);
    }
    
    /**
     * Navigation vers l'accueil
     */
    @FXML
    private void handleAccueil(ActionEvent event) {
        navigateTo("/user/account/user_dashboard.fxml", "DayFlow — Accueil", event);
    }
    
    /**
     * Navigation vers les objectifs
     */
    @FXML
    private void handleObjectifs(ActionEvent event) {
        navigateTo("/user/goals_routines/goalparticipation.fxml", "DayFlow — Objectifs", event);
    }
    
    /**
     * Navigation vers la communauté
     */
    @FXML
    private void handleCommunity(ActionEvent event) {
        navigateTo("/user/interaction/posts_feed.fxml", "DayFlow — Communauté", event);
    }
    
    /**
     * Navigation vers le calendrier
     */
    @FXML
    private void handleCalendrier(ActionEvent event) {
        showInfo("Calendrier", "La page calendrier sera bientôt disponible");
    }
    
    /**
     * Navigation vers les favoris
     */
    @FXML
    private void handleFavoris(ActionEvent event) {
        showInfo("Favoris", "La page favoris sera bientôt disponible");
    }
    
    /**
     * Navigation vers les posts
     */
    @FXML
    private void handlePosts(ActionEvent event) {
        navigateTo("/user/interaction/posts_feed.fxml", "DayFlow — Posts", event);
    }
    
    /**
     * Méthode utilitaire pour la navigation
     */
    private void navigateTo(String fxmlPath, String title, ActionEvent event) {
        try {
            NavigationManager.show(fxmlPath, title);
        } catch (IOException | IllegalStateException e) {
            // Fallback si NavigationManager n'est pas initialisé
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent root = loader.load();

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle(title);
                stage.show();
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Navigation vers " + fxmlPath + " échouée", ex);
                showError("Erreur de navigation", "Impossible d'ouvrir la page demandée");
            }
        }
    }
}

package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.Priority;
import model.goals_activity_management.Goal;
import services.Goal_acitvityManagment_module.GoalService;
import session.AppSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class GoalsDashboardController {

    @FXML private FlowPane goalsContainer;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> statusFilter;
    @FXML private ChoiceBox<String> priorityFilter;
    @FXML private Label activeCountLabel;
    @FXML private Label completedCountLabel;
    @FXML private Label pausedCountLabel;
    @FXML private Label failedCountLabel;
    @FXML private Button newGoalButton;

    private final GoalService goalService = new GoalService();
    private List<Goal> allGoals;

    @FXML
    private void initialize() {
        setupFilters();
        loadGoals();
        updateStatistics();
    }

    private void setupFilters() {
        statusFilter.getItems().addAll("Tous", "draft", "active", "paused", "completed", "failed", "archived");
        statusFilter.setValue("Tous");
        
        priorityFilter.getItems().addAll("Tous", "low", "medium", "high");
        priorityFilter.setValue("Tous");
    }

    private void loadGoals() {
        try {
            // For now, load all goals - you'll need to add a method to GoalService to fetch by user
            allGoals = fetchUserGoals();
            displayGoals(allGoals);
        } catch (Exception e) {
            showError("Erreur lors du chargement des objectifs: " + e.getMessage());
        }
    }

    private List<Goal> fetchUserGoals() {
        try {
            Integer userId = AppSession.getCurrentUser()
                .map(u -> u.getId())
                .orElse(null);
            
            if (userId == null) {
                return List.of();
            }
            
            return goalService.findByUserId(userId);
        } catch (SQLException e) {
            showError("Erreur lors du chargement: " + e.getMessage());
            return List.of();
        }
    }

    private void displayGoals(List<Goal> goals) {
        goalsContainer.getChildren().clear();
        
        for (Goal goal : goals) {
            VBox card = createGoalCard(goal);
            goalsContainer.getChildren().add(card);
        }
    }

    private VBox createGoalCard(Goal goal) {
        VBox card = new VBox(12);
        card.getStyleClass().add("goal-card");
        card.setPrefWidth(280);
        card.setPadding(new Insets(20));
        card.setOnMouseClicked(e -> onViewGoalDetails(goal));
        card.setStyle("-fx-cursor: hand;");

        // Icon
        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("goal-icon-container");
        iconContainer.setPrefSize(50, 50);
        Label icon = new Label("🎯");
        icon.getStyleClass().add("goal-icon");
        iconContainer.getChildren().add(icon);

        // Title and Favorite
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label title = new Label(goal.getTitle());
        title.getStyleClass().add("goal-title");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);
        
        Button favoriteBtn = new Button(goal.isFavorite() ? "❤️" : "🤍");
        favoriteBtn.getStyleClass().add("favorite-button");
        favoriteBtn.setOnAction(e -> {
            e.consume();
            toggleGoalFavorite(goal, favoriteBtn);
        });
        
        titleRow.getChildren().addAll(title, favoriteBtn);

        // Description
        Label description = new Label(goal.getDescription() != null ? goal.getDescription() : "");
        description.getStyleClass().add("goal-description");
        description.setWrapText(true);
        description.setMaxHeight(60);

        // Badges
        HBox badges = new HBox(8);
        Label statusBadge = new Label(goal.getStatus().toUpperCase());
        statusBadge.getStyleClass().add("status-badge-" + goal.getStatus());
        badges.getChildren().add(statusBadge);
        
        if (goal.getPriority() != null) {
            Label priorityBadge = new Label(goal.getPriority());
            priorityBadge.getStyleClass().add("priority-badge-" + goal.getPriority());
            badges.getChildren().add(priorityBadge);
        }

        // Progress
        Label progress = new Label("Progression " + goal.getProgress() + "%");
        progress.getStyleClass().add("progress-label");

        // Dates
        HBox dates = new HBox(15);
        dates.getStyleClass().add("goal-dates");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dates.getChildren().addAll(
            new Label("📅 " + goal.getStartDate().format(formatter)),
            new Label("📅 " + goal.getEndDate().format(formatter)),
            new Label("🔁 " + goal.getRoutines().size() + " routine(s)")
        );

        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button modifyBtn = new Button("✏️ Modifier");
        modifyBtn.getStyleClass().add("action-button-modify");
        modifyBtn.setOnAction(e -> onEditGoal(goal));
        
        Button duplicateBtn = new Button("📋 Dupliquer");
        duplicateBtn.getStyleClass().add("action-button-duplicate");
        duplicateBtn.setOnAction(e -> onDuplicateGoal(goal));
        
        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.getStyleClass().add("action-button-delete");
        deleteBtn.setOnAction(e -> onDeleteGoal(goal));
        
        actions.getChildren().addAll(modifyBtn, duplicateBtn, deleteBtn);

        card.getChildren().addAll(iconContainer, titleRow, description, badges, progress, dates, actions);
        return card;
    }

    private void toggleGoalFavorite(Goal goal, Button favoriteBtn) {
        try {
            goal.setFavorite(!goal.isFavorite());
            goalService.update(goal);
            favoriteBtn.setText(goal.isFavorite() ? "❤️" : "🤍");
        } catch (SQLException e) {
            showError("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    @FXML
    private void onApplyFilters() {
        String search = searchField.getText().toLowerCase();
        String status = statusFilter.getValue();
        String priority = priorityFilter.getValue();

        List<Goal> filtered = allGoals.stream()
            .filter(g -> search.isEmpty() || g.getTitle().toLowerCase().contains(search) || 
                        (g.getDescription() != null && g.getDescription().toLowerCase().contains(search)))
            .filter(g -> "Tous".equals(status) || g.getStatus().equals(status))
            .filter(g -> "Tous".equals(priority) || (g.getPriority() != null && g.getPriority().equals(priority)))
            .collect(Collectors.toList());

        displayGoals(filtered);
    }

    @FXML
    private void onNewGoal() {
        showGoalForm(null);
    }

    private void onEditGoal(Goal goal) {
        showGoalForm(goal);
    }

    private void onDuplicateGoal(Goal goal) {
        try {
            Goal duplicate = new Goal();
            duplicate.setTitle(goal.getTitle() + " (Copie)");
            duplicate.setDescription(goal.getDescription());
            duplicate.setStartDate(LocalDate.now());
            duplicate.setEndDate(LocalDate.now().plusDays(30));
            duplicate.setStatus("draft");
            duplicate.setPriority(goal.getPriority());
            duplicate.setProgress(0);
            
            // Set the current user
            AppSession.getCurrentUser().ifPresent(duplicate::setUser);
            
            goalService.insert(duplicate);
            loadGoals();
            updateStatistics();
            showInfo("Objectif dupliqué avec succès!");
        } catch (SQLException e) {
            showError("Erreur lors de la duplication: " + e.getMessage());
        }
    }

    private void onDeleteGoal(Goal goal) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'objectif");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cet objectif ?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    goalService.delete(goal.getId());
                    loadGoals();
                    updateStatistics();
                    showInfo("Objectif supprimé avec succès!");
                } catch (SQLException e) {
                    showError("Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    private void showGoalForm(Goal goal) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Goals_Routines/goal-form.fxml"));
            VBox formRoot = loader.load();
            
            // Get form controls
            TextField titleField = (TextField) formRoot.lookup("#titleField");
            TextArea descriptionField = (TextArea) formRoot.lookup("#descriptionField");
            DatePicker startDatePicker = (DatePicker) formRoot.lookup("#startDatePicker");
            DatePicker endDatePicker = (DatePicker) formRoot.lookup("#endDatePicker");
            ChoiceBox<String> priorityChoice = (ChoiceBox<String>) formRoot.lookup("#priorityChoice");
            ChoiceBox<String> statusChoice = (ChoiceBox<String>) formRoot.lookup("#statusChoice");
            Button saveButton = (Button) formRoot.lookup("#saveButton");
            Button cancelButton = (Button) formRoot.lookup("#cancelButton");
            
            // Setup choice boxes
            priorityChoice.getItems().addAll("low", "medium", "high");
            statusChoice.getItems().addAll("draft", "active", "paused", "completed", "failed");
            
            // Validation en temps réel
            titleField.textProperty().addListener((obs, old, newVal) -> {
                utils.FormValidator.validateTextField(titleField, 
                    utils.FormValidator.Validators.lengthBetween(3, 255),
                    "Le titre doit contenir entre 3 et 255 caractères");
                updateSaveButtonState(saveButton, titleField, startDatePicker, endDatePicker);
            });
            
            descriptionField.textProperty().addListener((obs, old, newVal) -> {
                utils.FormValidator.validateTextArea(descriptionField,
                    utils.FormValidator.Validators.maxLength(1000),
                    "La description ne peut pas dépasser 1000 caractères");
            });
            
            startDatePicker.valueProperty().addListener((obs, old, newVal) -> {
                utils.FormValidator.validateDatePicker(startDatePicker,
                    utils.FormValidator.Validators.notNull(),
                    "La date de début est obligatoire");
                if (endDatePicker.getValue() != null) {
                    utils.FormValidator.validateDatePicker(endDatePicker,
                        date -> date != null && !date.isBefore(newVal),
                        "La date de fin ne peut pas être avant la date de début");
                }
                updateSaveButtonState(saveButton, titleField, startDatePicker, endDatePicker);
            });
            
            endDatePicker.valueProperty().addListener((obs, old, newVal) -> {
                if (startDatePicker.getValue() != null) {
                    utils.FormValidator.validateDatePicker(endDatePicker,
                        date -> date != null && !date.isBefore(startDatePicker.getValue()),
                        "La date de fin ne peut pas être avant la date de début");
                }
                updateSaveButtonState(saveButton, titleField, startDatePicker, endDatePicker);
            });
            
            // Populate if editing
            if (goal != null) {
                titleField.setText(goal.getTitle());
                descriptionField.setText(goal.getDescription());
                startDatePicker.setValue(goal.getStartDate());
                endDatePicker.setValue(goal.getEndDate());
                priorityChoice.setValue(goal.getPriority());
                statusChoice.setValue(goal.getStatus());
                Label formTitle = (Label) formRoot.lookup(".form-title");
                if (formTitle != null) formTitle.setText("Modifier l'Objectif");
            } else {
                priorityChoice.setValue("medium");
                statusChoice.setValue("draft");
                startDatePicker.setValue(LocalDate.now());
                endDatePicker.setValue(LocalDate.now().plusDays(30));
            }
            
            // Initial validation
            updateSaveButtonState(saveButton, titleField, startDatePicker, endDatePicker);
            
            // Create dialog
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(goal == null ? "Nouvel Objectif" : "Modifier l'Objectif");
            dialog.setScene(new Scene(formRoot));
            
            // Save button action
            saveButton.setOnAction(e -> {
                try {
                    Goal goalToSave = goal != null ? goal : new Goal();
                    goalToSave.setTitle(titleField.getText());
                    goalToSave.setDescription(descriptionField.getText());
                    goalToSave.setStartDate(startDatePicker.getValue());
                    goalToSave.setEndDate(endDatePicker.getValue());
                    goalToSave.setPriority(priorityChoice.getValue());
                    goalToSave.setStatus(statusChoice.getValue());
                    
                    if (goal == null) {
                        // Set current user for new goals
                        AppSession.getCurrentUser().ifPresent(goalToSave::setUser);
                        goalService.insert(goalToSave);
                    } else {
                        goalToSave.onUpdate();
                        goalService.update(goalToSave);
                    }
                    
                    dialog.close();
                    loadGoals();
                    updateStatistics();
                    showInfo("Objectif enregistré avec succès!");
                } catch (IllegalArgumentException ex) {
                    showError(ex.getMessage());
                } catch (SQLException ex) {
                    showError("Erreur base de données: " + ex.getMessage());
                }
            });
            
            cancelButton.setOnAction(e -> dialog.close());
            
            dialog.showAndWait();
        } catch (IOException e) {
            showError("Erreur lors de l'ouverture du formulaire: " + e.getMessage());
        }
    }

    private void updateSaveButtonState(Button saveButton, TextField titleField, 
                                       DatePicker startDatePicker, DatePicker endDatePicker) {
        boolean isValid = titleField.getText() != null && 
                         titleField.getText().trim().length() >= 3 &&
                         titleField.getText().length() <= 255 &&
                         startDatePicker.getValue() != null &&
                         endDatePicker.getValue() != null &&
                         (startDatePicker.getValue() == null || endDatePicker.getValue() == null ||
                          !endDatePicker.getValue().isBefore(startDatePicker.getValue()));
        
        saveButton.setDisable(!isValid);
        if (!isValid) {
            saveButton.setStyle("-fx-opacity: 0.5;");
        } else {
            saveButton.setStyle("");
        }
    }

    private void updateStatistics() {
        long active = allGoals.stream().filter(g -> "active".equals(g.getStatus())).count();
        long completed = allGoals.stream().filter(g -> "completed".equals(g.getStatus())).count();
        long paused = allGoals.stream().filter(g -> "paused".equals(g.getStatus())).count();
        long failed = allGoals.stream().filter(g -> "failed".equals(g.getStatus())).count();
        
        activeCountLabel.setText(String.valueOf(active));
        completedCountLabel.setText(String.valueOf(completed));
        pausedCountLabel.setText(String.valueOf(paused));
        failedCountLabel.setText(String.valueOf(failed));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.showAndWait();
    }

    private void onViewGoalDetails(Goal goal) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Goals_Routines/routine-details.fxml"));
            javafx.scene.Parent root = loader.load();
            
            RoutineDetailsController controller = loader.getController();
            controller.setGoal(goal);
            
            javafx.stage.Stage stage = (javafx.stage.Stage) goalsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur lors de l'ouverture des détails: " + e.getMessage());
        }
    }
}

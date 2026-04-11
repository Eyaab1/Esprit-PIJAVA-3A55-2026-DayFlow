package controllers;

import controllers.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.goals_activity_management.Goal;
import model.goals_activity_management.Routine;
import services.Goal_acitvityManagment_module.GoalService;
import services.Goal_acitvityManagment_module.RoutineService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RoutineDetailsController {

    @FXML private Label goalTitleLabel;
    @FXML private Label goalDescriptionLabel;
    @FXML private Label goalStatusBadge;
    @FXML private Label goalProgressLabel;
    @FXML private Label goalStartDateLabel;
    @FXML private Label goalEndDateLabel;
    @FXML private Label routineCountLabel;
    @FXML private FlowPane routinesContainer;

    private Goal currentGoal;
    private final RoutineService routineService = new RoutineService();
    private final GoalService goalService = new GoalService();
    private List<Routine> routines = new ArrayList<>();

    public void setGoal(Goal goal) {
        this.currentGoal = goal;
        loadGoalDetails();
        loadRoutines();
    }

    private void loadGoalDetails() {
        if (currentGoal == null) return;

        goalTitleLabel.setText(currentGoal.getTitle());
        goalDescriptionLabel.setText(currentGoal.getDescription() != null ? currentGoal.getDescription() : "");
        goalStatusBadge.setText(currentGoal.getStatus().toUpperCase());
        goalStatusBadge.getStyleClass().clear();
        goalStatusBadge.getStyleClass().add("status-badge-" + currentGoal.getStatus());
        
        goalProgressLabel.setText(currentGoal.getProgress() + "%");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        goalStartDateLabel.setText(currentGoal.getStartDate().format(formatter));
        goalEndDateLabel.setText(currentGoal.getEndDate().format(formatter));
    }

    private void loadRoutines() {
        try {
            routines = fetchRoutinesForGoal(currentGoal.getId());
            displayRoutines();
            routineCountLabel.setText("(" + routines.size() + ")");
        } catch (Exception e) {
            showError("Erreur lors du chargement des routines: " + e.getMessage());
        }
    }

    private List<Routine> fetchRoutinesForGoal(int goalId) throws SQLException {
        // TODO: Add method to RoutineService to fetch by goal_id
        return new ArrayList<>();
    }

    private void displayRoutines() {
        routinesContainer.getChildren().clear();
        
        for (Routine routine : routines) {
            VBox card = createRoutineCard(routine);
            routinesContainer.getChildren().add(card);
        }
    }

    private VBox createRoutineCard(Routine routine) {
        VBox card = new VBox(12);
        card.getStyleClass().add("routine-card");
        card.setPrefWidth(280);
        card.setPadding(new Insets(20));
        card.setOnMouseClicked(e -> onViewRoutineDetails(routine));

        // Icon
        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("goal-icon-container");
        iconContainer.setPrefSize(50, 50);
        Label icon = new Label("🔁");
        icon.getStyleClass().add("goal-icon");
        iconContainer.getChildren().add(icon);

        // Title and Favorite
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label title = new Label(routine.getTitle());
        title.getStyleClass().add("goal-title");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);
        
        Button favoriteBtn = new Button(routine.isFavorite() ? "❤️" : "🤍");
        favoriteBtn.getStyleClass().add("favorite-button");
        favoriteBtn.setOnAction(e -> {
            e.consume();
            toggleRoutineFavorite(routine, favoriteBtn);
        });
        
        titleRow.getChildren().addAll(title, favoriteBtn);

        // Description
        Label description = new Label(routine.getDescription() != null ? routine.getDescription() : "");
        description.getStyleClass().add("goal-description");
        description.setWrapText(true);
        description.setMaxHeight(60);

        // Badges
        HBox badges = new HBox(8);
        Label statusBadge = new Label(routine.getStatus().toUpperCase());
        statusBadge.getStyleClass().add("status-badge-" + routine.getStatus());
        badges.getChildren().add(statusBadge);
        
        if (routine.getPriority() != null) {
            Label priorityBadge = new Label(routine.getPriority());
            priorityBadge.getStyleClass().add("priority-badge-" + routine.getPriority());
            badges.getChildren().add(priorityBadge);
        }

        // Activity count
        Label activityCount = new Label("🎯 " + routine.getActivities().size() + " activité(s)");
        activityCount.getStyleClass().add("progress-label");

        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button modifyBtn = new Button("✏️ Modifier");
        modifyBtn.getStyleClass().add("action-button-modify");
        modifyBtn.setOnAction(e -> {
            e.consume();
            onEditRoutine(routine);
        });
        
        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.getStyleClass().add("action-button-delete");
        deleteBtn.setOnAction(e -> {
            e.consume();
            onDeleteRoutine(routine);
        });
        
        actions.getChildren().addAll(modifyBtn, deleteBtn);

        card.getChildren().addAll(iconContainer, titleRow, description, badges, activityCount, actions);
        return card;
    }

    private void toggleRoutineFavorite(Routine routine, Button favoriteBtn) {
        try {
            routine.setFavorite(!routine.isFavorite());
            routineService.update(routine);
            favoriteBtn.setText(routine.isFavorite() ? "❤️" : "🤍");
        } catch (SQLException e) {
            showError("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    @FXML
    private void onBackToGoal() {
        try {
            NavigationManager.show("/Goals_Routines/goals-dashboard.fxml", "DayFlow — Mes Objectifs");
        } catch (IOException e) {
            showError("Erreur de navigation: " + e.getMessage());
        }
    }

    @FXML
    private void onEditGoal() {
        // Reuse goal form from dashboard
        showInfo("Modification de l'objectif — à implémenter");
    }

    @FXML
    private void onDeleteGoal() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'objectif");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cet objectif et toutes ses routines ?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    goalService.delete(currentGoal.getId());
                    onBackToGoal();
                } catch (SQLException e) {
                    showError("Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onNewRoutine() {
        showRoutineForm(null);
    }

    private void onEditRoutine(Routine routine) {
        showRoutineForm(routine);
    }

    private void onDeleteRoutine(Routine routine) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la routine");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette routine ?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    routineService.delete(routine.getId());
                    loadRoutines();
                    showInfo("Routine supprimée avec succès!");
                } catch (SQLException e) {
                    showError("Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    private void showRoutineForm(Routine routine) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Goals_Routines/routine-form.fxml"));
            VBox formRoot = loader.load();
            
            // Get form controls
            TextField titleField = (TextField) formRoot.lookup("#titleField");
            TextArea descriptionField = (TextArea) formRoot.lookup("#descriptionField");
            ChoiceBox<String> visibilityChoice = (ChoiceBox<String>) formRoot.lookup("#visibilityChoice");
            ChoiceBox<String> priorityChoice = (ChoiceBox<String>) formRoot.lookup("#priorityChoice");
            ChoiceBox<String> statusChoice = (ChoiceBox<String>) formRoot.lookup("#statusChoice");
            DatePicker deadlinePicker = (DatePicker) formRoot.lookup("#deadlinePicker");
            Button saveButton = (Button) formRoot.lookup("#saveButton");
            Button cancelButton = (Button) formRoot.lookup("#cancelButton");
            
            // Setup choice boxes
            visibilityChoice.getItems().addAll("public", "private");
            priorityChoice.getItems().addAll("low", "medium", "high");
            statusChoice.getItems().addAll("draft", "active", "paused", "completed", "skipped");
            
            // Populate if editing
            if (routine != null) {
                titleField.setText(routine.getTitle());
                descriptionField.setText(routine.getDescription());
                visibilityChoice.setValue(routine.getVisibility());
                priorityChoice.setValue(routine.getPriority());
                statusChoice.setValue(routine.getStatus());
                deadlinePicker.setValue(routine.getDeadline());
                Label formTitle = (Label) formRoot.lookup(".form-title");
                if (formTitle != null) formTitle.setText("Modifier la Routine");
            } else {
                visibilityChoice.setValue("private");
                priorityChoice.setValue("medium");
                statusChoice.setValue("draft");
            }
            
            // Create dialog
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(routine == null ? "Nouvelle Routine" : "Modifier la Routine");
            dialog.setScene(new Scene(formRoot));
            
            // Save button action
            saveButton.setOnAction(e -> {
                try {
                    Routine routineToSave = routine != null ? routine : new Routine();
                    routineToSave.setTitle(titleField.getText());
                    routineToSave.setDescription(descriptionField.getText());
                    routineToSave.setVisibility(visibilityChoice.getValue());
                    routineToSave.setPriority(priorityChoice.getValue());
                    routineToSave.setStatus(statusChoice.getValue());
                    routineToSave.setDeadline(deadlinePicker.getValue());
                    routineToSave.setGoal(currentGoal);
                    
                    if (routine == null) {
                        routineService.insert(routineToSave);
                    } else {
                        routineToSave.onUpdate();
                        routineService.update(routineToSave);
                    }
                    
                    dialog.close();
                    loadRoutines();
                    showInfo("Routine enregistrée avec succès!");
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

    private void onViewRoutineDetails(Routine routine) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Goals_Routines/activity-details.fxml"));
            javafx.scene.Parent root = loader.load();
            
            ActivityDetailsController controller = loader.getController();
            controller.setRoutine(routine);
            
            Stage stage = (Stage) routinesContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur lors de l'ouverture des détails: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.showAndWait();
    }
}

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
import model.goals_activity_management.Activity;
import model.goals_activity_management.Routine;
import services.Goal_acitvityManagment_module.ActivityService;
import services.Goal_acitvityManagment_module.RoutineService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ActivityDetailsController {

    @FXML private Label routineTitleLabel;
    @FXML private Label routineDescriptionLabel;
    @FXML private Label routineStatusBadge;
    @FXML private Label routineVisibilityLabel;
    @FXML private Label routinePriorityLabel;
    @FXML private Label activityCountLabel;
    @FXML private Label activityCountLabel2;
    @FXML private FlowPane activitiesContainer;

    private Routine currentRoutine;
    private final ActivityService activityService = new ActivityService();
    private final RoutineService routineService = new RoutineService();
    private List<Activity> activities = new ArrayList<>();

    public void setRoutine(Routine routine) {
        this.currentRoutine = routine;
        loadRoutineDetails();
        loadActivities();
    }

    private void loadRoutineDetails() {
        if (currentRoutine == null) return;

        routineTitleLabel.setText(currentRoutine.getTitle());
        routineDescriptionLabel.setText(currentRoutine.getDescription() != null ? currentRoutine.getDescription() : "");
        routineStatusBadge.setText(currentRoutine.getStatus().toUpperCase());
        routineStatusBadge.getStyleClass().clear();
        routineStatusBadge.getStyleClass().add("status-badge-" + currentRoutine.getStatus());
        
        routineVisibilityLabel.setText(currentRoutine.getVisibility());
        routinePriorityLabel.setText(currentRoutine.getPriority() != null ? currentRoutine.getPriority() : "N/A");
    }

    private void loadActivities() {
        try {
            activities = fetchActivitiesForRoutine(currentRoutine.getId());
            displayActivities();
            activityCountLabel.setText(String.valueOf(activities.size()));
            activityCountLabel2.setText("(" + activities.size() + ")");
        } catch (Exception e) {
            showError("Erreur lors du chargement des activités: " + e.getMessage());
        }
    }

    private List<Activity> fetchActivitiesForRoutine(int routineId) throws SQLException {
        return activityService.findByRoutineId(routineId);
    }

    private void displayActivities() {
        activitiesContainer.getChildren().clear();
        
        for (Activity activity : activities) {
            VBox card = createActivityCard(activity);
            activitiesContainer.getChildren().add(card);
        }
    }

    private VBox createActivityCard(Activity activity) {
        VBox card = new VBox(12);
        card.getStyleClass().add("activity-card");
        card.setPrefWidth(280);
        card.setPadding(new Insets(20));

        // Icon
        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("goal-icon-container");
        iconContainer.setPrefSize(50, 50);
        Label icon = new Label("⏱️");
        icon.getStyleClass().add("goal-icon");
        iconContainer.getChildren().add(icon);

        // Title and Favorite
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label title = new Label(activity.getTitle());
        title.getStyleClass().add("goal-title");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);
        
        Button favoriteBtn = new Button(activity.isFavorite() ? "❤️" : "🤍");
        favoriteBtn.getStyleClass().add("favorite-button");
        favoriteBtn.setOnAction(e -> {
            e.consume();
            toggleActivityFavorite(activity, favoriteBtn);
        });
        
        titleRow.getChildren().addAll(title, favoriteBtn);

        // Time info
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        Label timeInfo = new Label("🕐 " + activity.getStartTime().format(timeFormatter) + 
                                   " • " + activity.getDurationInMinutes() + " min");
        timeInfo.getStyleClass().add("goal-description");

        // Status badge
        HBox badges = new HBox(8);
        Label statusBadge = new Label(activity.getStatus().toUpperCase());
        statusBadge.getStyleClass().add("activity-status-" + activity.getStatus());
        badges.getChildren().add(statusBadge);
        
        if (activity.getPriority() != null) {
            Label priorityBadge = new Label(activity.getPriority());
            priorityBadge.getStyleClass().add("priority-badge-" + activity.getPriority());
            badges.getChildren().add(priorityBadge);
        }

        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button modifyBtn = new Button("✏️ Modifier");
        modifyBtn.getStyleClass().add("action-button-modify");
        modifyBtn.setOnAction(e -> onEditActivity(activity));
        
        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.getStyleClass().add("action-button-delete");
        deleteBtn.setOnAction(e -> onDeleteActivity(activity));
        
        actions.getChildren().addAll(modifyBtn, deleteBtn);

        card.getChildren().addAll(iconContainer, titleRow, timeInfo, badges, actions);
        return card;
    }

    private void toggleActivityFavorite(Activity activity, Button favoriteBtn) {
        try {
            activity.setFavorite(!activity.isFavorite());
            activityService.update(activity);
            favoriteBtn.setText(activity.isFavorite() ? "❤️" : "🤍");
        } catch (SQLException e) {
            showError("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    @FXML
    private void onBackToRoutine() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Goals_Routines/routine-details.fxml"));
            javafx.scene.Parent root = loader.load();
            
            RoutineDetailsController controller = loader.getController();
            controller.setGoal(currentRoutine.getGoal());
            
            Stage stage = (Stage) activitiesContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur de navigation: " + e.getMessage());
        }
    }

    @FXML
    private void onEditRoutine() {
        showInfo("Modification de la routine — à implémenter");
    }

    @FXML
    private void onDeleteRoutine() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la routine");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette routine et toutes ses activités ?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    routineService.delete(currentRoutine.getId());
                    onBackToRoutine();
                } catch (SQLException e) {
                    showError("Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onNewActivity() {
        showActivityForm(null);
    }

    private void onEditActivity(Activity activity) {
        showActivityForm(activity);
    }

    private void onDeleteActivity(Activity activity) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'activité");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette activité ?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    activityService.delete(activity.getId());
                    loadActivities();
                    showInfo("Activité supprimée avec succès!");
                } catch (SQLException e) {
                    showError("Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    private void showActivityForm(Activity activity) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Goals_Routines/activity-form.fxml"));
            VBox formRoot = loader.load();
            
            // Get form controls
            TextField titleField = (TextField) formRoot.lookup("#titleField");
            TextField startTimeField = (TextField) formRoot.lookup("#startTimeField");
            TextField durationField = (TextField) formRoot.lookup("#durationField");
            ChoiceBox<String> priorityChoice = (ChoiceBox<String>) formRoot.lookup("#priorityChoice");
            ChoiceBox<String> statusChoice = (ChoiceBox<String>) formRoot.lookup("#statusChoice");
            DatePicker deadlinePicker = (DatePicker) formRoot.lookup("#deadlinePicker");
            CheckBox hasReminderCheck = (CheckBox) formRoot.lookup("#hasReminderCheck");
            Button saveButton = (Button) formRoot.lookup("#saveButton");
            Button cancelButton = (Button) formRoot.lookup("#cancelButton");
            
            // Setup choice boxes
            priorityChoice.getItems().addAll("low", "medium", "high");
            statusChoice.getItems().addAll("pending", "in_progress", "completed", "skipped", "cancelled");
            
            // Validation en temps réel
            titleField.textProperty().addListener((obs, old, newVal) -> {
                utils.FormValidator.validateTextField(titleField,
                    utils.FormValidator.Validators.lengthBetween(3, 255),
                    "Le titre doit contenir entre 3 et 255 caractères");
                updateActivitySaveButtonState(saveButton, titleField, startTimeField, durationField);
            });
            
            startTimeField.textProperty().addListener((obs, old, newVal) -> {
                utils.FormValidator.validateTextField(startTimeField,
                    utils.FormValidator.Validators.isTimeFormat(),
                    "Format: HH:mm (ex: 09:30)");
                updateActivitySaveButtonState(saveButton, titleField, startTimeField, durationField);
            });
            
            durationField.textProperty().addListener((obs, old, newVal) -> {
                utils.FormValidator.validateTextField(durationField,
                    utils.FormValidator.Validators.isPositiveNumber(),
                    "La durée doit être un nombre positif (en minutes)");
                updateActivitySaveButtonState(saveButton, titleField, startTimeField, durationField);
            });
            
            // Populate if editing
            if (activity != null) {
                titleField.setText(activity.getTitle());
                startTimeField.setText(activity.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                durationField.setText(String.valueOf(activity.getDurationInMinutes()));
                priorityChoice.setValue(activity.getPriority());
                statusChoice.setValue(activity.getStatus());
                deadlinePicker.setValue(activity.getDeadline());
                hasReminderCheck.setSelected(activity.isHasReminder());
                Label formTitle = (Label) formRoot.lookup(".form-title");
                if (formTitle != null) formTitle.setText("Modifier l'Activité");
            } else {
                priorityChoice.setValue("medium");
                statusChoice.setValue("pending");
                startTimeField.setText("09:00");
                durationField.setText("30");
            }
            
            // Initial validation
            updateActivitySaveButtonState(saveButton, titleField, startTimeField, durationField);
            
            // Create dialog
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(activity == null ? "Nouvelle Activité" : "Modifier l'Activité");
            dialog.setScene(new Scene(formRoot));
            
            // Save button action
            saveButton.setOnAction(e -> {
                try {
                    Activity activityToSave = activity != null ? activity : new Activity();
                    activityToSave.setTitle(titleField.getText());
                    
                    // Parse time
                    String[] timeParts = startTimeField.getText().split(":");
                    LocalDateTime startTime = LocalDateTime.now()
                        .withHour(Integer.parseInt(timeParts[0]))
                        .withMinute(Integer.parseInt(timeParts[1]));
                    activityToSave.setStartTime(startTime);
                    
                    // Parse duration
                    int minutes = Integer.parseInt(durationField.getText());
                    activityToSave.setDuration(LocalTime.of(minutes / 60, minutes % 60));
                    
                    activityToSave.setPriority(priorityChoice.getValue());
                    activityToSave.setStatus(statusChoice.getValue());
                    activityToSave.setDeadline(deadlinePicker.getValue());
                    activityToSave.setHasReminder(hasReminderCheck.isSelected());
                    activityToSave.setRoutine(currentRoutine);
                    
                    if (activity == null) {
                        activityService.insert(activityToSave);
                    } else {
                        activityToSave.onUpdate();
                        activityService.update(activityToSave);
                    }
                    
                    dialog.close();
                    loadActivities();
                    showInfo("Activité enregistrée avec succès!");
                } catch (IllegalArgumentException ex) {
                    showError(ex.getMessage());
                } catch (SQLException ex) {
                    showError("Erreur base de données: " + ex.getMessage());
                } catch (Exception ex) {
                    showError("Erreur: " + ex.getMessage());
                }
            });
            
            cancelButton.setOnAction(e -> dialog.close());
            
            dialog.showAndWait();
        } catch (IOException e) {
            showError("Erreur lors de l'ouverture du formulaire: " + e.getMessage());
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

    private void updateActivitySaveButtonState(Button saveButton, TextField titleField, 
                                               TextField startTimeField, TextField durationField) {
        boolean isValid = titleField.getText() != null && 
                         titleField.getText().trim().length() >= 3 &&
                         titleField.getText().length() <= 255 &&
                         startTimeField.getText() != null &&
                         startTimeField.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$") &&
                         durationField.getText() != null &&
                         durationField.getText().matches("\\d+") &&
                         Integer.parseInt(durationField.getText()) > 0;
        
        saveButton.setDisable(!isValid);
        if (!isValid) {
            saveButton.setStyle("-fx-opacity: 0.5;");
        } else {
            saveButton.setStyle("");
        }
    }
}

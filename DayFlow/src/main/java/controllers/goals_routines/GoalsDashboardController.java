package controllers.goals_routines;

import controllers.navigation.NavigationManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import model.goals_activity_management.Goal;
import model.user.User;
import services.goals_routines.GoalService;
import services.goals_routines.GoalService.GoalListRow;
import services.goals_routines.GoalService.GoalStatusCounts;
import services.chatroom.GoalChatroomLifecycleService;
import session.AppSession;
import session.ChatroomNav;
import session.GoalNav;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class GoalsDashboardController {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private final GoalService goalService = new GoalService();
    private final GoalChatroomLifecycleService lifecycle = new GoalChatroomLifecycleService();

    private List<GoalListRow> allRows = new ArrayList<>();
    private Integer editingGoalId;
    private boolean gridView;

    @FXML
    private Label statActiveLabel;
    @FXML
    private Label statCompletedLabel;
    @FXML
    private Label statPausedLabel;
    @FXML
    private Label statFailedLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private VBox goalsListBox;
    @FXML
    private FlowPane goalsGridPane;
    @FXML
    private Button createGoalBtn;
    @FXML
    private Button listViewBtn;
    @FXML
    private Button gridViewBtn;
    @FXML
    private VBox goalFormPanel;
    @FXML
    private Label goalFormTitle;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private DatePicker deadlineDatePicker;
    @FXML
    private Spinner<Integer> deadlineHourSpinner;
    @FXML
    private Spinner<Integer> deadlineMinuteSpinner;
    @FXML
    private Spinner<Integer> deadlineSecondSpinner;
    @FXML
    private CheckBox emailReminderCheck;
    @FXML
    private DatePicker reminderDatePicker;
    @FXML
    private Spinner<Integer> reminderHourSpinner;
    @FXML
    private Spinner<Integer> reminderMinuteSpinner;
    @FXML
    private Spinner<Integer> reminderSecondSpinner;
    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private void initialize() {
        if (AppSession.getCurrentUser().isEmpty()) {
            createGoalBtn.setDisable(true);
        }
        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "Tous", "Actif", "Terminé", "En pause", "Échoué"));
        statusFilterCombo.getSelectionModel().selectFirst();

        sortCombo.setItems(FXCollections.observableArrayList(
                "Plus récent", "Titre (A-Z)"));
        sortCombo.getSelectionModel().selectFirst();

        setupForm();
        setViewMode(false);
        refreshAll();
    }

    private void refreshAll() {
        try {
            allRows = goalService.findAllForDashboard();
            GoalStatusCounts c = goalService.countGoalsByStatus();
            bindStats(c);
            applyFilters();
        } catch (SQLException e) {
            goalsListBox.getChildren().clear();
            goalsListBox.getChildren().add(new Label("Erreur : " + e.getMessage()));
        }
    }

    private void bindStats(GoalStatusCounts c) {
        statActiveLabel.setText(String.valueOf(c.active()));
        statCompletedLabel.setText(String.valueOf(c.completed()));
        statPausedLabel.setText(String.valueOf(c.paused()));
        statFailedLabel.setText(String.valueOf(c.failed()));
    }

    @FXML
    private void onApplyFilters() {
        applyFilters();
    }

    private void applyFilters() {
        goalsListBox.getChildren().clear();
        goalsGridPane.getChildren().clear();
        String q = searchField.getText() != null ? searchField.getText().trim().toLowerCase(Locale.ROOT) : "";
        String st = statusFilterCombo.getSelectionModel().getSelectedItem();
        String sort = sortCombo.getSelectionModel().getSelectedItem();

        List<GoalListRow> rows = new ArrayList<>(allRows);
        rows.removeIf(r -> {
            Goal g = r.goal();
            if (!q.isEmpty()) {
                String title = g.getTitle() != null ? g.getTitle().toLowerCase(Locale.ROOT) : "";
                String desc = g.getDescription() != null ? g.getDescription().toLowerCase(Locale.ROOT) : "";
                if (!title.contains(q) && !desc.contains(q)) {
                    return true;
                }
            }
            if (st != null && !"Tous".equals(st)) {
                String gs = g.getStatus() != null ? g.getStatus().toLowerCase(Locale.ROOT) : "";
                boolean ok = switch (st) {
                    case "Actif" -> "active".equals(gs);
                    case "Terminé" -> "completed".equals(gs);
                    case "En pause" -> "paused".equals(gs);
                    case "Échoué" -> "failed".equals(gs);
                    default -> true;
                };
                if (!ok) {
                    return true;
                }
            }
            return false;
        });

        Comparator<GoalListRow> cmp;
        if ("Titre (A-Z)".equals(sort)) {
            cmp = Comparator.comparing(r -> r.goal().getTitle() != null ? r.goal().getTitle().toLowerCase(Locale.FRENCH) : "");
        } else {
            cmp = Comparator.comparing((GoalListRow r) -> r.goal().getCreatedAt() != null ? r.goal().getCreatedAt() : java.time.LocalDateTime.MIN).reversed();
        }
        rows.sort(cmp);

        for (GoalListRow row : rows) {
            if (gridView) {
                goalsGridPane.getChildren().add(buildGoalGridCard(row));
            } else {
                goalsListBox.getChildren().add(buildGoalCard(row));
            }
        }
        if (rows.isEmpty()) {
            Label empty = new Label("Aucun objectif ne correspond à ces critères.");
            if (gridView) {
                goalsGridPane.getChildren().add(empty);
            } else {
                goalsListBox.getChildren().add(empty);
            }
        }
    }

    private VBox buildGoalCard(GoalListRow row) {
        Goal g = row.goal();
        VBox card = new VBox(12);
        card.getStyleClass().add("goal-row-card");

        HBox head = new HBox(12);
        head.setAlignment(Pos.TOP_LEFT);
        Label icon = new Label("🎯");
        icon.setStyle("-fx-font-size: 22px;");
        VBox textCol = new VBox(6);
        Label title = new Label(g.getTitle());
        title.getStyleClass().add("goal-row-title");
        title.setWrapText(true);
        String desc = g.getDescription() != null ? g.getDescription() : "";
        if (desc.length() > 140) {
            desc = desc.substring(0, 137) + "…";
        }
        Label dLbl = new Label(desc);
        dLbl.setWrapText(true);
        dLbl.getStyleClass().add("goal-row-desc");
        textCol.getChildren().addAll(title, dLbl);
        HBox.setHgrow(textCol, Priority.ALWAYS);
        
        // Add edit and delete icons
        int goalId = g.getId();
        HBox actionIcons = new HBox(8);
        actionIcons.setAlignment(Pos.CENTER_RIGHT);
        
        Button editBtn = new Button("✏️");
        editBtn.getStyleClass().add("btn-icon-edit");
        editBtn.setStyle("-fx-font-size: 16px; -fx-padding: 4px 8px;");
        editBtn.setOnAction(e -> onEditGoal(goalId));
        
        Button duplicateBtn = new Button("📋");
        duplicateBtn.getStyleClass().add("btn-icon-duplicate");
        duplicateBtn.setStyle("-fx-font-size: 16px; -fx-padding: 4px 8px;");
        duplicateBtn.setOnAction(e -> onDuplicateGoal(goalId));
        
        Button deleteBtn = new Button("🗑️");
        deleteBtn.getStyleClass().add("btn-icon-delete");
        deleteBtn.setStyle("-fx-font-size: 16px; -fx-padding: 4px 8px;");
        deleteBtn.setOnAction(e -> onDeleteGoal(goalId));
        
        actionIcons.getChildren().addAll(editBtn, duplicateBtn, deleteBtn);
        head.getChildren().addAll(icon, textCol, actionIcons);

        HBox badges = new HBox(8);
        badges.getStyleClass().add("goal-row-meta");
        Label st = new Label(g.getStatus() != null ? g.getStatus().toUpperCase(Locale.FRENCH) : "");
        st.getStyleClass().addAll("badge-goal-status", statusBadgeClass(g.getStatus()));
        badges.getChildren().add(st);
        if (g.getPriority() != null) {
            Label pr = new Label(g.getPriority().toLowerCase(Locale.FRENCH));
            pr.getStyleClass().add("badge-priority");
            badges.getChildren().add(pr);
        }

        // Add deadline countdown
        if (g.getDeadline() != null) {
            Label deadlineLabel = createDeadlineLabel(g);
            badges.getChildren().add(deadlineLabel);
        }

        int prog = g.getProgress();
        Label progLbl = new Label("Progression " + prog + "%");
        progLbl.getStyleClass().add("goal-progress-line");
        ProgressBar bar = new ProgressBar(prog / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("goal-progress-bar");

        Button join = new Button("Rejoindre");
        join.getStyleClass().add("btn-join");
        join.setOnAction(e -> onJoin(goalId));

        Button chat = new Button("Chatroom");
        chat.getStyleClass().add("btn-chat");
        chat.setOnAction(e -> onChatroom(goalId));

        Button open = new Button("Détails & routines");
        open.getStyleClass().add("btn-open-detail");
        open.setOnAction(e -> openGoalDetail(goalId));

        HBox actions = new HBox(10, join, chat, open);
        actions.getStyleClass().add("goal-row-actions");

        card.getChildren().addAll(head, badges, progLbl, bar, actions);
        return card;
    }

    private Label createDeadlineLabel(Goal g) {
        java.time.LocalDateTime deadline = g.getDeadline();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now, deadline);
        long hoursRemaining = java.time.temporal.ChronoUnit.HOURS.between(now, deadline);
        
        String deadlineText;
        String styleClass = "badge-deadline";
        
        if (daysRemaining < 0) {
            // Overdue
            deadlineText = "⏰ OVERDUE";
            styleClass = "badge-deadline-overdue";
        } else if (daysRemaining == 0) {
            // Today
            if (hoursRemaining <= 0) {
                deadlineText = "⏰ TODAY (expired)";
                styleClass = "badge-deadline-critical";
            } else {
                deadlineText = "⏰ TODAY (" + hoursRemaining + "h left)";
                styleClass = "badge-deadline-critical";
            }
        } else if (daysRemaining == 1) {
            deadlineText = "⏰ TOMORROW";
            styleClass = "badge-deadline-urgent";
        } else if (daysRemaining <= 3) {
            deadlineText = "⏰ Still " + daysRemaining + " days to end";
            styleClass = "badge-deadline-urgent";
        } else if (daysRemaining <= 7) {
            deadlineText = "⏰ Still " + daysRemaining + " days to end";
            styleClass = "badge-deadline-warning";
        } else {
            deadlineText = "⏰ Still " + daysRemaining + " days to end";
            styleClass = "badge-deadline-normal";
        }
        
        Label deadlineLabel = new Label(deadlineText);
        deadlineLabel.getStyleClass().addAll("badge", styleClass);
        return deadlineLabel;
    }

    private static String statusBadgeClass(String status) {
        if (status == null) {
            return "badge-other";
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "active" -> "badge-active";
            case "completed" -> "badge-completed";
            case "paused" -> "badge-paused";
            case "failed" -> "badge-failed";
            case "draft" -> "badge-draft";
            case "archived" -> "badge-archived";
            default -> "badge-other";
        };
    }

    private void openGoalDetail(int goalId) {
        GoalNav.setSelectedGoalId(goalId);
        try {
            NavigationManager.show("/user/goals_routines/goal_detail.fxml", "DayFlow — Objectif");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void onJoin(int goalId) {
        Optional<Integer> uid = AppSession.getCurrentUser().map(u -> u.getId());
        if (uid.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Connectez-vous pour rejoindre un objectif.");
            return;
        }
        try {
            String msg = lifecycle.requestJoin(goalId, uid.get());
            alert(Alert.AlertType.INFORMATION, msg);
            refreshAll();
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, ex.getMessage());
        }
    }

    private void onChatroom(int goalId) {
        ChatroomNav.setOpenGoalId(goalId);
        try {
            NavigationManager.show("/user/chatroom/chatroom_hub.fxml", "DayFlow — Chat");
        } catch (IOException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void onCreateGoal() {
        Optional<Integer> uid = AppSession.getCurrentUser().map(u -> u.getId());
        if (uid.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Connectez-vous pour créer un objectif.");
            return;
        }
        editingGoalId = null;
        clearForm();
        goalFormTitle.setText("Créer un objectif");
        statusCombo.getItems().setAll("active", "draft");
        statusCombo.getSelectionModel().select("active");
        goalFormPanel.setVisible(true);
        goalFormPanel.setManaged(true);
    }

    private static void alert(Alert.AlertType t, String m) {
        new Alert(t, m).showAndWait();
    }

    private void onEditGoal(int goalId) {
        try {
            Optional<GoalListRow> goalOpt = goalService.findAllForDashboard().stream()
                    .filter(row -> row.goal().getId() == goalId)
                    .findFirst();
            
            if (goalOpt.isEmpty()) {
                alert(Alert.AlertType.ERROR, "Objectif non trouvé.");
                return;
            }
            
            Goal g = goalOpt.get().goal();
            editingGoalId = goalId;
            goalFormTitle.setText("Modifier l'objectif");
            titleField.setText(g.getTitle() != null ? g.getTitle() : "");
            descriptionField.setText(g.getDescription() != null ? g.getDescription() : "");
            startDatePicker.setValue(g.getStartDate());
            endDatePicker.setValue(g.getEndDate());
            if (g.getDeadline() != null) {
                deadlineDatePicker.setValue(g.getDeadline().toLocalDate());
                deadlineHourSpinner.getValueFactory().setValue(g.getDeadline().getHour());
                deadlineMinuteSpinner.getValueFactory().setValue(g.getDeadline().getMinute());
                deadlineSecondSpinner.getValueFactory().setValue(g.getDeadline().getSecond());
            } else {
                deadlineDatePicker.setValue(null);
                deadlineHourSpinner.getValueFactory().setValue(12);
                deadlineMinuteSpinner.getValueFactory().setValue(0);
                deadlineSecondSpinner.getValueFactory().setValue(0);
            }
            emailReminderCheck.setSelected(g.isEmailReminderEnabled());
            if (g.getEmailReminderAt() != null) {
                reminderDatePicker.setValue(g.getEmailReminderAt().toLocalDate());
                reminderHourSpinner.getValueFactory().setValue(g.getEmailReminderAt().getHour());
                reminderMinuteSpinner.getValueFactory().setValue(g.getEmailReminderAt().getMinute());
                reminderSecondSpinner.getValueFactory().setValue(g.getEmailReminderAt().getSecond());
            } else {
                reminderDatePicker.setValue(null);
                reminderHourSpinner.getValueFactory().setValue(9);
                reminderMinuteSpinner.getValueFactory().setValue(0);
                reminderSecondSpinner.getValueFactory().setValue(0);
            }
            statusCombo.getItems().setAll("active", "draft", "paused", "completed", "failed", "archived");
            statusCombo.getSelectionModel().select(g.getStatus());
            updateReminderInputsEnabled();
            goalFormPanel.setVisible(true);
            goalFormPanel.setManaged(true);
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage());
        }
    }

    private void onDeleteGoal(int goalId) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Supprimer l'objectif");
        confirmDialog.setHeaderText("Êtes-vous sûr?");
        confirmDialog.setContentText("Cette action est irréversible. Tous les données associées seront supprimées.");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                goalService.delete(goalId);
                alert(Alert.AlertType.INFORMATION, "Objectif supprimé avec succès.");
                refreshAll();
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage());
            }
        }
    }

    private void onDuplicateGoal(int goalId) {
        try {
            Optional<GoalListRow> goalOpt = goalService.findAllForDashboard().stream()
                    .filter(row -> row.goal().getId() == goalId)
                    .findFirst();
            
            if (goalOpt.isEmpty()) {
                alert(Alert.AlertType.ERROR, "Objectif non trouvé.");
                return;
            }
            
            Goal originalGoal = goalOpt.get().goal();
            Optional<Integer> uid = AppSession.getCurrentUser().map(u -> u.getId());
            
            if (uid.isEmpty()) {
                alert(Alert.AlertType.WARNING, "Connectez-vous pour dupliquer un objectif.");
                return;
            }
            
            // Create a new goal with the same data
            Goal newGoal = new Goal();
            newGoal.setTitle(originalGoal.getTitle() + " (Copie)");
            newGoal.setDescription(originalGoal.getDescription());
            newGoal.setStartDate(originalGoal.getStartDate());
            newGoal.setEndDate(originalGoal.getEndDate());
            newGoal.setStatus("draft"); // New goals start as draft
            newGoal.setPriority(originalGoal.getPriority());
            newGoal.setProgress(0); // Reset progress
            newGoal.setRequiredTasks(originalGoal.getRequiredTasks());
            
            // Recalculate deadline: add 7 days to original deadline
            if (originalGoal.getDeadline() != null) {
                java.time.LocalDateTime newDeadline = originalGoal.getDeadline().plusDays(7);
                newGoal.setDeadline(newDeadline);
            }
            
            User owner = new User();
            owner.setId(uid.get());
            newGoal.setUser(owner);
            newGoal.onUpdate();
            newGoal.validate();
            
            // Insert the new goal
            goalService.insert(newGoal);
            
            // Create chatroom and participation for the new goal
            lifecycle.ensureChatroomAndOwner(newGoal.getId(), uid.get());
            
            alert(Alert.AlertType.INFORMATION, "Objectif dupliqué avec succès. La copie a été créée en tant que brouillon.");
            refreshAll();
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage());
        }
    }

    private static Spinner<Integer> createTimeSpinner(int min, int max, int initialValue) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initialValue);
        spinner.setEditable(true);
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                (SpinnerValueFactory.IntegerSpinnerValueFactory) spinner.getValueFactory();
        valueFactory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                if (value == null) {
                    return String.format("%02d", min);
                }
                return String.format("%02d", value);
            }

            @Override
            public Integer fromString(String text) {
                if (text == null || text.isBlank()) {
                    return min;
                }
                int parsed = Integer.parseInt(text.trim());
                if (parsed < min) {
                    return min;
                }
                if (parsed > max) {
                    return max;
                }
                return parsed;
            }
        });
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d{0,2}")) {
                return change;
            }
            return null;
        };
        spinner.getEditor().setTextFormatter(new TextFormatter<>(filter));
        spinner.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                try {
                    int value = Integer.parseInt(spinner.getEditor().getText());
                    valueFactory.setValue(Math.max(min, Math.min(max, value)));
                } catch (Exception ignored) {
                    valueFactory.setValue(initialValue);
                }
                spinner.getEditor().setText(valueFactory.getConverter().toString(valueFactory.getValue()));
            }
        });
        spinner.getEditor().setText(valueFactory.getConverter().toString(valueFactory.getValue()));
        return spinner;
    }

    private VBox buildGoalGridCard(GoalListRow row) {
        Goal g = row.goal();
        VBox card = buildGoalCard(row);
        card.getStyleClass().add("goal-grid-card");
        card.setPrefWidth(340);
        return card;
    }

    @FXML
    private void onSaveGoal() {
        Optional<Integer> uid = AppSession.getCurrentUser().map(User::getId);
        if (uid.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Connectez-vous pour enregistrer un objectif.");
            return;
        }
        try {
            Goal g = editingGoalId == null ? new Goal() : goalService.findById(editingGoalId);
            if (g == null) {
                throw new IllegalArgumentException("Objectif non trouvé.");
            }
            g.setTitle(titleField.getText() != null ? titleField.getText().trim() : "");
            g.setDescription(descriptionField.getText() != null ? descriptionField.getText().trim() : null);
            g.setStartDate(startDatePicker.getValue());
            g.setEndDate(endDatePicker.getValue());

            if (deadlineDatePicker.getValue() != null) {
                g.setDeadline(java.time.LocalDateTime.of(
                        deadlineDatePicker.getValue(),
                        java.time.LocalTime.of(
                                deadlineHourSpinner.getValue(),
                                deadlineMinuteSpinner.getValue(),
                                deadlineSecondSpinner.getValue())
                ));
            } else {
                g.setDeadline(null);
            }
            g.setEmailReminderEnabled(emailReminderCheck.isSelected());
            if (emailReminderCheck.isSelected()) {
                if (reminderDatePicker.getValue() == null) {
                    throw new IllegalArgumentException("Veuillez choisir la date du rappel email.");
                }
                g.setEmailReminderAt(java.time.LocalDateTime.of(
                        reminderDatePicker.getValue(),
                        java.time.LocalTime.of(
                                reminderHourSpinner.getValue(),
                                reminderMinuteSpinner.getValue(),
                                reminderSecondSpinner.getValue())
                ));
            } else {
                g.setEmailReminderAt(null);
            }
            g.setStatus(statusCombo.getValue());
            g.onUpdate();
            g.validate();
            if (editingGoalId == null) {
                User owner = new User();
                owner.setId(uid.get());
                g.setUser(owner);
                goalService.insert(g);
                lifecycle.ensureChatroomAndOwner(g.getId(), uid.get());
                alert(Alert.AlertType.INFORMATION, "Objectif créé.");
            } else {
                goalService.update(g);
                alert(Alert.AlertType.INFORMATION, "Objectif modifié.");
            }
            onCancelGoal();
            refreshAll();
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, ex.getMessage());
        }
    }

    @FXML
    private void onCancelGoal() {
        editingGoalId = null;
        clearForm();
        goalFormPanel.setVisible(false);
        goalFormPanel.setManaged(false);
    }

    @FXML
    private void onClearDeadline() {
        deadlineDatePicker.setValue(null);
    }

    @FXML
    private void onListView() {
        setViewMode(false);
        applyFilters();
    }

    @FXML
    private void onGridView() {
        setViewMode(true);
        applyFilters();
    }

    private void setViewMode(boolean gridMode) {
        this.gridView = gridMode;
        goalsListBox.setVisible(!gridMode);
        goalsListBox.setManaged(!gridMode);
        goalsGridPane.setVisible(gridMode);
        goalsGridPane.setManaged(gridMode);
        listViewBtn.getStyleClass().remove("view-toggle-active");
        gridViewBtn.getStyleClass().remove("view-toggle-active");
        if (gridMode) {
            gridViewBtn.getStyleClass().add("view-toggle-active");
        } else {
            listViewBtn.getStyleClass().add("view-toggle-active");
        }
    }

    private void setupForm() {
        statusCombo.setItems(FXCollections.observableArrayList("active", "draft"));
        statusCombo.getSelectionModel().select("active");
        emailReminderCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateReminderInputsEnabled();
            if (newVal && reminderDatePicker.getValue() == null) {
                reminderDatePicker.setValue(deadlineDatePicker.getValue() != null
                        ? deadlineDatePicker.getValue()
                        : java.time.LocalDate.now());
            }
        });
        deadlineDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (emailReminderCheck.isSelected() && reminderDatePicker.getValue() == null && newVal != null) {
                reminderDatePicker.setValue(newVal);
            }
        });
        updateReminderInputsEnabled();
        goalFormPanel.setVisible(false);
        goalFormPanel.setManaged(false);
        clearForm();
    }

    private void updateReminderInputsEnabled() {
        boolean enabled = emailReminderCheck.isSelected();
        reminderDatePicker.setDisable(!enabled);
        reminderHourSpinner.setDisable(!enabled);
        reminderMinuteSpinner.setDisable(!enabled);
        reminderSecondSpinner.setDisable(!enabled);
        if (!enabled) {
            reminderDatePicker.setValue(null);
        }
    }

    private void clearForm() {
        titleField.clear();
        descriptionField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        deadlineDatePicker.setValue(null);
        deadlineHourSpinner.setValueFactory(createTimeSpinner(0, 23, 12).getValueFactory());
        deadlineMinuteSpinner.setValueFactory(createTimeSpinner(0, 59, 0).getValueFactory());
        deadlineSecondSpinner.setValueFactory(createTimeSpinner(0, 59, 0).getValueFactory());
        emailReminderCheck.setSelected(false);
        reminderDatePicker.setValue(null);
        reminderHourSpinner.setValueFactory(createTimeSpinner(0, 23, 9).getValueFactory());
        reminderMinuteSpinner.setValueFactory(createTimeSpinner(0, 59, 0).getValueFactory());
        reminderSecondSpinner.setValueFactory(createTimeSpinner(0, 59, 0).getValueFactory());
        statusCombo.getSelectionModel().select("active");
    }
}

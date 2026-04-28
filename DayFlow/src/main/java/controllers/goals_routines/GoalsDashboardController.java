package controllers.goals_routines;

import controllers.navigation.NavigationManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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

public class GoalsDashboardController {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private final GoalService goalService = new GoalService();
    private final GoalChatroomLifecycleService lifecycle = new GoalChatroomLifecycleService();

    private List<GoalListRow> allRows = new ArrayList<>();

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
    private Button createGoalBtn;

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
            goalsListBox.getChildren().add(buildGoalCard(row));
        }
        if (rows.isEmpty()) {
            goalsListBox.getChildren().add(new Label("Aucun objectif ne correspond à ces critères."));
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

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouvel objectif");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleF = new TextField();
        titleF.setPromptText("Titre");
        TextArea descF = new TextArea();
        descF.setPromptText("Description");
        descF.setPrefRowCount(4);
        descF.setWrapText(true);
        DatePicker start = new DatePicker();
        DatePicker end = new DatePicker();
        DatePicker deadlineDate = new DatePicker();
        Spinner<Integer> deadlineHour = new Spinner<>(0, 23, 12);
        Spinner<Integer> deadlineMinute = new Spinner<>(0, 59, 0);
        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll("active", "draft");
        status.getSelectionModel().select("active");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        int r = 0;
        grid.add(new Label("Titre *"), 0, r);
        grid.add(titleF, 1, r++);
        grid.add(new Label("Description"), 0, r);
        grid.add(descF, 1, r++);
        grid.add(new Label("Début *"), 0, r);
        grid.add(start, 1, r++);
        grid.add(new Label("Fin *"), 0, r);
        grid.add(end, 1, r++);
        grid.add(new Label("Deadline (optionnel)"), 0, r);
        javafx.scene.layout.HBox deadlineBox = new javafx.scene.layout.HBox(5);
        deadlineBox.getChildren().addAll(deadlineDate, new Label("à"), deadlineHour, new Label("h"), deadlineMinute);
        grid.add(deadlineBox, 1, r++);
        grid.add(new Label("Statut"), 0, r);
        grid.add(status, 1, r);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }
        try {
            Goal g = new Goal();
            g.setTitle(titleF.getText() != null ? titleF.getText().trim() : "");
            g.setDescription(descF.getText() != null ? descF.getText().trim() : null);
            g.setStartDate(start.getValue());
            g.setEndDate(end.getValue());
            
            // Set deadline if provided
            if (deadlineDate.getValue() != null) {
                java.time.LocalDateTime deadline = java.time.LocalDateTime.of(
                    deadlineDate.getValue(),
                    java.time.LocalTime.of(deadlineHour.getValue(), deadlineMinute.getValue())
                );
                g.setDeadline(deadline);
            }
            
            g.setStatus(status.getValue());
            User owner = new User();
            owner.setId(uid.get());
            g.setUser(owner);
            g.onUpdate();
            g.validate();
            goalService.insert(g);
            lifecycle.ensureChatroomAndOwner(g.getId(), uid.get());
            alert(Alert.AlertType.INFORMATION, "Objectif créé. Vous êtes administrateur du chatroom associé.");
            refreshAll();
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, ex.getMessage());
        }
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
            
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Modifier l'objectif");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            TextField titleF = new TextField(g.getTitle());
            TextArea descF = new TextArea(g.getDescription() != null ? g.getDescription() : "");
            descF.setPrefRowCount(4);
            descF.setWrapText(true);
            DatePicker start = new DatePicker(g.getStartDate());
            DatePicker end = new DatePicker(g.getEndDate());
            ComboBox<String> status = new ComboBox<>();
            status.getItems().addAll("active", "draft", "paused", "completed", "failed", "archived");
            status.getSelectionModel().select(g.getStatus());

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));
            int r = 0;
            grid.add(new Label("Titre *"), 0, r);
            grid.add(titleF, 1, r++);
            grid.add(new Label("Description"), 0, r);
            grid.add(descF, 1, r++);
            grid.add(new Label("Début *"), 0, r);
            grid.add(start, 1, r++);
            grid.add(new Label("Fin *"), 0, r);
            grid.add(end, 1, r++);
            grid.add(new Label("Statut"), 0, r);
            grid.add(status, 1, r);
            dialog.getDialogPane().setContent(grid);

            Optional<ButtonType> res = dialog.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) {
                return;
            }
            
            g.setTitle(titleF.getText() != null ? titleF.getText().trim() : "");
            g.setDescription(descF.getText() != null ? descF.getText().trim() : null);
            g.setStartDate(start.getValue());
            g.setEndDate(end.getValue());
            g.setStatus(status.getValue());
            g.onUpdate();
            g.validate();
            goalService.update(g);
            alert(Alert.AlertType.INFORMATION, "Objectif modifié avec succès.");
            refreshAll();
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
}

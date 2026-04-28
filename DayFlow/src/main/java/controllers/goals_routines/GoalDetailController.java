package controllers.goals_routines;

import controllers.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.goals_activity_management.Goal;
import model.goals_activity_management.Routine;
import services.chatroom.GoalParticipationService;
import services.goals_routines.ActivityService;
import services.goals_routines.GoalService;
import services.goals_routines.RoutineService;
import session.AppSession;
import session.ChatroomNav;
import session.GoalNav;
import session.RoutineNav;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GoalDetailController {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private final GoalService goalService = new GoalService();
    private final RoutineService routineService = new RoutineService();
    private final ActivityService activityService = new ActivityService();
    private final GoalParticipationService participationService = new GoalParticipationService();

    private int goalId;
    private boolean canManageGoal;

    @FXML
    private Hyperlink backLink;
    @FXML
    private Label goalTitleLabel;
    @FXML
    private Label statusBadge;
    @FXML
    private Label goalDescLabel;
    @FXML
    private Label progressPctLabel;
    @FXML
    private ProgressBar goalProgressBar;
    @FXML
    private Label startDateLabel;
    @FXML
    private Label endDateLabel;
    @FXML
    private Button openChatBtn;
    @FXML
    private Label routinesCountLabel;
    @FXML
    private VBox routinesEmptyBox;
    @FXML
    private VBox routinesListBox;
    @FXML
    private Button editGoalBtn;
    @FXML
    private Button deleteGoalBtn;
    @FXML
    private Button newRoutineBtn;

    @FXML
    private void initialize() {
        Integer gid = GoalNav.pullSelectedGoalId();
        if (gid == null) {
            goBackToDashboard();
            return;
        }
        goalId = gid;

        backLink.setOnAction(e -> goBackToDashboard());

        try {
            reload();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            goBackToDashboard();
        }
    }

    private void goBackToDashboard() {
        try {
            NavigationManager.show("/user/goals_routines/goals_dashboard.fxml", "DayFlow — Objectifs");
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private void reload() throws SQLException {
        Goal g = goalService.findById(goalId);
        if (g == null) {
            new Alert(Alert.AlertType.WARNING, "Objectif introuvable.").showAndWait();
            goBackToDashboard();
            return;
        }

        goalTitleLabel.setText(g.getTitle());
        goalDescLabel.setText(g.getDescription() != null ? g.getDescription() : "");

        String st = g.getStatus() != null ? g.getStatus().toUpperCase(Locale.FRENCH) : "";
        statusBadge.setText(st);
        statusBadge.getStyleClass().removeIf(s -> s.startsWith("gd-badge-"));
        statusBadge.getStyleClass().add(switch (g.getStatus() != null ? g.getStatus().toLowerCase(Locale.ROOT) : "") {
            case "active" -> "gd-badge-active";
            case "paused" -> "gd-badge-paused";
            case "completed" -> "gd-badge-done";
            case "failed" -> "gd-badge-fail";
            default -> "gd-badge-other";
        });

        int prog = g.getProgress();
        progressPctLabel.setText(prog + "%");
        goalProgressBar.setProgress(prog / 100.0);

        startDateLabel.setText(g.getStartDate() != null ? g.getStartDate().format(DF) : "—");
        endDateLabel.setText(g.getEndDate() != null ? g.getEndDate().format(DF) : "—");

        canManageGoal = false;
        Optional<Integer> uid = AppSession.getCurrentUser().map(u -> u.getId());
        if (uid.isPresent()) {
            canManageGoal = participationService.isOwnerOrAdmin(uid.get(), goalId);
        }
        if (editGoalBtn != null) {
            editGoalBtn.setVisible(canManageGoal);
            editGoalBtn.setManaged(canManageGoal);
        }
        if (deleteGoalBtn != null) {
            deleteGoalBtn.setVisible(canManageGoal);
            deleteGoalBtn.setManaged(canManageGoal);
        }
        if (newRoutineBtn != null) {
            newRoutineBtn.setDisable(!canManageGoal);
        }

        List<Routine> routines = routineService.findByGoalId(goalId);
        routinesCountLabel.setText("Routines (" + routines.size() + ")");

        boolean empty = routines.isEmpty();
        routinesEmptyBox.setVisible(empty);
        routinesEmptyBox.setManaged(empty);
        routinesListBox.setVisible(!empty);
        routinesListBox.setManaged(!empty);

        routinesListBox.getChildren().clear();
        for (Routine r : routines) {
            routinesListBox.getChildren().add(buildRoutineRow(r));
        }

        boolean logged = AppSession.getCurrentUser().isPresent();
        openChatBtn.setDisable(!logged);
    }

    private HBox buildRoutineRow(Routine r) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("gd-routine-item");

        VBox textCol = new VBox(4);
        Label title = new Label(r.getTitle());
        title.getStyleClass().add("gd-routine-title");
        String meta = (r.getStatus() != null ? r.getStatus() : "")
                + (r.getPriority() != null ? " · " + r.getPriority() : "");
        Label m = new Label(meta);
        m.getStyleClass().add("gd-routine-meta");
        textCol.getChildren().addAll(title, m);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Button open = new Button("Ouvrir");
        open.getStyleClass().add("btn-gd-open-routine");
        open.setOnAction(e -> openRoutineDetail(r.getId()));

        if (canManageGoal) {
            Button del = new Button("Supprimer");
            del.getStyleClass().add("btn-gd-delete-routine");
            del.setOnAction(e -> onDeleteRoutineFromList(r));
            row.getChildren().addAll(textCol, open, del);
        } else {
            row.getChildren().addAll(textCol, open);
        }
        return row;
    }

    private void onDeleteRoutineFromList(Routine r) {
        if (!canManageGoal) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Supprimer cette routine ?");
        confirm.setContentText("Les activités associées seront aussi supprimées.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }
        try {
            activityService.deleteByRoutineId(r.getId());
            routineService.delete(r.getId());
            reload();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onEditGoal() {
        if (!canManageGoal) {
            return;
        }
        Goal g;
        try {
            g = goalService.findById(goalId);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            return;
        }
        if (g == null) {
            return;
        }

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
        status.getItems().addAll("draft", "active", "paused", "completed", "failed", "archived");
        status.getSelectionModel().select(g.getStatus() != null ? g.getStatus() : "draft");
        ComboBox<String> prio = new ComboBox<>();
        prio.getItems().addAll("", "low", "medium", "high");
        if (g.getPriority() != null) {
            prio.getSelectionModel().select(g.getPriority());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        int row = 0;
        grid.add(new Label("Titre *"), 0, row);
        grid.add(titleF, 1, row++);
        grid.add(new Label("Description"), 0, row);
        grid.add(descF, 1, row++);
        grid.add(new Label("Début *"), 0, row);
        grid.add(start, 1, row++);
        grid.add(new Label("Fin *"), 0, row);
        grid.add(end, 1, row++);
        grid.add(new Label("Statut"), 0, row);
        grid.add(status, 1, row++);
        grid.add(new Label("Priorité"), 0, row);
        grid.add(prio, 1, row);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }
        try {
            g.setTitle(titleF.getText() != null ? titleF.getText().trim() : "");
            g.setDescription(descF.getText() != null ? descF.getText().trim() : null);
            g.setStartDate(start.getValue());
            g.setEndDate(end.getValue());
            g.setStatus(status.getValue());
            String pr = prio.getSelectionModel().getSelectedItem();
            if (pr != null && !pr.isBlank()) {
                g.setPriority(pr);
            } else {
                g.setPriority(null);
            }
            g.validate();
            g.onUpdate();
            goalService.update(g);
            reload();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onDeleteGoal() {
        if (!canManageGoal) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Supprimer cet objectif ?");
        confirm.setContentText("Les routines, activités, participations et le salon de discussion seront supprimés.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }
        try {
            goalService.deleteWithDependencies(goalId);
            goBackToDashboard();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void openRoutineDetail(int routineId) {
        RoutineNav.setPending(new RoutineNav.PendingRoutineDetail(routineId, goalId));
        try {
            NavigationManager.show("/user/goals_routines/routine_detail.fxml", "DayFlow — Routine");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onOpenChatroom() {
        ChatroomNav.setOpenGoalId(goalId);
        try {
            NavigationManager.show("/user/chatroom/chatroom_hub.fxml", "DayFlow — Chat");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onNewRoutine() {
        Optional<Integer> uid = AppSession.getCurrentUser().map(u -> u.getId());
        if (uid.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Connectez-vous pour créer une routine.").showAndWait();
            return;
        }
        if (!canManageGoal) {
            new Alert(Alert.AlertType.WARNING, "Seul le propriétaire (ou un administrateur) peut gérer les routines.")
                    .showAndWait();
            return;
        }

        Goal g = null;
        try {
            g = goalService.findById(goalId);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            return;
        }
        if (g == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle Routine");
        dialog.setHeaderText(null);

        TextField titleF = new TextField();
        titleF.setPromptText("Ex : Entraînement matinal");
        TextArea descF = new TextArea();
        descF.setPromptText("Décrivez votre routine…");
        descF.setPrefRowCount(4);
        descF.setWrapText(true);

        ComboBox<String> vis = new ComboBox<>();
        vis.getItems().addAll("Privé", "Public");
        vis.getSelectionModel().select("Privé");

        ComboBox<String> prio = new ComboBox<>();
        prio.getItems().addAll("", "low", "medium", "high");
        prio.setPromptText("Priorité");
        prio.getSelectionModel().select("medium");

        DatePicker deadline = new DatePicker();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        int r = 0;
        Label sub = new Label("Objectif : " + g.getTitle());
        sub.setStyle("-fx-text-fill: #ea580c; -fx-font-weight: 600;");
        grid.add(sub, 0, r++, 2, 1);
        grid.add(new Label("Titre *"), 0, r);
        grid.add(titleF, 1, r++);
        grid.add(new Label("Description"), 0, r);
        grid.add(descF, 1, r++);
        grid.add(new Label("Visibilité"), 0, r);
        grid.add(vis, 1, r++);
        grid.add(new Label("Priorité"), 0, r);
        grid.add(prio, 1, r++);
        grid.add(new Label("Deadline"), 0, r);
        grid.add(deadline, 1, r);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String tt = titleF.getText() != null ? titleF.getText().trim() : "";
            if (tt.length() < 3) {
                new Alert(Alert.AlertType.WARNING, "Le titre doit contenir au moins 3 caractères.").showAndWait();
                ev.consume();
            }
        });

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Enregistrer");
        okBtn.setStyle("-fx-background-color: #f97316; -fx-text-fill: white; -fx-font-weight: bold;");

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }

        try {
            Routine routine = new Routine();
            routine.setTitle(titleF.getText() != null ? titleF.getText().trim() : "");
            String d = descF.getText() != null ? descF.getText().trim() : "";
            if (!d.isEmpty()) {
                routine.setDescription(d);
            }
            routine.setVisibility("Privé".equals(vis.getValue()) ? "private" : "public");
            String pr = prio.getSelectionModel().getSelectedItem();
            if (pr != null && !pr.isBlank()) {
                routine.setPriority(pr);
            }
            if (deadline.getValue() != null) {
                routine.setDeadline(deadline.getValue().atStartOfDay());
            }
            routine.setStatus("active");
            Goal stub = new Goal();
            stub.setId(goalId);
            routine.setGoal(stub);
            routine.validate();
            routineService.insert(routine);
            reload();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }
}

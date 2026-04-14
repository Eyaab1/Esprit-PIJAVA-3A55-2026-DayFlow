package controllers.goals;

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
import services.Goal_acitvityManagment_module.GoalService;
import services.Goal_acitvityManagment_module.RoutineService;
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

    private int goalId;

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

        row.getChildren().addAll(textCol, open);
        return row;
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
            routine.setDeadline(deadline.getValue());
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

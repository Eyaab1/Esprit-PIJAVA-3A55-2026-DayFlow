package controllers.goals_routines;

import controllers.navigation.NavigationManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import model.goals_activity_management.Activity;
import model.goals_activity_management.Routine;
import services.goals_routines.ActivityService;
import services.goals_routines.RoutineService;
import session.AppSession;
import session.GoalNav;
import session.RoutineNav;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class RoutineDetailController {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);

    private final RoutineService routineService = new RoutineService();
    private final ActivityService activityService = new ActivityService();

    private int routineId;
    private int parentGoalId;
    private List<Activity> allActivities = new ArrayList<>();

    @FXML
    private Hyperlink backLink;
    @FXML
    private Label routineTitleLabel;
    @FXML
    private Label routineDescLabel;
    @FXML
    private Label visibilityBadge;
    @FXML
    private Button editBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Label activitiesCountLabel;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private VBox activitiesEmptyBox;
    @FXML
    private VBox activitiesListBox;

    @FXML
    private void initialize() {
        RoutineNav.PendingRoutineDetail pending = RoutineNav.pullPending();
        if (pending == null) {
            goToGoals();
            return;
        }
        routineId = pending.routineId();
        parentGoalId = pending.parentGoalId();

        backLink.setOnAction(e -> goBackToGoal());

        sortCombo.setItems(FXCollections.observableArrayList(
                "Date début (ancien)", "Date début (récent)"));
        sortCombo.getSelectionModel().selectFirst();

        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "Tous", "pending", "in_progress", "completed", "skipped", "cancelled"));
        statusFilterCombo.getSelectionModel().selectFirst();

        sortCombo.valueProperty().addListener((a, b, c) -> refreshActivityList());
        statusFilterCombo.valueProperty().addListener((a, b, c) -> refreshActivityList());

        try {
            reload();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            goBackToGoal();
        }
    }

    private void goToGoals() {
        try {
            NavigationManager.show("/user/goals_routines/goals_dashboard.fxml", "DayFlow — Objectifs");
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private void goBackToGoal() {
        GoalNav.setSelectedGoalId(parentGoalId);
        try {
            NavigationManager.show("/user/goals_routines/goal_detail.fxml", "DayFlow — Objectif");
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private void reload() throws SQLException {
        Routine r = routineService.findById(routineId);
        if (r == null) {
            new Alert(Alert.AlertType.WARNING, "Routine introuvable.").showAndWait();
            goBackToGoal();
            return;
        }

        routineTitleLabel.setText(r.getTitle());
        routineDescLabel.setText(r.getDescription() != null && !r.getDescription().isBlank()
                ? r.getDescription() : "—");

        visibilityBadge.getStyleClass().removeIf(s -> s.startsWith("rd-badge-"));
        boolean isPrivate = "private".equalsIgnoreCase(r.getVisibility());
        visibilityBadge.setText(isPrivate ? "PRIVATE" : "PUBLIC");
        visibilityBadge.getStyleClass().add(isPrivate ? "rd-badge-private" : "rd-badge-public");

        allActivities = activityService.findByRoutineId(routineId);
        activitiesCountLabel.setText("Activités (" + allActivities.size() + ")");

        boolean logged = AppSession.getCurrentUser().isPresent();
        editBtn.setDisable(!logged);
        deleteBtn.setDisable(!logged);

        refreshActivityList();
    }

    private void refreshActivityList() {
        List<Activity> rows = new ArrayList<>(allActivities);

        String st = statusFilterCombo.getSelectionModel().getSelectedItem();
        if (st != null && !"Tous".equals(st)) {
            rows.removeIf(a -> a.getStatus() == null || !st.equalsIgnoreCase(a.getStatus()));
        }

        String sort = sortCombo.getSelectionModel().getSelectedItem();
        Comparator<Activity> cmp;
        if ("Date début (récent)".equals(sort)) {
            cmp = Comparator.comparing((Activity a) -> a.getStartTime() != null ? a.getStartTime() : LocalDateTime.MIN).reversed();
        } else {
            cmp = Comparator.comparing(a -> a.getStartTime() != null ? a.getStartTime() : LocalDateTime.MAX);
        }
        rows.sort(cmp);

        activitiesListBox.getChildren().clear();
        for (Activity a : rows) {
            activitiesListBox.getChildren().add(buildActivityCard(a));
        }

        boolean empty = rows.isEmpty();
        activitiesEmptyBox.setVisible(empty);
        activitiesEmptyBox.setManaged(empty);
        activitiesListBox.setVisible(!empty);
        activitiesListBox.setManaged(!empty);
    }

    private VBox buildActivityCard(Activity a) {
        VBox card = new VBox(14);
        card.getStyleClass().add("activity-card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox head = new HBox(12);
        head.setAlignment(Pos.TOP_LEFT);
        Label title = new Label(a.getTitle());
        title.getStyleClass().add("activity-card-title");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label prioBadge = new Label();
        applyPriorityBadge(prioBadge, a);

        head.getChildren().addAll(title, prioBadge);

        HBox line1 = detailLine("📅", "Début : "
                + (a.getStartTime() != null ? a.getStartTime().format(DTF) : "—"));
        HBox line2 = detailLine("⏳", "Durée planifiée : " + formatDurationHuman(a.getDuration()));
        VBox meta = new VBox(6, line1, line2);
        meta.getStyleClass().add("activity-card-meta");

        VBox timerBox = new VBox(8);
        timerBox.getStyleClass().add("activity-timer-box");

        HBox timerHead = new HBox();
        timerHead.setAlignment(Pos.CENTER_LEFT);
        Label timerK = new Label("⏱ TIMER");
        timerK.getStyleClass().add("activity-timer-kicker");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label stateLbl = new Label("Prêt");
        stateLbl.getStyleClass().add("activity-timer-state");
        timerHead.getChildren().addAll(timerK, sp, stateLbl);

        ProgressBar pb = new ProgressBar(0);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.getStyleClass().add("activity-timer-progress");

        int totalSec = Math.max(1, localDurationToSeconds(a.getDuration()));
        Label bigTime = new Label(formatSecondsAsHms(totalSec));
        bigTime.getStyleClass().add("activity-timer-big");

        final int[] elapsed = {0};
        final Timeline[] timerRef = {null};

        Button startBtn = new Button("Démarrer");
        startBtn.getStyleClass().add("activity-btn-start");
        Button resetBtn = new Button("↻");
        resetBtn.getStyleClass().add("activity-btn-reset");
        HBox timerCtrl = new HBox(10, startBtn, resetBtn);
        timerCtrl.setAlignment(Pos.CENTER_LEFT);

        Runnable resetTimer = () -> {
            if (timerRef[0] != null) {
                timerRef[0].stop();
                timerRef[0] = null;
            }
            elapsed[0] = 0;
            pb.setProgress(0);
            bigTime.setText(formatSecondsAsHms(totalSec));
            stateLbl.setText("Prêt");
        };

        startBtn.setOnAction(ev -> {
            if (timerRef[0] != null && timerRef[0].getStatus() == Animation.Status.RUNNING) {
                return;
            }
            if (timerRef[0] != null) {
                timerRef[0].stop();
                timerRef[0] = null;
            }
            elapsed[0] = 0;
            pb.setProgress(0);
            bigTime.setText(formatSecondsAsHms(totalSec));
            stateLbl.setText("En cours");
            Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                elapsed[0]++;
                double p = Math.min(1.0, elapsed[0] / (double) totalSec);
                pb.setProgress(p);
                int rem = totalSec - elapsed[0];
                bigTime.setText(formatSecondsAsHms(Math.max(0, rem)));
                if (elapsed[0] >= totalSec) {
                    if (timerRef[0] != null) {
                        timerRef[0].stop();
                    }
                    stateLbl.setText("Terminé");
                }
            }));
            tl.setCycleCount(Timeline.INDEFINITE);
            timerRef[0] = tl;
            tl.play();
        });

        resetBtn.setOnAction(ev -> resetTimer.run());

        timerBox.getChildren().addAll(timerHead, pb, bigTime, timerCtrl);

        Label statusBadge = new Label(formatStatusUpper(a.getStatus()));
        statusBadge.getStyleClass().addAll("activity-status-badge", statusBadgeStyle(a.getStatus()));

        boolean canEdit = AppSession.getCurrentUser().isPresent();
        Button edit = new Button("Modifier");
        edit.getStyleClass().add("activity-btn-modifier");
        edit.setMinHeight(40);
        edit.setPrefHeight(40);
        edit.setMaxHeight(40);
        edit.setMinWidth(112);
        edit.setPrefWidth(112);
        edit.setMaxWidth(112);
        edit.setDisable(!canEdit);
        edit.setOnAction(ev -> showActivityDialog(a));

        Button del = new Button("Supprimer");
        del.getStyleClass().add("activity-btn-del");
        del.setMinHeight(40);
        del.setPrefHeight(40);
        del.setMaxHeight(40);
        del.setMinWidth(118);
        del.setPrefWidth(118);
        del.setMaxWidth(118);
        del.setDisable(!canEdit);
        del.setTooltip(new Tooltip("Supprimer cette activité"));
        del.setOnAction(ev -> deleteActivity(a));

        HBox actionGroup = new HBox(10, edit, del);
        actionGroup.setAlignment(Pos.CENTER_RIGHT);
        actionGroup.getStyleClass().add("activity-card-actions");

        Region footSpacer = new Region();
        HBox.setHgrow(footSpacer, Priority.ALWAYS);
        HBox footer = new HBox(12, statusBadge, footSpacer, actionGroup);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.getStyleClass().add("activity-card-footer");

        card.getChildren().addAll(head, meta, timerBox, footer);
        return card;
    }

    private static HBox detailLine(String icon, String text) {
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        Label ic = new Label(icon);
        Label tx = new Label(text);
        tx.getStyleClass().add("activity-detail-line");
        h.getChildren().addAll(ic, tx);
        return h;
    }

    private static void applyPriorityBadge(Label badge, Activity a) {
        badge.getStyleClass().removeIf(s -> s.startsWith("activity-prio"));
        if (a.getPriority() == null) {
            badge.setText("");
            badge.setVisible(false);
            badge.setManaged(false);
            return;
        }
        badge.setVisible(true);
        badge.setManaged(true);
        switch (a.getPriority().toLowerCase(Locale.ROOT)) {
            case "low" -> {
                badge.setText("Faible");
                badge.getStyleClass().addAll("activity-prio-badge", "activity-prio-low");
            }
            case "medium" -> {
                badge.setText("Moy.");
                badge.getStyleClass().add("activity-prio-badge");
            }
            case "high" -> {
                badge.setText("Élevée");
                badge.getStyleClass().addAll("activity-prio-badge", "activity-prio-high");
            }
            default -> {
                badge.setText(a.getPriority());
                badge.getStyleClass().add("activity-prio-badge");
            }
        }
    }

    private static String formatDurationHuman(LocalTime d) {
        if (d == null) {
            return "—";
        }
        int h = d.getHour();
        int m = d.getMinute();
        if (h > 0) {
            return h + "h" + String.format("%02d", m) + "min";
        }
        return m + "min";
    }

    private static int localDurationToSeconds(LocalTime d) {
        if (d == null) {
            return 60;
        }
        return d.getHour() * 3600 + d.getMinute() * 60 + d.getSecond();
    }

    private static String formatSecondsAsHms(int totalSec) {
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static String formatStatusUpper(String status) {
        if (status == null) {
            return "—";
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "pending" -> "PENDING";
            case "in_progress" -> "EN COURS";
            case "completed" -> "TERMINÉE";
            case "skipped" -> "IGNORÉE";
            case "cancelled" -> "ANNULÉE";
            default -> status.toUpperCase(Locale.FRENCH);
        };
    }

    private static String statusBadgeStyle(String status) {
        if (status == null) {
            return "activity-status-pending";
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "pending" -> "activity-status-pending";
            case "in_progress" -> "activity-status-progress";
            case "completed" -> "activity-status-done";
            case "skipped" -> "activity-status-skip";
            case "cancelled" -> "activity-status-cancel";
            default -> "activity-status-pending";
        };
    }

    private void deleteActivity(Activity a) {
        if (AppSession.getCurrentUser().isEmpty()) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Supprimer cette activité ?");
        confirm.setContentText(a.getTitle());
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }
        try {
            activityService.delete(a.getId());
            reload();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onEditRoutine() {
        if (AppSession.getCurrentUser().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Connectez-vous pour modifier.").showAndWait();
            return;
        }
        Routine r;
        try {
            r = routineService.findById(routineId);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            return;
        }
        if (r == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier la routine");

        TextField titleF = new TextField(r.getTitle());
        TextArea descF = new TextArea(r.getDescription() != null ? r.getDescription() : "");
        descF.setPrefRowCount(3);
        descF.setWrapText(true);

        ComboBox<String> vis = new ComboBox<>();
        vis.getItems().addAll("Privé", "Public");
        vis.getSelectionModel().select("private".equalsIgnoreCase(r.getVisibility()) ? "Privé" : "Public");

        ComboBox<String> prio = new ComboBox<>();
        prio.getItems().addAll("", "low", "medium", "high");
        if (r.getPriority() != null) {
            prio.getSelectionModel().select(r.getPriority());
        }

        DatePicker deadline = new DatePicker(r.getDeadline() != null ? r.getDeadline().toLocalDate() : null);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        int row = 0;
        grid.add(new Label("Titre *"), 0, row);
        grid.add(titleF, 1, row++);
        grid.add(new Label("Description"), 0, row);
        grid.add(descF, 1, row++);
        grid.add(new Label("Visibilité"), 0, row);
        grid.add(vis, 1, row++);
        grid.add(new Label("Priorité"), 0, row);
        grid.add(prio, 1, row++);
        grid.add(new Label("Deadline"), 0, row);
        grid.add(deadline, 1, row);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }

        try {
            r.setTitle(titleF.getText() != null ? titleF.getText().trim() : "");
            String d = descF.getText() != null ? descF.getText().trim() : "";
            if (!d.isEmpty()) {
                r.setDescription(d);
            }
            r.setVisibility("Privé".equals(vis.getValue()) ? "private" : "public");
            String pr = prio.getSelectionModel().getSelectedItem();
            if (pr != null && !pr.isBlank()) {
                r.setPriority(pr);
            } else {
                r.setPriority(null);
            }
            if (deadline.getValue() != null) {
                r.setDeadline(deadline.getValue().atStartOfDay());
            }
            r.validate();
            routineService.update(r);
            reload();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onDeleteRoutine() {
        if (AppSession.getCurrentUser().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Connectez-vous pour supprimer.").showAndWait();
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
            activityService.deleteByRoutineId(routineId);
            routineService.delete(routineId);
            goBackToGoal();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private static final Map<String, String> STATUS_FR_TO_DB = new LinkedHashMap<>();

    static {
        STATUS_FR_TO_DB.put("En attente", "pending");
        STATUS_FR_TO_DB.put("En cours", "in_progress");
        STATUS_FR_TO_DB.put("Terminée", "completed");
        STATUS_FR_TO_DB.put("Ignorée", "skipped");
        STATUS_FR_TO_DB.put("Annulée", "cancelled");
    }

    @FXML
    private void onNewActivity() {
        if (AppSession.getCurrentUser().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Connectez-vous pour créer une activité.").showAndWait();
            return;
        }
        showActivityDialog(null);
    }

    private void showActivityDialog(Activity existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        boolean editing = existing != null;
        dialog.setTitle(editing ? "Modifier l'activité" : "Nouvelle activité");
        dialog.setHeaderText(null);
        if (routineTitleLabel.getScene() != null && routineTitleLabel.getScene().getWindow() != null) {
            dialog.initOwner(routineTitleLabel.getScene().getWindow());
        }

        URL css = getClass().getResource("/user/goals_routines/activity_dialog.css");
        if (css != null) {
            dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        dialog.getDialogPane().getStyleClass().add("activity-dialog-pane");
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().setMinWidth(480);

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, cancelType);

        TextField titleF = new TextField();
        titleF.setPromptText("Ex : Courir 30 minutes");
        titleF.setMaxWidth(Double.MAX_VALUE);

        DatePicker startDate = new DatePicker(LocalDate.now());
        TextField startTimeF = new TextField("09:00");
        startTimeF.setPromptText("HH:mm");
        HBox startDateTime = new HBox(8, startDate, startTimeF);
        HBox.setHgrow(startDate, Priority.ALWAYS);
        startTimeF.setPrefWidth(96);

        TextField durF = new TextField("01:00");
        durF.setPromptText("HH:mm");
        durF.setPrefWidth(120);

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(STATUS_FR_TO_DB.keySet());
        statusCombo.getSelectionModel().select("En attente");

        CheckBox reminderCheck = new CheckBox("🔔 Activer un rappel pour cette activité");
        DatePicker reminderDate = new DatePicker(LocalDate.now());
        TextField reminderTimeF = new TextField("08:00");
        reminderTimeF.setPromptText("HH:mm");
        HBox reminderRow = new HBox(8, new Label("Rappel le"), reminderDate, reminderTimeF);
        reminderRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(reminderDate, Priority.ALWAYS);
        reminderTimeF.setPrefWidth(88);
        VBox reminderInner = new VBox(8, reminderCheck, reminderRow);
        reminderInner.getStyleClass().add("activity-reminder-box");
        reminderRow.setVisible(false);
        reminderRow.setManaged(false);
        reminderCheck.selectedProperty().addListener((o, old, n) -> {
            reminderRow.setVisible(Boolean.TRUE.equals(n));
            reminderRow.setManaged(Boolean.TRUE.equals(n));
            if (Boolean.TRUE.equals(n)) {
                if (reminderDate.getValue() == null) {
                    reminderDate.setValue(startDate.getValue() != null ? startDate.getValue() : LocalDate.now());
                }
            }
        });

        ComboBox<String> prioCombo = new ComboBox<>();
        prioCombo.getItems().addAll("(aucune)", "Faible", "Moyenne", "Élevée");
        prioCombo.getSelectionModel().select("(aucune)");

        DatePicker deadlinePicker = new DatePicker();
        deadlinePicker.setPromptText("dd/mm/yyyy");

        Label heading = new Label(editing ? "Modifier l'activité" : "Nouvelle activité");
        heading.getStyleClass().add("activity-dialog-heading");
        heading.setMaxWidth(Double.MAX_VALUE);

        VBox body = new VBox(18);
        body.getStyleClass().add("activity-form-body");

        Label lt = new Label("Titre *");
        lt.getStyleClass().add("activity-field-label");
        body.getChildren().addAll(lt, titleF);

        HBox rowStartDur = new HBox(20);
        rowStartDur.getStyleClass().add("activity-row-half");
        VBox colStart = new VBox(8);
        Label ls = new Label("Heure de début");
        ls.getStyleClass().add("activity-field-label");
        colStart.getChildren().addAll(ls, startDateTime);
        HBox.setHgrow(colStart, Priority.ALWAYS);
        VBox colDur = new VBox(8);
        Label ld = new Label("Durée");
        ld.getStyleClass().add("activity-field-label");
        colDur.getChildren().addAll(ld, durF);
        rowStartDur.getChildren().addAll(colStart, colDur);

        Label lst = new Label("Statut");
        lst.getStyleClass().add("activity-field-label");
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        Label lpr = new Label("Priorité");
        lpr.getStyleClass().add("activity-field-label");
        prioCombo.setMaxWidth(Double.MAX_VALUE);

        Label ldl = new Label("Deadline");
        ldl.getStyleClass().add("activity-field-label");

        body.getChildren().addAll(
                rowStartDur,
                lst, statusCombo,
                reminderInner,
                lpr, prioCombo,
                ldl, deadlinePicker);

        VBox shell = new VBox(0);
        shell.getStyleClass().add("activity-dialog-shell");
        shell.getChildren().addAll(heading, body);

        if (editing) {
            titleF.setText(existing.getTitle());
            if (existing.getStartTime() != null) {
                startDate.setValue(existing.getStartTime().toLocalDate());
                startTimeF.setText(String.format("%02d:%02d",
                        existing.getStartTime().getHour(), existing.getStartTime().getMinute()));
            }
            if (existing.getDuration() != null) {
                durF.setText(String.format("%02d:%02d",
                        existing.getDuration().getHour(), existing.getDuration().getMinute()));
            }
            statusCombo.getSelectionModel().select(statusDbToFr(existing.getStatus()));
            if (existing.isHasReminder() && existing.getReminderAt() != null) {
                reminderCheck.setSelected(true);
                reminderRow.setVisible(true);
                reminderRow.setManaged(true);
                reminderDate.setValue(existing.getReminderAt().toLocalDate());
                reminderTimeF.setText(String.format("%02d:%02d",
                        existing.getReminderAt().getHour(), existing.getReminderAt().getMinute()));
            }
            prioCombo.getSelectionModel().select(priorityFrFromDb(existing.getPriority()));
            if (existing.getDeadline() != null) {
                deadlinePicker.setValue(existing.getDeadline().toLocalDate());
            }
        }

        dialog.getDialogPane().setContent(shell);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.getStyleClass().add("activity-btn-save");
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelType);
        cancelBtn.getStyleClass().add("activity-btn-cancel");

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String tt = titleF.getText() != null ? titleF.getText().trim() : "";
            if (tt.length() < 3) {
                new Alert(Alert.AlertType.WARNING, "Le titre doit contenir au moins 3 caractères.").showAndWait();
                ev.consume();
            }
            if (reminderCheck.isSelected()) {
                if (reminderDate.getValue() == null) {
                    new Alert(Alert.AlertType.WARNING, "Choisissez une date pour le rappel.").showAndWait();
                    ev.consume();
                }
            }
        });

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || !saveType.equals(res.get())) {
            return;
        }

        try {
            LocalDate date = startDate.getValue() != null ? startDate.getValue() : LocalDate.now();
            LocalTime startT = parseHHmm(startTimeF.getText(), LocalTime.of(9, 0));
            LocalTime durationT = parseDurationHHmm(durF.getText(), LocalTime.of(1, 0));

            String statusFr = statusCombo.getSelectionModel().getSelectedItem();
            String statusDb = STATUS_FR_TO_DB.getOrDefault(statusFr, "pending");

            String prioFr = prioCombo.getSelectionModel().getSelectedItem();
            String prioDb = switch (prioFr != null ? prioFr : "") {
                case "Faible" -> "low";
                case "Moyenne" -> "medium";
                case "Élevée" -> "high";
                default -> null;
            };

            if (editing) {
                existing.setTitle(titleF.getText() != null ? titleF.getText().trim() : "");
                existing.setStartTime(LocalDateTime.of(date, startT));
                existing.setDuration(durationT);
                existing.setStatus(statusDb);
                if (prioDb != null) {
                    existing.setPriority(prioDb);
                } else {
                    existing.setPriority(null);
                }
                if (deadlinePicker.getValue() != null) {
                    existing.setDeadline(deadlinePicker.getValue().atStartOfDay());
                } else {
                    existing.setDeadline(null);
                }
                existing.setReminderAt(null);
                existing.setHasReminder(false);
                if (reminderCheck.isSelected()) {
                    LocalDate rd = reminderDate.getValue();
                    LocalTime rt = parseHHmm(reminderTimeF.getText(), LocalTime.of(8, 0));
                    existing.setReminderAt(LocalDateTime.of(rd, rt));
                    existing.setHasReminder(true);
                }
                existing.validate();
                activityService.update(existing);
            } else {
                Activity a = new Activity();
                a.setTitle(titleF.getText() != null ? titleF.getText().trim() : "");
                a.setStartTime(LocalDateTime.of(date, startT));
                a.setDuration(durationT);
                a.setStatus(statusDb);
                if (prioDb != null) {
                    a.setPriority(prioDb);
                }
                if (deadlinePicker.getValue() != null) {
                    a.setDeadline(deadlinePicker.getValue().atStartOfDay());
                }

                a.setReminderAt(null);
                a.setHasReminder(false);
                if (reminderCheck.isSelected()) {
                    LocalDate rd = reminderDate.getValue();
                    LocalTime rt = parseHHmm(reminderTimeF.getText(), LocalTime.of(8, 0));
                    a.setReminderAt(LocalDateTime.of(rd, rt));
                    a.setHasReminder(true);
                }

                Routine stub = new Routine();
                stub.setId(routineId);
                a.setRoutine(stub);
                a.validate();
                activityService.insert(a);
            }
            reload();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private static String statusDbToFr(String db) {
        if (db == null) {
            return "En attente";
        }
        for (Map.Entry<String, String> e : STATUS_FR_TO_DB.entrySet()) {
            if (e.getValue().equalsIgnoreCase(db)) {
                return e.getKey();
            }
        }
        return "En attente";
    }

    private static String priorityFrFromDb(String p) {
        if (p == null) {
            return "(aucune)";
        }
        return switch (p.toLowerCase(Locale.ROOT)) {
            case "low" -> "Faible";
            case "medium" -> "Moyenne";
            case "high" -> "Élevée";
            default -> "(aucune)";
        };
    }

    private static LocalTime parseHHmm(String raw, LocalTime fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String[] p = raw.trim().split(":");
        try {
            int h = Integer.parseInt(p[0].trim());
            int m = p.length > 1 ? Integer.parseInt(p[1].trim()) : 0;
            return LocalTime.of(Math.min(23, Math.max(0, h)), Math.min(59, Math.max(0, m)));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static LocalTime parseDurationHHmm(String raw, LocalTime fallback) {
        return parseHHmm(raw, fallback);
    }
}

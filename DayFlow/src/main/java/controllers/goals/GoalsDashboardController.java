package controllers.goals;

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
import services.Goal_acitvityManagment_module.GoalService;
import services.Goal_acitvityManagment_module.GoalService.GoalListRow;
import services.Goal_acitvityManagment_module.GoalService.GoalStatusCounts;
import services.chatroom_module.GoalChatroomLifecycleService;
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
        try {
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
        } catch (Throwable e) {
            // Capturer TOUT — y compris Error et RuntimeException
            e.printStackTrace();
            String msg = e.getClass().getName() + ": " + e.getMessage();
            if (e.getCause() != null) {
                msg += "\nCaused by: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage();
            }
            if (goalsListBox != null) {
                goalsListBox.getChildren().clear();
                goalsListBox.getChildren().add(new javafx.scene.control.Label(msg));
            }
            throw new RuntimeException(msg, e); // re-throw pour que FXMLLoader affiche le vrai message
        }
    }

    private void refreshAll() {
        try {
            allRows = goalService.findAllForDashboard();
            GoalStatusCounts c = goalService.countGoalsByStatus();
            bindStats(c);
            applyFilters();
        } catch (Exception e) {
            goalsListBox.getChildren().clear();
            goalsListBox.getChildren().add(new Label("Erreur chargement : " + e.getClass().getSimpleName() + " — " + e.getMessage()));
            e.printStackTrace();
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
        head.getChildren().addAll(icon, textCol);

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

        int prog = g.getProgress();
        Label progLbl = new Label("Progression " + prog + "%");
        progLbl.getStyleClass().add("goal-progress-line");
        ProgressBar bar = new ProgressBar(prog / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("goal-progress-bar");

        int goalId = g.getId();
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
            NavigationManager.show("/user/goals/goal_detail.fxml", "DayFlow — Objectif");
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
        grid.add(new Label("Statut"), 0, r);
        grid.add(status, 1, r);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }
        try {
            String titleText = titleF.getText() != null ? titleF.getText().trim() : "";
            String descText  = descF.getText()  != null ? descF.getText().trim()  : "";

            // Validation manuelle (title + description fusionnés → description en BD)
            if (titleText.length() < 3)
                throw new IllegalArgumentException("Le titre doit contenir au moins 3 caractères.");
            if (start.getValue() == null)
                throw new IllegalArgumentException("La date de début est obligatoire.");
            if (end.getValue() == null)
                throw new IllegalArgumentException("La date de fin est obligatoire.");
            if (!end.getValue().isAfter(start.getValue()))
                throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");

            // En BD : description = "Titre — Description" (title n'existe pas en BD)
            String fullDesc = descText.isBlank() ? titleText : titleText + " — " + descText;

            Goal g = new Goal();
            // setTitle sans validation BD (champ Java uniquement)
            g.setTitleDirect(titleText);
            g.setDescription(fullDesc);
            g.setStartDate(start.getValue());
            g.setEndDate(end.getValue());
            g.setStatus(status.getValue());

            // Lier l'utilisateur courant
            model.user.User u = new model.user.User();
            u.setId(uid.get());
            g.setUser(u);

            goalService.insert(g);
            lifecycle.ensureChatroomAndOwner(g.getId(), uid.get());
            alert(Alert.AlertType.INFORMATION, "Objectif créé ✅");
            refreshAll();
        } catch (Exception ex) {
            ex.printStackTrace();
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            alert(Alert.AlertType.ERROR,
                    cause.getClass().getSimpleName() + ": " +
                    (cause.getMessage() != null ? cause.getMessage() : "erreur inconnue"));
        }
    }

    private static void alert(Alert.AlertType t, String m) {
        new Alert(t, m).showAndWait();
    }
}

package controllers.interaction;

import controllers.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.goals_activity_management.Goal;
import model.goals_activity_management.GoalParticipation;
import model.user.User;
import services.goals_routines.GoalService;
import services.goals_routines.GoalService.GoalDiscussionRow;
import services.chatroom.GoalChatroomLifecycleService;
import session.AppSession;
import session.ChatroomNav;
import session.GoalNav;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Communauté : grille d’objectifs pour rejoindre un salon (demande au propriétaire) et ouvrir le chat une fois accepté.
 */
public class CommunityDiscussionsController {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private final GoalService goalService = new GoalService();
    private final GoalChatroomLifecycleService lifecycle = new GoalChatroomLifecycleService();

    private List<GoalDiscussionRow> allRows = new ArrayList<>();

    @FXML
    private Button backBtn;
    @FXML
    private TextField searchField;
    @FXML
    private FlowPane cardsFlow;

    @FXML
    private void initialize() {
        if (AppSession.getCurrentUser().isEmpty()) {
            cardsFlow.getChildren().add(wrapMessage("Connectez-vous pour voir les salons de discussion."));
            backBtn.setOnAction(e -> onBack());
            return;
        }
        searchField.textProperty().addListener((o, a, q) -> applyFilter(q));
        reload();
    }

    private VBox wrapMessage(String text) {
        VBox v = new VBox(new Label(text));
        v.setAlignment(Pos.CENTER_LEFT);
        v.getStyleClass().add("disc-card");
        return v;
    }

    private void reload() {
        Optional<Integer> uid = AppSession.getCurrentUser().map(User::getId);
        if (uid.isEmpty()) {
            return;
        }
        try {
            allRows = goalService.findGoalsForCommunityDiscussion(uid.get());
            applyFilter(searchField.getText());
        } catch (SQLException e) {
            cardsFlow.getChildren().clear();
            cardsFlow.getChildren().add(wrapMessage("Erreur : " + e.getMessage()));
        }
    }

    private void applyFilter(String raw) {
        String q = raw != null ? raw.trim().toLowerCase(Locale.ROOT) : "";
        cardsFlow.getChildren().clear();
        for (GoalDiscussionRow row : allRows) {
            Goal g = row.goal();
            if (!q.isEmpty()) {
                String t = g.getTitle() != null ? g.getTitle().toLowerCase(Locale.ROOT) : "";
                String d = g.getDescription() != null ? g.getDescription().toLowerCase(Locale.ROOT) : "";
                if (!t.contains(q) && !d.contains(q)) {
                    continue;
                }
            }
            cardsFlow.getChildren().add(buildCard(row));
        }
        if (cardsFlow.getChildren().isEmpty()) {
            cardsFlow.getChildren().add(wrapMessage("Aucun objectif ne correspond à votre recherche."));
        }
    }

    private VBox buildCard(GoalDiscussionRow row) {
        Goal g = row.goal();
        VBox card = new VBox(10);
        card.getStyleClass().add("disc-card");

        Label title = new Label(g.getTitle() != null ? g.getTitle() : "—");
        title.getStyleClass().add("disc-card-title");
        title.setWrapText(true);

        String ownerDisplay = ownerDisplayName(row);
        Label owner = new Label("Par " + ownerDisplay);
        owner.getStyleClass().add("disc-owner-line");

        String desc = g.getDescription() != null ? g.getDescription() : "";
        if (desc.length() > 160) {
            desc = desc.substring(0, 157) + "…";
        }
        Label dLbl = new Label(desc);
        dLbl.getStyleClass().add("disc-desc");
        dLbl.setWrapText(true);

        VBox meta = new VBox(6);
        meta.getStyleClass().add("disc-meta-box");
        String dateRange = formatDateRange(g);
        Label dates = new Label("📅  " + dateRange);
        dates.getStyleClass().add("disc-meta-line");
        Label parts = new Label("👥  " + row.participantCount() + " participant(s)");
        parts.getStyleClass().add("disc-meta-line");
        Label st = new Label("🏷  " + (g.getStatus() != null ? g.getStatus().toUpperCase(Locale.FRENCH) : ""));
        st.getStyleClass().addAll("badge-disc-status", statusBadgeClass(g.getStatus()));
        meta.getChildren().addAll(dates, parts, st);

        HBox actions = new HBox(8);
        actions.getStyleClass().add("disc-actions");
        actions.setAlignment(Pos.CENTER_LEFT);

        int goalId = g.getId();
        boolean isOwner = GoalParticipation.ROLE_OWNER.equals(row.myRole())
                && GoalParticipation.STATUS_APPROVED.equals(row.myParticipationStatus());
        boolean approved = GoalParticipation.STATUS_APPROVED.equals(row.myParticipationStatus());
        boolean pending = GoalParticipation.STATUS_PENDING.equals(row.myParticipationStatus());
        boolean rejected = GoalParticipation.STATUS_REJECTED.equals(row.myParticipationStatus());
        boolean hasRoom = row.chatroomId() != null;

        if (isOwner) {
            Label hint = new Label("Vous êtes propriétaire — validez les demandes depuis l’objectif.");
            hint.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b; -fx-wrap-text:true;");
            Button chat = new Button("💬  Chatroom");
            chat.getStyleClass().add("btn-disc-chat");
            chat.setDisable(!hasRoom);
            chat.setOnAction(e -> openChat(goalId));
            Button detail = createDetailButton(goalId);
            actions.getChildren().addAll(chat, detail);
            card.getChildren().addAll(title, owner, dLbl, meta, hint, actions);
            return card;
        }

        if (approved) {
            Button leave = new Button("🚪  Quitter");
            leave.getStyleClass().add("btn-disc-leave");
            leave.setOnAction(e -> onLeave(goalId));
            Button chat = new Button("💬  Chatroom");
            chat.getStyleClass().add("btn-disc-chat");
            chat.setDisable(!hasRoom);
            chat.setOnAction(e -> openChat(goalId));
            Button detail = createDetailButton(goalId);
            actions.getChildren().addAll(leave, chat, detail);
        } else if (pending) {
            Button wait = new Button("⏳  En attente d'approbation");
            wait.getStyleClass().add("btn-disc-pending");
            wait.setDisable(true);
            Button chatWait = new Button("💬  Chatroom (en attente)");
            chatWait.getStyleClass().add("btn-disc-chat-wait");
            chatWait.setDisable(true);
            Button detail = createDetailButton(goalId);
            actions.getChildren().addAll(wait, chatWait, detail);
        } else {
            Button join = new Button("➕  Rejoindre");
            join.getStyleClass().add("btn-disc-join");
            join.setOnAction(e -> onJoin(goalId, ownerDisplay));
            Button chatDis = new Button("💬  Chatroom");
            chatDis.getStyleClass().add("btn-disc-chat-disabled");
            chatDis.setDisable(true);
            Button detail = createDetailButton(goalId);
            actions.getChildren().addAll(join, chatDis, detail);
            if (rejected) {
                Label rej = new Label("Demande refusée — vous pouvez redemander.");
                rej.setStyle("-fx-font-size:11px; -fx-text-fill:#b45309;");
                card.getChildren().addAll(title, owner, dLbl, meta, rej, actions);
                return card;
            }
        }

        card.getChildren().addAll(title, owner, dLbl, meta, actions);
        return card;
    }

    private Button createDetailButton(int goalId) {
        Button detail = new Button("👁  Détails");
        detail.getStyleClass().add("btn-disc-detail");
        detail.setOnAction(e -> openGoalDetail(goalId));
        return detail;
    }

    private static String ownerDisplayName(GoalDiscussionRow row) {
        String fn = row.ownerFirstName() != null ? row.ownerFirstName().trim() : "";
        String ln = row.ownerLastName() != null ? row.ownerLastName().trim() : "";
        String s = (fn + " " + ln).trim();
        return s.isEmpty() ? "Propriétaire" : s;
    }

    private static String formatDateRange(Goal g) {
        String a = g.getStartDate() != null ? g.getStartDate().format(DF) : "?";
        String b = g.getEndDate() != null ? g.getEndDate().format(DF) : "?";
        return a + " — " + b;
    }

    private static String statusBadgeClass(String status) {
        if (status == null) {
            return "badge-d-other";
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "active" -> "badge-d-active";
            case "completed" -> "badge-d-completed";
            case "paused" -> "badge-d-paused";
            case "failed" -> "badge-d-failed";
            case "draft" -> "badge-d-draft";
            default -> "badge-d-other";
        };
    }

    private void onJoin(int goalId, String ownerName) {
        Optional<Integer> uid = AppSession.getCurrentUser().map(User::getId);
        if (uid.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Connectez-vous pour rejoindre.");
            return;
        }
        try {
            String msg = lifecycle.requestJoin(goalId, uid.get());
            alert(Alert.AlertType.INFORMATION,
                    msg + "\n\nLe propriétaire (" + ownerName + ") recevra votre demande et pourra l’accepter depuis la fiche objectif.");
            reload();
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, ex.getMessage());
        }
    }

    private void onLeave(int goalId) {
        Optional<Integer> uid = AppSession.getCurrentUser().map(User::getId);
        if (uid.isEmpty()) {
            return;
        }
        try {
            String msg = lifecycle.leaveGoalDiscussion(uid.get(), goalId);
            alert(Alert.AlertType.INFORMATION, msg);
            reload();
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, ex.getMessage());
        }
    }

    private void openChat(int goalId) {
        ChatroomNav.setOpenGoalId(goalId);
        try {
            NavigationManager.show("/user/chatroom/chatroom_hub.fxml", "DayFlow — Chat");
        } catch (IOException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    private void openGoalDetail(int goalId) {
        GoalNav.setSelectedGoalId(goalId);
        try {
            NavigationManager.show("/user/goals_routines/goal_detail.fxml", "DayFlow — Objectif");
        } catch (IOException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void onBack() {
        try {
            NavigationManager.show("/user/interaction/community.fxml", "DayFlow — Community");
        } catch (IOException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    private static void alert(Alert.AlertType t, String m) {
        new Alert(t, m).showAndWait();
    }
}

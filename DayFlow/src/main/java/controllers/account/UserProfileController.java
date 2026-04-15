package controllers.account;

import controllers.navigation.NavigationManager;
import enums.PostStatus;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.interaction.Post;
import model.user.User;
import services.account.UserService;
import services.chatroom.ChatroomService;
import services.chatroom.ChatroomService.ChatroomListItem;
import services.interaction.PostService;
import services.interaction.PostService.PostWithStats;
import services.interaction.SavedPostService;
import session.AppSession;
import session.ChatroomNav;
import utils.HtmlPlainText;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Page profil utilisateur : infos, statistiques posts, onglets activités (sans bloc « IA / visionary »).
 */
public class UserProfileController {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private final UserService userService = new UserService();
    private final PostService postService = new PostService();
    private final SavedPostService savedPostService = new SavedPostService();
    private final ChatroomService chatroomService = new ChatroomService();

    private final ToggleGroup tabGroup = new ToggleGroup();
    private int userId;

    @FXML
    private Label heroAvatarLabel;
    @FXML
    private Label heroNameLabel;
    @FXML
    private Label heroEmailLabel;
    @FXML
    private Label sideEmailLabel;
    @FXML
    private Label sideAgeLabel;
    @FXML
    private Label sideMemberLabel;
    @FXML
    private Label statPublishedLabel;
    @FXML
    private Label statSavedLabel;
    @FXML
    private Label statDraftsLabel;
    @FXML
    private Label statScheduledLabel;
    @FXML
    private ToggleButton tabPosts;
    @FXML
    private ToggleButton tabSaved;
    @FXML
    private ToggleButton tabScheduled;
    @FXML
    private ToggleButton tabDrafts;
    @FXML
    private ToggleButton tabChatrooms;
    @FXML
    private Hyperlink openFeedLink;
    @FXML
    private VBox contentBox;

    @FXML
    private void initialize() {
        Optional<User> session = AppSession.getCurrentUser();
        if (session.isEmpty() || session.get().getId() == null) {
            contentBox.getChildren().setAll(new Label("Veuillez vous connecter pour voir votre profil."));
            return;
        }
        userId = session.get().getId();

        for (ToggleButton b : List.of(tabPosts, tabSaved, tabScheduled, tabDrafts, tabChatrooms)) {
            b.setToggleGroup(tabGroup);
        }
        tabGroup.selectedToggleProperty().addListener((obs, prev, sel) -> {
            if (sel == null) {
                tabPosts.setSelected(true);
                return;
            }
            refreshTabContent();
        });

        openFeedLink.setOnAction(e -> onOpenFeed());

        loadUserHeader();
        refreshStatsAndTabs();
        tabPosts.setSelected(true);
    }

    private void loadUserHeader() {
        try {
            Optional<User> u = userService.findById(userId);
            if (u.isEmpty()) {
                heroNameLabel.setText("Utilisateur introuvable");
                return;
            }
            User user = u.get();
            heroAvatarLabel.setText(initials(user.getFirstName(), user.getLastName()));
            heroNameLabel.setText(fullName(user));
            heroEmailLabel.setText(user.getEmail() != null ? user.getEmail() : "");

            sideEmailLabel.setText("EMAIL : " + (user.getEmail() != null ? user.getEmail() : "—"));
            if (user.getAge() != null) {
                sideAgeLabel.setText("ÂGE : " + user.getAge() + " ans");
            } else {
                sideAgeLabel.setText("ÂGE : —");
            }
            if (user.getCreatedAt() != null) {
                sideMemberLabel.setText("MEMBRE DEPUIS : " + user.getCreatedAt().format(DF));
            } else {
                sideMemberLabel.setText("MEMBRE DEPUIS : —");
            }
        } catch (SQLException e) {
            heroNameLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void refreshStatsAndTabs() {
        try {
            int pub = postService.countPostsByAuthorAndStatus(userId, PostStatus.PUBLISHED);
            int draft = postService.countPostsByAuthorAndStatus(userId, PostStatus.DRAFT);
            int sched = postService.countPostsByAuthorAndStatus(userId, PostStatus.SCHEDULED);
            int saved = savedPostService.findByUserId(Integer.valueOf(userId)).size();
            int rooms = chatroomService.findAccessibleForUser(userId).size();

            statPublishedLabel.setText(String.valueOf(pub));
            statSavedLabel.setText(String.valueOf(saved));
            statDraftsLabel.setText(String.valueOf(draft));
            statScheduledLabel.setText(String.valueOf(sched));

            tabPosts.setText("Mes posts (" + pub + ")");
            tabSaved.setText("Sauvegardés (" + saved + ")");
            tabScheduled.setText("Planifiés (" + sched + ")");
            tabDrafts.setText("Brouillons (" + draft + ")");
            tabChatrooms.setText("Chatrooms (" + rooms + ")");
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void refreshTabContent() {
        contentBox.getChildren().clear();
        ToggleButton sel = (ToggleButton) tabGroup.getSelectedToggle();
        if (sel == null) {
            return;
        }
        try {
            if (sel == tabPosts) {
                fillPostCards(postService.findProfilePostsByAuthorAndStatus(userId, PostStatus.PUBLISHED));
            } else if (sel == tabSaved) {
                fillPostCards(postService.findProfileSavedPosts(userId));
            } else if (sel == tabScheduled) {
                fillPostCards(postService.findProfilePostsByAuthorAndStatus(userId, PostStatus.SCHEDULED));
            } else if (sel == tabDrafts) {
                fillPostCards(postService.findProfilePostsByAuthorAndStatus(userId, PostStatus.DRAFT));
            } else if (sel == tabChatrooms) {
                fillChatroomCards(chatroomService.findAccessibleForUser(userId));
            }
        } catch (SQLException e) {
            contentBox.getChildren().add(new Label("Erreur : " + e.getMessage()));
        }
    }

    private void fillPostCards(List<PostWithStats> rows) {
        if (rows.isEmpty()) {
            contentBox.getChildren().add(new Label("Aucun élément dans cette catégorie."));
            return;
        }
        for (PostWithStats row : rows) {
            contentBox.getChildren().add(buildPostCard(row));
        }
    }

    private VBox buildPostCard(PostWithStats row) {
        Post p = row.post();
        VBox card = new VBox(8);
        card.getStyleClass().add("profile-post-card");

        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(p.getTitle() != null ? p.getTitle() : "—");
        title.getStyleClass().add("profile-post-title");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Label badge = new Label(statusFr(p.getStatus()));
        badge.getStyleClass().add(statusBadgeClass(p.getStatus()));
        head.getChildren().addAll(title, grow, badge);

        String body = HtmlPlainText.toPlain(p.getContent());
        if (body.length() > 200) {
            body = body.substring(0, 197) + "…";
        }
        Label content = new Label(body);
        content.setWrapText(true);
        content.getStyleClass().add("profile-post-body");

        String dateStr = p.getCreatedAt() != null ? p.getCreatedAt().format(DF) : "—";
        HBox foot = new HBox(20);
        foot.getStyleClass().add("profile-post-footer");
        foot.setAlignment(Pos.CENTER_LEFT);
        foot.getChildren().addAll(
                new Label("📅  " + dateStr),
                new Label("❤️  " + row.likeCount()),
                new Label("💬  " + row.commentCount()));

        card.getChildren().addAll(head, content, foot);
        return card;
    }

    private static String statusFr(PostStatus s) {
        if (s == null) {
            return "—";
        }
        return switch (s) {
            case PUBLISHED -> "PUBLIÉ";
            case DRAFT -> "BROUILLON";
            case SCHEDULED -> "PLANIFIÉ";
            case HIDDEN -> "MASQUÉ";
        };
    }

    private static String statusBadgeClass(PostStatus s) {
        if (s == null) {
            return "badge-post-draft";
        }
        return switch (s) {
            case PUBLISHED -> "badge-post-pub";
            case DRAFT -> "badge-post-draft";
            case SCHEDULED -> "badge-post-sched";
            case HIDDEN -> "badge-post-hidden";
        };
    }

    private void fillChatroomCards(List<ChatroomListItem> rooms) {
        if (rooms.isEmpty()) {
            contentBox.getChildren().add(new Label("Aucun salon accessible pour le moment."));
            return;
        }
        for (ChatroomListItem it : rooms) {
            VBox card = new VBox(8);
            card.getStyleClass().add("profile-chat-card");
            Label t = new Label(it.goalTitle() != null ? it.goalTitle() : "Objectif #" + it.goalId());
            t.getStyleClass().add("profile-chat-title");
            String snip = it.lastMessageSnippet();
            Label sub = new Label(snip != null && !snip.isBlank()
                    ? truncate(snip, 80)
                    : "Pas encore de message.");
            sub.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");
            sub.setWrapText(true);
            Button open = new Button("Ouvrir le salon");
            open.getStyleClass().add("btn-open-chat");
            int gid = it.goalId();
            open.setOnAction(e -> openChatroom(gid));
            card.getChildren().addAll(t, sub, open);
            contentBox.getChildren().add(card);
        }
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }

    private void openChatroom(int goalId) {
        ChatroomNav.setOpenGoalId(goalId);
        try {
            NavigationManager.show("/user/chatroom/chatroom_hub.fxml", "DayFlow — Chat");
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onOpenFeed() {
        try {
            NavigationManager.show("/user/interaction/posts_feed.fxml", "DayFlow — Posts");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private static String initials(String first, String last) {
        String a = (first != null && !first.isBlank()) ? first.substring(0, 1).toUpperCase(Locale.ROOT) : "";
        String b = (last != null && !last.isBlank()) ? last.substring(0, 1).toUpperCase(Locale.ROOT) : "";
        String s = a + b;
        return s.isEmpty() ? "?" : s;
    }

    private static String fullName(User u) {
        String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String ln = u.getLastName() != null ? u.getLastName().trim() : "";
        String s = (fn + " " + ln).trim();
        return s.isEmpty() ? "Utilisateur" : s;
    }
}

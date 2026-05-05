package controllers.components;

import controllers.account.AuthNavigation;
import controllers.navigation.NavigationManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import model.notification.Notification;
import services.account.AccountSecurityService;
import services.notification.NotificationService;
import session.AppSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.util.Duration;

public class NavbarController {

    private static NavbarController instance;
    private final AccountSecurityService accountSecurityService = new AccountSecurityService();
    private final NotificationService notificationService = new NotificationService();
    private static final DateTimeFormatter NOTIF_DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);
    private Timeline notificationsRefreshTimeline;
    private javafx.stage.Popup notificationsMenu;

    @FXML
    private Hyperlink manageSessionsLink;
    @FXML
    private Hyperlink avatarLink;
    @FXML
    private StackPane notificationsContainer;
    @FXML
    private Button notificationsBellButton;
    @FXML
    private Label notificationsBadgeLabel;

    @FXML
    private void initialize() {
        instance = this;
        initNotificationAutoRefresh();
        refreshFromSession();
    }

    /** Met à jour avatar et visibilité coach après connexion. */
    public static void refreshFromSession() {
        if (instance != null) {
            instance.applyRoleVisibility();
            instance.applyAvatar();
            instance.refreshNotifications();
        }
    }

    private void applyRoleVisibility() {
        boolean coach = AppSession.isCoach();
        manageSessionsLink.setVisible(coach);
        manageSessionsLink.setManaged(coach);
        boolean authenticated = AppSession.getCurrentUser().isPresent();
        notificationsContainer.setVisible(authenticated);
        notificationsContainer.setManaged(authenticated);
    }

    private void applyAvatar() {
        avatarLink.setText(AppSession.getCurrentUser()
                .map(u -> initials(u.getFirstName(), u.getLastName()))
                .orElse("?"));
    }

    @FXML
    private void onProfile() {
        if (AppSession.getCurrentUser().isEmpty()) {
            toastSoon("Profil");
            return;
        }
        navigate("/user/account/user_profile.fxml", "DayFlow — Profil");
    }

    private static String initials(String first, String last) {
        String a = (first != null && !first.isBlank()) ? first.substring(0, 1).toUpperCase() : "";
        String b = (last != null && !last.isBlank()) ? last.substring(0, 1).toUpperCase() : "";
        String s = a + b;
        return s.isEmpty() ? "?" : s;
    }

    @FXML
    private void onAccueil() {
        if (AppSession.isCoach()) {
            navigate("/user/account/coach_dashboard.fxml", "DayFlow — Coach");
        } else {
            navigate("/user/account/user_dashboard.fxml", "DayFlow — Accueil");
        }
    }

    @FXML
    private void onObjectifs() {
        navigate("/user/goals_routines/goals_dashboard.fxml", "DayFlow — Mes objectifs");
    }

    @FXML
    private void onCommunity() {
        navigate("/user/interaction/community.fxml", "DayFlow — Community");
    }

    @FXML
    private void onCalendrier() {
        navigate("/user/goals_routines/goals_calendar.fxml", "DayFlow — Calendrier objectifs");
    }

    @FXML
    private void onFavoris() {
        toastSoon("Favoris");
    }

    @FXML
    private void onPosts() {
        navigate("/user/interaction/posts_feed.fxml", "DayFlow — Posts");
    }

    @FXML
    private void handleMesDemandes() {
        navigate("/user/coaching_session/mes_demandes.fxml", "DayFlow — Mes demandes");
    }

    @FXML
    private void onGererSessions() {
        if (AppSession.isCoach()) {
            navigate("/user/coaching_session/mes_sessions.fxml", "DayFlow — Mes sessions");
        } else {
            toastSoon("Gérer sessions");
        }
    }

    @FXML
    private void onMessages() {
        navigate("/user/chatroom/chatroom_hub.fxml", "DayFlow — Messages");
    }

    @FXML
    private void onNotifications() {
        if (AppSession.getCurrentUser().isEmpty()) {
            toastSoon("Notifications");
            return;
        }
        showNotificationsPopup();
    }

    @FXML
    private void onLogout() {
        AppSession.getCurrentUser().ifPresent(u -> {
            try {
                if (u.getId() != null) {
                    accountSecurityService.revokeCurrentSession(u.getId(), AppSession.getSessionToken().orElse(null));
                }
            } catch (Exception ignored) {
            }
        });
        AppSession.clear();
        try {
            AuthNavigation.showLanding();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private static void navigate(String path, String title) {
        try {
            NavigationManager.show(path, title);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private static void toastSoon(String module) {
        new Alert(Alert.AlertType.INFORMATION, module + " — bientôt disponible.").showAndWait();
    }

    private void initNotificationAutoRefresh() {
        notificationsRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> refreshNotifications())
        );
        notificationsRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        notificationsRefreshTimeline.play();
    }

    private void refreshNotifications() {
        Optional<Integer> userId = AppSession.getCurrentUser().map(u -> u.getId());
        if (userId.isEmpty() || userId.get() == null) {
            notificationsBadgeLabel.setVisible(false);
            notificationsBadgeLabel.setManaged(false);
            return;
        }
        try {
            int unread = notificationService.countUnreadByUser(userId.get());
            notificationsBadgeLabel.setText(String.valueOf(unread));
            boolean hasUnread = unread > 0;
            notificationsBadgeLabel.setVisible(hasUnread);
            notificationsBadgeLabel.setManaged(hasUnread);
        } catch (SQLException e) {
            notificationsBadgeLabel.setVisible(false);
            notificationsBadgeLabel.setManaged(false);
        }
    }

    private void showNotificationsPopup() {
        Integer uid = AppSession.getCurrentUser().map(u -> u.getId()).orElse(null);
        if (uid == null) return;

        try {
            List<Notification> notifications = notificationService.findLatestByUser(uid, 20);

            // ── Panel ──────────────────────────────────────────────
            VBox panel = new VBox(0);
            panel.getStyleClass().add("notif-panel");
            panel.setPrefWidth(370);

            // Header
            HBox header = new HBox(8);
            header.getStyleClass().add("notif-header");
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(0, 0, 10, 0));

            Label title = new Label("Notifications");
            title.getStyleClass().add("notif-title");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button markAllBtn = new Button("Tout marquer lu");
            markAllBtn.getStyleClass().add("notif-mark-all-btn");
            markAllBtn.setOnAction(e -> {
                try {
                    notificationService.markAllAsRead(uid);
                    refreshNotifications();
                    if (notificationsMenu != null) notificationsMenu.hide();
                    showNotificationsPopup();
                } catch (SQLException ex) {
                    new Alert(Alert.AlertType.ERROR, "Erreur : " + ex.getMessage()).showAndWait();
                }
            });

            header.getChildren().addAll(title, spacer, markAllBtn);
            panel.getChildren().add(header);

            Separator sep = new Separator();
            sep.setPadding(new Insets(0, 0, 8, 0));
            panel.getChildren().add(sep);

            if (notifications.isEmpty()) {
                Label empty = new Label("Aucune notification pour l'instant");
                empty.getStyleClass().add("notif-empty");
                empty.setMaxWidth(Double.MAX_VALUE);
                empty.setAlignment(Pos.CENTER);
                panel.getChildren().add(empty);
            } else {
                VBox list = new VBox(6);
                list.getStyleClass().add("notif-list");
                list.setPadding(new Insets(4, 0, 0, 0));
                for (Notification n : notifications) {
                    list.getChildren().add(buildNotificationItem(n, uid));
                }

                ScrollPane scroll = new ScrollPane(list);
                scroll.getStyleClass().add("notif-scroll");
                scroll.setFitToWidth(true);
                scroll.setPrefViewportHeight(340);
                scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                // Force white background — no transparency bleed
                scroll.setStyle("-fx-background: white; -fx-background-color: white; -fx-border-color: transparent;");
                panel.getChildren().add(scroll);
            }

            // Load CSS into the panel's scene via stylesheet
            String css = getClass().getResource("/components/navbar/navbar.css").toExternalForm();

            // ── Popup ──────────────────────────────────────────────
            if (notificationsMenu != null) notificationsMenu.hide();
            notificationsMenu = new Popup();
            notificationsMenu.setAutoHide(true);
            notificationsMenu.setAutoFix(true);
            notificationsMenu.getContent().add(panel);

            // Apply CSS after adding to popup
            panel.getStylesheets().add(css);

            // Position below the bell button
            Bounds bounds = notificationsBellButton.localToScreen(notificationsBellButton.getBoundsInLocal());
            double x = bounds.getMaxX() - 370;
            double y = bounds.getMaxY() + 6;
            notificationsMenu.show(notificationsBellButton.getScene().getWindow(), x, y);

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible de charger les notifications : " + e.getMessage()).showAndWait();
        }
    }

    private VBox buildNotificationItem(Notification n, int uid) {
        VBox item = new VBox(4);
        item.getStyleClass().add("notif-item");
        if (!n.isRead()) {
            item.getStyleClass().add("notif-item-unread");
        }
        item.setOnMouseClicked(e -> {
            try {
                if (!n.isRead() && n.getId() != null) {
                    notificationService.markAsRead(n.getId(), uid);
                    refreshNotifications();
                    if (notificationsMenu != null) {
                        notificationsMenu.hide();
                    }
                    showNotificationsPopup();
                }
            } catch (SQLException ex) {
                new Alert(Alert.AlertType.ERROR, "Impossible de marquer la notification : " + ex.getMessage()).showAndWait();
            }
        });

        // Top row: badge + unread dot
        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label typeBadge = new Label(humanType(n.getType()));
        typeBadge.getStyleClass().addAll("notif-type-badge", badgeStyleForType(n.getType()));
        Region rowSpacer = new Region();
        HBox.setHgrow(rowSpacer, Priority.ALWAYS);
        topRow.getChildren().add(typeBadge);
        topRow.getChildren().add(rowSpacer);
        if (!n.isRead()) {
            StackPane dot = new StackPane();
            dot.getStyleClass().add("notif-unread-dot");
            topRow.getChildren().add(dot);
        }

        Label msg = new Label(n.getMessage() != null ? n.getMessage() : "—");
        msg.getStyleClass().add("notif-message");
        msg.setWrapText(true);
        msg.setMaxWidth(320);

        Label meta = new Label(n.getCreatedAt() != null ? n.getCreatedAt().format(NOTIF_DF) : "—");
        meta.getStyleClass().add("notif-meta");

        item.getChildren().addAll(topRow, msg, meta);
        return item;
    }

    private static String badgeStyleForType(String type) {
        if (type == null) return "notif-type-badge-info";
        String t = type.toLowerCase(java.util.Locale.ROOT);
        if (t.contains("success") || t.contains("accept") || t.contains("paid")) return "notif-type-badge-success";
        if (t.contains("error") || t.contains("fail") || t.contains("reject") || t.contains("ban")) return "notif-type-badge-error";
        return "notif-type-badge-info";
    }

    private static String humanType(String type) {
        if (type == null || type.isBlank()) {
            return "INFO";
        }
        return type.replace('_', ' ').toUpperCase(Locale.ROOT);
    }
}

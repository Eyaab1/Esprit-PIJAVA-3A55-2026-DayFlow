package controllers.components;

import controllers.account.AuthNavigation;
import controllers.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import services.account.AccountSecurityService;
import session.AppSession;

import java.io.IOException;

public class NavbarController {

    private static NavbarController instance;
    private final AccountSecurityService accountSecurityService = new AccountSecurityService();

    @FXML
    private Hyperlink manageSessionsLink;
    @FXML
    private Hyperlink avatarLink;

    @FXML
    private void initialize() {
        instance = this;
        refreshFromSession();
    }

    /** Met à jour avatar et visibilité coach après connexion. */
    public static void refreshFromSession() {
        if (instance != null) {
            instance.applyRoleVisibility();
            instance.applyAvatar();
        }
    }

    private void applyRoleVisibility() {
        boolean coach = AppSession.isCoach();
        manageSessionsLink.setVisible(coach);
        manageSessionsLink.setManaged(coach);
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
        toastSoon("Notifications");
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
}

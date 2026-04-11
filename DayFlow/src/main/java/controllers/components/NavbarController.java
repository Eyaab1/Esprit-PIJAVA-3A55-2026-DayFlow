package controllers.components;

import controllers.auth.AuthNavigation;
import controllers.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import session.AppSession;

import java.io.IOException;

public class NavbarController {

    private static NavbarController instance;

    @FXML
    private Hyperlink manageSessionsLink;
    @FXML
    private Label avatarLabel;

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
        avatarLabel.setText(AppSession.getCurrentUser()
                .map(u -> initials(u.getFirstName(), u.getLastName()))
                .orElse("?"));
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
            navigate("/user/coachdashboard/coach_dashboard.fxml", "DayFlow — Coach");
        } else {
            navigate("/user/userdashboard/user_dashboard.fxml", "DayFlow — Accueil");
        }
    }

    @FXML
    private void onObjectifs() {
        toastSoon("Objectifs");
    }

    @FXML
    private void onCommunity() {
        toastSoon("Community");
    }

    @FXML
    private void onCalendrier() {
        toastSoon("Calendrier");
    }

    @FXML
    private void onFavoris() {
        toastSoon("Favoris");
    }

    @FXML
    private void onPosts() {
        toastSoon("Posts");
    }

    @FXML
    private void onMesDemandes() {
        toastSoon("Mes demandes");
    }

    @FXML
    private void onGererSessions() {
        if (AppSession.isCoach()) {
            navigate("/user/coachdashboard/coach_dashboard.fxml", "DayFlow — Coach");
        } else {
            toastSoon("Gérer sessions");
        }
    }

    @FXML
    private void onMessages() {
        toastSoon("Messages");
    }

    @FXML
    private void onNotifications() {
        toastSoon("Notifications");
    }

    @FXML
    private void onLogout() {
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

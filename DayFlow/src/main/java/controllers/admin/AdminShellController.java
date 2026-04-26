package controllers.admin;

import controllers.account.AuthNavigation;
import controllers.components.NavbarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import services.account.AccountSecurityService;
import session.AppSession;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

/**
 * Coque administration : barre latérale, en-tête, zone centrale (dashboard, utilisateurs, …).
 */
public class AdminShellController {
    private final AccountSecurityService accountSecurityService = new AccountSecurityService();

    @FXML
    private StackPane adminContentPane;
    @FXML
    private Button navDashboardBtn;
    @FXML
    private Button navUsersBtn;
    @FXML
    private Button navGoalsBtn;
    @FXML
    private Button navPostsBtn;
    @FXML
    private Button navCoachesBtn;
    @FXML
    private Button navCoachRequestsBtn;
    @FXML
    private Button navReclamationsBtn;
    @FXML
    private Label adminEmailLabel;

    private List<Button> mainNavButtons;

    @FXML
    private void initialize() {
        mainNavButtons = List.of(
                navDashboardBtn,
                navUsersBtn,
                navGoalsBtn,
                navPostsBtn,
                navCoachesBtn,
                navCoachRequestsBtn,
                navReclamationsBtn
        );
        if (!AppSession.isAdmin()) {
            try {
                AuthNavigation.showLanding();
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            }
            return;
        }
        AppSession.getCurrentUser().ifPresent(u -> adminEmailLabel.setText(u.getEmail()));
        try {
            loadDashboard();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onNavDashboard() throws IOException {
        loadDashboard();
    }

    @FXML
    private void onNavUsers() throws IOException {
        loadUsers();
    }

    @FXML
    private void onNavGoals() throws IOException {
        loadGoals();
    }

    @FXML
    private void onNavPosts() throws IOException {
        loadPosts();
    }

    @FXML
    private void onNavCoaches() throws IOException {
        loadCoaches();
    }

    @FXML
    private void onNavCoachRequests() throws IOException {
        loadCoachRequests();
    }

    @FXML
    private void onNavReclamations() throws IOException {
        loadReclamations();
    }

    @FXML
    private void onNavPlaceholder() {
        new Alert(Alert.AlertType.INFORMATION, "Cette section arrive bientôt.").showAndWait();
    }

    @FXML
    private void onLogout() {
        try {
            AppSession.getCurrentUser().ifPresent(u -> {
                try {
                    if (u.getId() != null) {
                        accountSecurityService.revokeCurrentSession(u.getId(), AppSession.getSessionToken().orElse(null));
                    }
                } catch (Exception ignored) {
                }
            });
            AppSession.clear();
            NavbarController.refreshFromSession();
            AuthNavigation.showLogin();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    public void loadDashboard() throws IOException {
        FXMLLoader loader = loadCenter("/admin/admin_dashboard.fxml", navDashboardBtn);
        AdminDashboardController c = loader.getController();
        c.setShell(this);
    }

    public void loadUsers() throws IOException {
        loadCenter("/admin/admin_users.fxml", navUsersBtn);
    }

    public void loadGoals() throws IOException {
        loadCenter("/admin/admin_goals.fxml", navGoalsBtn);
    }

    public void loadPosts() throws IOException {
        loadCenter("/admin/admin_posts.fxml", navPostsBtn);
    }

    public void loadCoaches() throws IOException {
        loadCenter("/admin/admin_coaches.fxml", navCoachesBtn);
    }

    public void loadCoachRequests() throws IOException {
        loadCenter("/admin/admin_coach_requests.fxml", navCoachRequestsBtn);
    }

    public void loadReclamations() throws IOException {
        loadCenter("/admin/admin_reclamations.fxml", navReclamationsBtn);
    }

    private FXMLLoader loadCenter(String resource, Button activeNav) throws IOException {
        URL url = Objects.requireNonNull(getClass().getResource(resource), resource);
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        stretchToParent(root);
        adminContentPane.getChildren().setAll(root);
        setNavActive(activeNav);
        return loader;
    }

    private static void stretchToParent(Parent root) {
        if (root instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
            r.setMaxHeight(Double.MAX_VALUE);
        }
    }

    private void setNavActive(Button active) {
        if (mainNavButtons != null) {
            for (Button b : mainNavButtons) {
                if (b != null) {
                    b.getStyleClass().remove("admin-nav-btn-active");
                }
            }
        }
        if (active != null && !active.getStyleClass().contains("admin-nav-btn-active")) {
            active.getStyleClass().add("admin-nav-btn-active");
        }
    }
}

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
 * Coque administration : barre latérale, en-tête, zone centrale
 * (dashboard, utilisateurs, objectifs, posts, réclamations, etc.).
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
    private Button navModerationLogsBtn;

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
                navModerationLogsBtn,
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

        AppSession.getCurrentUser()
                .ifPresent(user -> adminEmailLabel.setText(user.getEmail()));

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
    private void onNavModerationLogs() throws IOException {
        loadModerationLogs();
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
        new Alert(
                Alert.AlertType.INFORMATION,
                "Cette section arrive bientôt."
        ).showAndWait();
    }

    @FXML
    private void onLogout() {
        try {
            AppSession.getCurrentUser().ifPresent(user -> {
                try {
                    if (user.getId() != null) {
                        accountSecurityService.revokeCurrentSession(
                                user.getId(),
                                AppSession.getSessionToken().orElse(null)
                        );
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
        FXMLLoader loader = loadCenter(
                "/admin/admin_dashboard.fxml",
                navDashboardBtn
        );

        AdminDashboardController controller = loader.getController();
        controller.setShell(this);
    }

    public void loadUsers() throws IOException {
        loadCenter(
                "/admin/admin_users.fxml",
                navUsersBtn
        );
    }

    public void loadGoals() throws IOException {
        loadCenter(
                "/admin/admin_goals.fxml",
                navGoalsBtn
        );
    }

    public void loadPosts() throws IOException {
        FXMLLoader loader = loadCenter(
                "/admin/admin_posts.fxml",
                navPostsBtn
        );

        AdminPostsController controller = loader.getController();
        controller.setShell(this);
    }

    public void loadPostDetails(int postId) throws IOException {
        FXMLLoader loader = loadCenter(
                "/admin/admin_post_details.fxml",
                navPostsBtn
        );

        AdminPostDetailsController controller = loader.getController();
        controller.setContext(this, postId);
    }

    public void loadModerationLogs() throws IOException {
        loadCenter(
                "/admin/admin_moderation_logs.fxml",
                navModerationLogsBtn
        );
    }

    public void loadCoaches() throws IOException {
        loadCenter(
                "/admin/admin_coaches.fxml",
                navCoachesBtn
        );
    }

    public void loadCoachRequests() throws IOException {
        loadCenter(
                "/admin/admin_coach_requests.fxml",
                navCoachRequestsBtn
        );
    }

    public void loadReclamations() throws IOException {
        loadCenter(
                "/admin/admin_reclamations.fxml",
                navReclamationsBtn
        );
    }

    private FXMLLoader loadCenter(String resource, Button activeNav) throws IOException {
        URL url = Objects.requireNonNull(
                getClass().getResource(resource),
                resource
        );

        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();

        stretchToParent(root);
        adminContentPane.getChildren().setAll(root);
        setNavActive(activeNav);

        return loader;
    }

    private static void stretchToParent(Parent root) {
        if (root instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            region.setMaxHeight(Double.MAX_VALUE);
        }
    }

    private void setNavActive(Button active) {
        if (mainNavButtons != null) {
            for (Button button : mainNavButtons) {
                if (button != null) {
                    button.getStyleClass()
                            .remove("admin-nav-btn-active");
                }
            }
        }

        if (active != null &&
                !active.getStyleClass().contains("admin-nav-btn-active")) {
            active.getStyleClass()
                    .add("admin-nav-btn-active");
        }
    }
}
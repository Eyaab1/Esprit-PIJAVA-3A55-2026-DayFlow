package controllers.account;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.user.User;
import controllers.components.NavbarController;
import controllers.navigation.NavigationManager;
import services.account.AccountSecurityService;
import services.account.AuthService;
import services.account.IpGeolocationService;
import services.account.SecurityAlertMailService;
import services.admin.ModerationActionService;
import session.AppSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class LoginController {

    private final AuthService authService = new AuthService();
    private final AccountSecurityService accountSecurityService = new AccountSecurityService();
    private final IpGeolocationService ipGeolocationService = new IpGeolocationService();
    private final SecurityAlertMailService securityAlertMailService = new SecurityAlertMailService();
    private final ModerationActionService moderationActionService = new ModerationActionService();

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Button goToSignupButton;
    @FXML
    private Hyperlink forgotPasswordLink;
    @FXML
    private Button googleLoginButton;
    @FXML
    private Hyperlink resetWithTokenLink;
    @FXML
    private Label formMessageLabel;

    @FXML
    private void initialize() {
        loginButton.setOnAction(e -> onLogin());
        goToSignupButton.setOnAction(e -> navigateToSignup());
        forgotPasswordLink.setOnAction(e -> onForgotPassword());
        googleLoginButton.setOnAction(e -> onGoogleLogin());
        resetWithTokenLink.setOnAction(e -> onResetWithToken());
    }

    private void onLogin() {
        clearMessage();
        try {
            AuthService.LoginResult loginResult = authService.loginDetailed(emailField.getText(), passwordField.getText());
            Optional<User> user = loginResult.user();
            if (user.isPresent()) {
                User u = user.get();
                // Lift expired posting ban if applicable
                moderationActionService.liftExpiredPostingBan(u.getId());
                if (loginResult.securityMeta() != null) {
                    AppSession.setCurrentUser(u, loginResult.securityMeta().sessionToken(), loginResult.securityMeta().deviceLabel());
                    if (loginResult.securityMeta().suspicious()) {
                        String reason = loginResult.securityMeta().suspiciousReason() == null
                                ? "New or risky login detected."
                                : loginResult.securityMeta().suspiciousReason();
                        showError("Suspicious login detected: " + reason + ". Please review active sessions in your profile.");
                    }
                } else {
                    AppSession.setCurrentUser(u);
                }
                NavbarController.refreshFromSession();
                try {
                    if (AppSession.isAdmin()) {
                        NavigationManager.show("/admin/admin_shell.fxml", "DayFlow — Administration");
                    } else if (AppSession.isCoach()) {
                        NavigationManager.show("/user/account/coach_dashboard.fxml", "DayFlow — Coach");
                    } else {
                        NavigationManager.show("/user/account/user_dashboard.fxml", "DayFlow — Accueil");
                    }
                } catch (IOException io) {
                    showError(io.getMessage());
                }
            } else {
                showError("Email ou mot de passe incorrect.");
            }
        } catch (SQLException ex) {
            showError("Erreur base de données : " + ex.getMessage());
        } catch (IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    private void navigateToSignup() {
        try {
            AuthNavigation.showSignup();
        } catch (IOException ex) {
            showError(ex.getMessage());
        }
    }

    private void onForgotPassword() {
        try {
            AuthNavigation.showForgotPassword();
        } catch (IOException ex) {
            showError(ex.getMessage());
        }
    }

    private void onResetWithToken() {
        try {
            AuthNavigation.showResetPassword();
        } catch (IOException ex) {
            showError(ex.getMessage());
        }
    }

    private void onGoogleLogin() {
        try {
            clearMessage();
            User u = authService.loginWithGoogle();
            if (u.getId() != null) {
                // Lift expired posting ban if applicable
                moderationActionService.liftExpiredPostingBan(u.getId());
                AccountSecurityService.LoginSuccessMeta meta = accountSecurityService.registerSuccessfulLogin(u.getId(), u.getEmail());
                AppSession.setCurrentUser(u, meta.sessionToken(), meta.deviceLabel());
                if (meta.suspicious()) {
                    IpGeolocationService.GeoInfo geo = ipGeolocationService.resolve(meta.ipAddress());
                    securityAlertMailService.sendSuspiciousLoginAlert(
                            u.getEmail(),
                            ((u.getFirstName() == null ? "" : u.getFirstName().trim()) + " "
                                    + (u.getLastName() == null ? "" : u.getLastName().trim())).trim(),
                            meta.suspiciousReason(),
                            meta.deviceLabel(),
                            geo.ipAddress(),
                            geo.locationLabel()
                    );
                }
            } else {
                AppSession.setCurrentUser(u);
            }
            NavbarController.refreshFromSession();
            if (AppSession.isAdmin()) {
                NavigationManager.show("/admin/admin_shell.fxml", "DayFlow — Administration");
            } else if (AppSession.isCoach()) {
                NavigationManager.show("/user/account/coach_dashboard.fxml", "DayFlow — Coach");
            } else {
                NavigationManager.show("/user/account/user_dashboard.fxml", "DayFlow — Accueil");
            }
        } catch (Exception ex) {
            showError("Google login failed: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        formMessageLabel.getStyleClass().remove("auth-success-msg");
        if (!formMessageLabel.getStyleClass().contains("auth-error-msg")) {
            formMessageLabel.getStyleClass().add("auth-error-msg");
        }
        formMessageLabel.setText(message);
        formMessageLabel.setVisible(true);
        formMessageLabel.setManaged(true);
    }

    private void clearMessage() {
        formMessageLabel.setText("");
        formMessageLabel.setVisible(false);
        formMessageLabel.setManaged(false);
    }
}

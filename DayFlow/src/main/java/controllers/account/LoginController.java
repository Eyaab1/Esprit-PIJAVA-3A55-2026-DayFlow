package controllers.account;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.user.User;
import controllers.components.NavbarController;
import controllers.navigation.NavigationManager;
import services.account.AuthService;
import session.AppSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class LoginController {

    private final AuthService authService = new AuthService();

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
    private void initialize() {
        loginButton.setOnAction(e -> onLogin());
        goToSignupButton.setOnAction(e -> navigateToSignup());
        forgotPasswordLink.setOnAction(e -> onForgotPassword());
        googleLoginButton.setOnAction(e -> onGoogleLogin());
        resetWithTokenLink.setOnAction(e -> onResetWithToken());
    }

    private void onLogin() {
        try {
            Optional<User> user = authService.login(emailField.getText(), passwordField.getText());
            if (user.isPresent()) {
                User u = user.get();
                AppSession.setCurrentUser(u);
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
                    new Alert(Alert.AlertType.ERROR, io.getMessage()).showAndWait();
                }
            } else {
                new Alert(Alert.AlertType.ERROR, "Email ou mot de passe incorrect.").showAndWait();
            }
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur base de données : " + ex.getMessage()).showAndWait();
        } catch (IllegalStateException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        }
    }

    private void navigateToSignup() {
        try {
            AuthNavigation.showSignup();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private void onForgotPassword() {
        try {
            AuthNavigation.showForgotPassword();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private void onResetWithToken() {
        try {
            AuthNavigation.showResetPassword();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private void onGoogleLogin() {
        try {
            User u = authService.loginWithGoogle();
            AppSession.setCurrentUser(u);
            NavbarController.refreshFromSession();
            if (AppSession.isAdmin()) {
                NavigationManager.show("/admin/admin_shell.fxml", "DayFlow — Administration");
            } else if (AppSession.isCoach()) {
                NavigationManager.show("/user/account/coach_dashboard.fxml", "DayFlow — Coach");
            } else {
                NavigationManager.show("/user/account/user_dashboard.fxml", "DayFlow — Accueil");
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Google login failed: " + ex.getMessage()).showAndWait();
        }
    }
}

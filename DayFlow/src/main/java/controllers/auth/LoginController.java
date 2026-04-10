package controllers.auth;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.user.User;
import controllers.components.NavbarController;
import controllers.navigation.NavigationManager;
import services.UserServices.UserService;
import session.AppSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class LoginController {

    private final UserService userService = new UserService();

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
    private void initialize() {
        loginButton.setOnAction(e -> onLogin());
        goToSignupButton.setOnAction(e -> navigateToSignup());
        forgotPasswordLink.setOnAction(e -> onForgotPassword());
    }

    private void onLogin() {
        try {
            Optional<User> user = userService.login(emailField.getText(), passwordField.getText());
            if (user.isPresent()) {
                User u = user.get();
                AppSession.setCurrentUser(u);
                NavbarController.refreshFromSession();
                try {
                    NavigationManager.show("/views/home_dashboard.fxml", "DayFlow — Accueil");
                } catch (IOException io) {
                    new Alert(Alert.AlertType.ERROR, io.getMessage()).showAndWait();
                }
            } else {
                new Alert(Alert.AlertType.ERROR, "Email ou mot de passe incorrect.").showAndWait();
            }
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur base de données : " + ex.getMessage()).showAndWait();
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
        new Alert(Alert.AlertType.INFORMATION, "Fonctionnalité à venir.").showAndWait();
    }
}

package controllers.auth;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.user.User;
import services.UserServices.UserService;

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
                new Alert(Alert.AlertType.INFORMATION,
                        "Bienvenue, " + u.getFirstName() + " " + u.getLastName()
                                + "\n" + u.getEmail() + "\nRôles : " + u.getRoles()).showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR, "Email ou mot de passe incorrect.").showAndWait();
            }
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur base de données : " + ex.getMessage()).showAndWait();
        }
    }

    private void navigateToSignup() {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            AuthNavigation.showSignup(stage);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private void onForgotPassword() {
        new Alert(Alert.AlertType.INFORMATION, "Fonctionnalité à venir.").showAndWait();
    }
}

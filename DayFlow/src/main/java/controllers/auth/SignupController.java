package controllers.auth;

import enums.UserRole;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.user.User;
import services.UserServices.UserService;

import java.io.IOException;
import java.sql.SQLException;

public class SignupController {

    private final UserService userService = new UserService();

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private DatePicker birthDatePicker;
    @FXML
    private ChoiceBox<UserRole> roleChoiceBox;
    @FXML
    private Button signupButton;
    @FXML
    private Button goToLoginButton;

    @FXML
    private void initialize() {
        roleChoiceBox.getItems().setAll(UserRole.USER, UserRole.COACH);
        roleChoiceBox.setValue(UserRole.USER);
        signupButton.setOnAction(e -> onSignup());
        goToLoginButton.setOnAction(e -> navigateToLogin());
    }

    private void onSignup() {
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            new Alert(Alert.AlertType.ERROR, "Les mots de passe ne correspondent pas.").showAndWait();
            return;
        }
        UserRole role = roleChoiceBox.getValue();
        if (role == null) role = UserRole.USER;

        try {
            User created = userService.signUp(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    role
            );

            // Message propre sans infos techniques
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("Compte créé avec succès 🎉\nBienvenue " + created.getFirstName() + " !");
            alert.showAndWait();

            // Connexion automatique + redirection dashboard
            session.AppSession.setCurrentUser(created);
            controllers.components.NavbarController.refreshFromSession();

            // Email de bienvenue (asynchrone)
            if (created.getEmail() != null) {
                services.EmailService.sendWelcome(created.getEmail(),
                        created.getFirstName() != null ? created.getFirstName() : "");
            }
            try {
                if (session.AppSession.isCoach()) {
                    controllers.navigation.NavigationManager.show(
                            "/user/coachdashboard/coach_dashboard.fxml", "DayFlow — Coach");
                } else {
                    controllers.navigation.NavigationManager.show(
                            "/user/userdashboard/user_dashboard.fxml", "DayFlow — Accueil");
                }
            } catch (IOException io) {
                new Alert(Alert.AlertType.ERROR, io.getMessage()).showAndWait();
            }

        } catch (IllegalArgumentException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur base de données : " + ex.getMessage()).showAndWait();
        }
    }

    private void navigateToLogin() {
        try {
            AuthNavigation.showLogin();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }
}

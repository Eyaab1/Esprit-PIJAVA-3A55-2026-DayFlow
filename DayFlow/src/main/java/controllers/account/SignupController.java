package controllers.account;

import enums.UserRole;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import model.user.User;
import services.account.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.function.UnaryOperator;

public class SignupController {

    private final UserService userService = new UserService();

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField ageField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
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
        phoneField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getText().isEmpty()) {
                return change;
            }
            if (change.getText().matches("[0-9+()\\s.\\-]+")) {
                return change;
            }
            return null;
        }));
        UnaryOperator<TextFormatter.Change> ageFilter = change -> {
            String next = change.getControlNewText();
            if (next.isEmpty()) {
                return change;
            }
            if (next.matches("\\d{1,3}")) {
                return change;
            }
            return null;
        };
        ageField.setTextFormatter(new TextFormatter<>(ageFilter));
        signupButton.setOnAction(e -> onSignup());
        goToLoginButton.setOnAction(e -> navigateToLogin());
    }

    private void onSignup() {
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            new Alert(Alert.AlertType.ERROR, "Les mots de passe ne correspondent pas.").showAndWait();
            return;
        }
        UserRole role = roleChoiceBox.getValue();
        if (role == null) {
            role = UserRole.USER;
        }
        try {
            User created = userService.signUp(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    role,
                    phoneField.getText(),
                    ageField.getText()
            );
            new Alert(Alert.AlertType.INFORMATION,
                    "Compte créé, id = " + created.getId() + ". Vous pouvez vous connecter.").showAndWait();
            navigateToLogin();
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

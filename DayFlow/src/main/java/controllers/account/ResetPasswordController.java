package controllers.account;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import services.account.PasswordResetService;

import java.io.IOException;
import java.sql.SQLException;

public class ResetPasswordController {

    private final PasswordResetService passwordResetService = new PasswordResetService();

    @FXML
    private TextField tokenField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button resetButton;
    @FXML
    private Button backButton;

    @FXML
    private void initialize() {
        resetButton.setOnAction(e -> onReset());
        backButton.setOnAction(e -> onBack());
    }

    private void onReset() {
        if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
            new Alert(Alert.AlertType.ERROR, "Passwords do not match.").showAndWait();
            return;
        }
        try {
            boolean ok = passwordResetService.resetPassword(tokenField.getText(), newPasswordField.getText());
            if (ok) {
                new Alert(Alert.AlertType.INFORMATION, "Password updated. Please login.").showAndWait();
                AuthNavigation.showLogin();
            } else {
                new Alert(Alert.AlertType.ERROR, "Invalid or expired token.").showAndWait();
            }
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + ex.getMessage()).showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private void onBack() {
        try {
            AuthNavigation.showLogin();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }
}

package controllers.account;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import services.account.PasswordResetService;

import java.io.IOException;
import java.sql.SQLException;

public class ForgotPasswordController {

    private final PasswordResetService passwordResetService = new PasswordResetService();

    @FXML
    private TextField emailField;
    @FXML
    private Button sendButton;
    @FXML
    private Button backButton;
    @FXML
    private Label formMessageLabel;

    @FXML
    private void initialize() {
        sendButton.setOnAction(e -> onSend());
        backButton.setOnAction(e -> onBack());
    }

    private void onSend() {
        clearMessage();
        try {
            passwordResetService.requestReset(emailField.getText());
            showSuccess("If the email exists, a 6-digit reset code has been sent.");
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void onBack() {
        try {
            AuthNavigation.showLogin();
        } catch (IOException ex) {
            showError(ex.getMessage());
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

    private void showSuccess(String message) {
        formMessageLabel.getStyleClass().remove("auth-error-msg");
        if (!formMessageLabel.getStyleClass().contains("auth-success-msg")) {
            formMessageLabel.getStyleClass().add("auth-success-msg");
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

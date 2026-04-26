package controllers.account;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
    private void initialize() {
        sendButton.setOnAction(e -> onSend());
        backButton.setOnAction(e -> onBack());
    }

    private void onSend() {
        try {
            passwordResetService.requestReset(emailField.getText());
            new Alert(Alert.AlertType.INFORMATION,
                    "If the email exists, a reset message has been sent.").showAndWait();
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + ex.getMessage()).showAndWait();
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

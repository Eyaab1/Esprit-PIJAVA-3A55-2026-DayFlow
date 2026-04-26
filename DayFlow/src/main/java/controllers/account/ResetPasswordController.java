package controllers.account;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    private Label formMessageLabel;

    @FXML
    private void initialize() {
        resetButton.setOnAction(e -> onReset());
        backButton.setOnAction(e -> onBack());
    }

    private void onReset() {
        clearMessage();
        if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
            showError("Passwords do not match.");
            return;
        }
        try {
            boolean ok = passwordResetService.resetPassword(tokenField.getText(), newPasswordField.getText());
            if (ok) {
                showSuccess("Password updated. Please login.");
                AuthNavigation.showLogin();
            } else {
                showError("Invalid or expired reset code.");
            }
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (IOException ex) {
            showError(ex.getMessage());
        }
    }

    public void setPrefilledToken(String tokenOrLink) {
        tokenField.setText(normalizeToken(tokenOrLink));
    }

    private String normalizeToken(String tokenOrLink) {
        if (tokenOrLink == null) {
            return "";
        }
        String trimmed = tokenOrLink.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int tokenParamIndex = trimmed.indexOf("token=");
        if (tokenParamIndex >= 0) {
            String tokenPart = trimmed.substring(tokenParamIndex + "token=".length());
            int ampIndex = tokenPart.indexOf('&');
            String value = (ampIndex >= 0 ? tokenPart.substring(0, ampIndex) : tokenPart).trim();
            return value.replaceAll("\\D", "");
        }
        return trimmed.replaceAll("\\D", "");
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

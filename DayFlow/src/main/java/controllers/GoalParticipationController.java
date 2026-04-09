package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.GoalParticipation;
import services.chatroom.GoalParticipationService;

import java.sql.SQLException;

public class GoalParticipationController {

    @FXML private TextField userIdField;
    @FXML private TextField goalIdField;
    @FXML private ComboBox<String> roleBox;
    @FXML private ComboBox<String> statusBox;
    @FXML private TextField updateIdField;
    @FXML private Label statusLabel;

    private GoalParticipationService service;

    @FXML
    public void initialize() {
        service = new GoalParticipationService();
        roleBox.getItems().addAll(
            GoalParticipation.ROLE_MEMBER,
            GoalParticipation.ROLE_ADMIN,
            GoalParticipation.ROLE_OWNER
        );
        statusBox.getItems().addAll(
            GoalParticipation.STATUS_PENDING,
            GoalParticipation.STATUS_APPROVED,
            GoalParticipation.STATUS_REJECTED
        );
        roleBox.setValue(GoalParticipation.ROLE_MEMBER);
        statusBox.setValue(GoalParticipation.STATUS_APPROVED);
    }

    @FXML
    public void addParticipation() {
        try {
            int userId = Integer.parseInt(userIdField.getText().trim());
            int goalId = Integer.parseInt(goalIdField.getText().trim());

            GoalParticipation gp = new GoalParticipation(userId, goalId);
            service.create(gp);
            showStatus("Participation ajoutée ✅", true);
            clearFields();
        } catch (IllegalArgumentException e) {
            showStatus("Erreur : " + e.getMessage(), false);
        } catch (SQLException e) {
            showStatus("Erreur BD : " + e.getMessage(), false);
        }
    }

    @FXML
    public void updateParticipation() {
        try {
            int id = Integer.parseInt(updateIdField.getText().trim());
            String role = roleBox.getValue();
            String status = statusBox.getValue();

            GoalParticipation gp = new GoalParticipation();
            gp.setId(id);
            gp.setRole(role);
            gp.setStatus(status);
            service.update(gp);
            showStatus("Participation modifiée ✅", true);
            clearFields();
        } catch (IllegalArgumentException e) {
            showStatus("Erreur : " + e.getMessage(), false);
        } catch (SQLException e) {
            showStatus("Erreur BD : " + e.getMessage(), false);
        }
    }

    @FXML
    public void deleteParticipation() {
        try {
            int id = Integer.parseInt(updateIdField.getText().trim());
            service.delete(id);
            showStatus("Participation supprimée ✅", true);
            clearFields();
        } catch (NumberFormatException e) {
            showStatus("ID invalide.", false);
        } catch (SQLException e) {
            showStatus("Erreur BD : " + e.getMessage(), false);
        }
    }

    private void showStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
    }

    private void clearFields() {
        userIdField.clear();
        goalIdField.clear();
        updateIdField.clear();
        roleBox.setValue(GoalParticipation.ROLE_MEMBER);
        statusBox.setValue(GoalParticipation.STATUS_APPROVED);
    }
}

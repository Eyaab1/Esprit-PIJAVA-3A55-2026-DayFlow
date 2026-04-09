package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.chatroom.Chatroom;
import services.chatroom_module.ChatroomService;

import java.sql.SQLException;

public class ChatroomController {

    @FXML private TextField goalIdField;
    @FXML private ComboBox<String> stateBox;
    @FXML private TextField updateIdField;
    @FXML private Label statusLabel;

    private ChatroomService service;

    @FXML
    public void initialize() {
        service = new ChatroomService();
        stateBox.getItems().addAll("active", "inactive");
        stateBox.setValue("active");
    }

    @FXML
    public void addChatroom() {
        try {
            int goalId = Integer.parseInt(goalIdField.getText().trim());
            String state = stateBox.getValue();

            Chatroom c = new Chatroom(goalId, state);
            service.create(c);
            showStatus("Chatroom ajouté avec succès ✅", true);
            clearFields();
        } catch (IllegalArgumentException e) {
            showStatus("Erreur : " + e.getMessage(), false);
        } catch (SQLException e) {
            showStatus("Erreur BD : " + e.getMessage(), false);
        }
    }

    @FXML
    public void updateChatroom() {
        try {
            int id = Integer.parseInt(updateIdField.getText().trim());
            int goalId = Integer.parseInt(goalIdField.getText().trim());
            String state = stateBox.getValue();

            Chatroom c = new Chatroom(goalId, state);
            c.setId(id);
            service.update(c);
            showStatus("Chatroom modifié ✅", true);
            clearFields();
        } catch (IllegalArgumentException e) {
            showStatus("Erreur : " + e.getMessage(), false);
        } catch (SQLException e) {
            showStatus("Erreur BD : " + e.getMessage(), false);
        }
    }

    @FXML
    public void deleteChatroom() {
        try {
            int id = Integer.parseInt(updateIdField.getText().trim());
            service.delete(id);
            showStatus("Chatroom supprimé ✅", true);
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
        goalIdField.clear();
        updateIdField.clear();
        stateBox.setValue("active");
    }
}

package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Message;
import services.chatroom.MessageService;

import java.sql.SQLException;

public class MessageController {

    @FXML private TextField contentField;
    @FXML private TextField chatroomIdField;
    @FXML private TextField authorIdField;
    @FXML private CheckBox pinnedCheck;
    @FXML private TextField updateIdField;
    @FXML private Label statusLabel;

    private MessageService service;

    @FXML
    public void initialize() {
        service = new MessageService();
    }

    @FXML
    public void addMessage() {
        try {
            String content = contentField.getText();
            int chatroomId = Integer.parseInt(chatroomIdField.getText().trim());
            int authorId = Integer.parseInt(authorIdField.getText().trim());

            Message m = new Message(content, chatroomId, authorId);
            service.create(m);
            showStatus("Message envoyé ✅", true);
            clearFields();
        } catch (IllegalArgumentException e) {
            showStatus("Erreur : " + e.getMessage(), false);
        } catch (SQLException e) {
            showStatus("Erreur BD : " + e.getMessage(), false);
        }
    }

    @FXML
    public void updateMessage() {
        try {
            int id = Integer.parseInt(updateIdField.getText().trim());
            String content = contentField.getText();
            boolean pinned = pinnedCheck.isSelected();

            Message m = new Message();
            m.setId(id);
            m.setContent(content);
            m.setPinned(pinned);
            m.setEdited(true);
            service.update(m);
            showStatus("Message modifié ✅", true);
            clearFields();
        } catch (IllegalArgumentException e) {
            showStatus("Erreur : " + e.getMessage(), false);
        } catch (SQLException e) {
            showStatus("Erreur BD : " + e.getMessage(), false);
        }
    }

    @FXML
    public void deleteMessage() {
        try {
            int id = Integer.parseInt(updateIdField.getText().trim());
            service.delete(id);
            showStatus("Message supprimé ✅", true);
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
        contentField.clear();
        chatroomIdField.clear();
        authorIdField.clear();
        updateIdField.clear();
        pinnedCheck.setSelected(false);
    }
}

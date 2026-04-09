package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Message;
import services.MessageService;

import java.sql.SQLException;

public class MessageController {

    @FXML private TextField contentField;
    @FXML private TextField chatroomIdField;
    @FXML private TextField authorIdField;
    @FXML private CheckBox  pinnedCheck;
    @FXML private TextField updateIdField;
    @FXML private Label     statusLabel;

    @FXML private TableView<Message> tableView;
    @FXML private TableColumn<Message, Integer> colId;
    @FXML private TableColumn<Message, String>  colContent;
    @FXML private TableColumn<Message, Integer> colChatroomId;
    @FXML private TableColumn<Message, Integer> colAuthorId;
    @FXML private TableColumn<Message, Boolean> colPinned;
    @FXML private TableColumn<Message, String>  colCreatedAt;

    private MessageService service;

    @FXML
    public void initialize() {
        service = new MessageService();

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colContent.setCellValueFactory(new PropertyValueFactory<>("content"));
        colChatroomId.setCellValueFactory(new PropertyValueFactory<>("chatroomId"));
        colAuthorId.setCellValueFactory(new PropertyValueFactory<>("authorId"));
        colPinned.setCellValueFactory(new PropertyValueFactory<>("pinned"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // clic sur ligne → remplit les champs
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                updateIdField.setText(String.valueOf(selected.getId()));
                contentField.setText(selected.getContent());
                chatroomIdField.setText(String.valueOf(selected.getChatroomId()));
                authorIdField.setText(String.valueOf(selected.getAuthorId()));
                pinnedCheck.setSelected(selected.isPinned());
            }
        });

        loadAll();
    }

    @FXML
    public void addMessage() {
        try {
            String content = contentField.getText();
            int chatroomId = Integer.parseInt(chatroomIdField.getText().trim());
            int authorId   = Integer.parseInt(authorIdField.getText().trim());
            Message m = new Message(content, chatroomId, authorId);
            service.create(m);
            showStatus("Message envoyé ✅", true);
            clearFields();
            loadAll();
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
            Message m = new Message();
            m.setId(id);
            m.setContent(contentField.getText());
            m.setPinned(pinnedCheck.isSelected());
            m.setEdited(true);
            service.update(m);
            showStatus("Message modifié ✅", true);
            clearFields();
            loadAll();
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
            loadAll();
        } catch (NumberFormatException e) {
            showStatus("ID invalide.", false);
        } catch (SQLException e) {
            showStatus("Erreur BD : " + e.getMessage(), false);
        }
    }

    @FXML
    public void loadAll() {
        try {
            tableView.setItems(FXCollections.observableArrayList(service.getAll()));
        } catch (SQLException e) {
            showStatus("Erreur chargement : " + e.getMessage(), false);
        }
    }

    private void showStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().setAll(success ? "status-ok" : "status-err");
    }

    private void clearFields() {
        contentField.clear();
        chatroomIdField.clear();
        authorIdField.clear();
        updateIdField.clear();
        pinnedCheck.setSelected(false);
    }
}

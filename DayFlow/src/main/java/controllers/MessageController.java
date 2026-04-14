package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.chatroom.Message;
import services.chatroom_module.MessageService;

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

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                updateIdField.setText(String.valueOf(sel.getId()));
                contentField.setText(sel.getContent());
                chatroomIdField.setText(String.valueOf(sel.getChatroomId()));
                authorIdField.setText(String.valueOf(sel.getAuthorId()));
                pinnedCheck.setSelected(sel.isPinned());
            }
        });
        loadAll();
    }

    @FXML
    public void addMessage() {
        try {
            Message m = new Message(contentField.getText(),
                    Integer.parseInt(chatroomIdField.getText().trim()),
                    Integer.parseInt(authorIdField.getText().trim()));
            service.create(m);
            showStatus("Message envoyé ✅", true);
            clearFields(); loadAll();
        } catch (Exception e) { showStatus("Erreur : " + e.getMessage(), false); }
    }

    @FXML
    public void updateMessage() {
        try {
            Message m = new Message();
            m.setId(Integer.parseInt(updateIdField.getText().trim()));
            m.setContent(contentField.getText());
            m.setPinned(pinnedCheck.isSelected());
            m.setEdited(true);
            service.update(m);
            showStatus("Message modifié ✅", true);
            clearFields(); loadAll();
        } catch (Exception e) { showStatus("Erreur : " + e.getMessage(), false); }
    }

    @FXML
    public void deleteMessage() {
        try {
            service.delete(Integer.parseInt(updateIdField.getText().trim()));
            showStatus("Message supprimé ✅", true);
            clearFields(); loadAll();
        } catch (Exception e) { showStatus("Erreur : " + e.getMessage(), false); }
    }

    @FXML
    public void loadAll() {
        try {
            tableView.setItems(FXCollections.observableArrayList(service.getAll()));
        } catch (SQLException e) { showStatus("Erreur : " + e.getMessage(), false); }
    }

    private void showStatus(String msg, boolean ok) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().setAll(ok ? "status-ok" : "status-err");
    }

    private void clearFields() {
        contentField.clear(); chatroomIdField.clear();
        authorIdField.clear(); updateIdField.clear();
        pinnedCheck.setSelected(false);
    }
}

package controllers;

<<<<<<< HEAD
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.chatroom.Chatroom;
import services.chatroom_module.ChatroomService;
=======
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Chatroom;
import services.ChatroomService;
>>>>>>> origin/chatroom

import java.sql.SQLException;

public class ChatroomController {

    @FXML private TextField goalIdField;
    @FXML private ComboBox<String> stateBox;
    @FXML private TextField updateIdField;
    @FXML private Label statusLabel;

<<<<<<< HEAD
=======
    @FXML private TableView<Chatroom> tableView;
    @FXML private TableColumn<Chatroom, Integer> colId;
    @FXML private TableColumn<Chatroom, Integer> colGoalId;
    @FXML private TableColumn<Chatroom, String>  colState;
    @FXML private TableColumn<Chatroom, String>  colCreatedAt;

>>>>>>> origin/chatroom
    private ChatroomService service;

    @FXML
    public void initialize() {
        service = new ChatroomService();
        stateBox.getItems().addAll("active", "inactive");
        stateBox.setValue("active");
<<<<<<< HEAD
=======

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colGoalId.setCellValueFactory(new PropertyValueFactory<>("goalId"));
        colState.setCellValueFactory(new PropertyValueFactory<>("state"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // clic sur ligne → remplit les champs
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                updateIdField.setText(String.valueOf(selected.getId()));
                goalIdField.setText(String.valueOf(selected.getGoalId()));
                stateBox.setValue(selected.getState());
            }
        });

        loadAll();
>>>>>>> origin/chatroom
    }

    @FXML
    public void addChatroom() {
        try {
            int goalId = Integer.parseInt(goalIdField.getText().trim());
<<<<<<< HEAD
            String state = stateBox.getValue();

            Chatroom c = new Chatroom(goalId, state);
            service.create(c);
            showStatus("Chatroom ajouté avec succès ✅", true);
            clearFields();
=======
            Chatroom c = new Chatroom(goalId, stateBox.getValue());
            service.create(c);
            showStatus("Chatroom ajouté ✅", true);
            clearFields();
            loadAll();
>>>>>>> origin/chatroom
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
<<<<<<< HEAD
            String state = stateBox.getValue();

            Chatroom c = new Chatroom(goalId, state);
=======
            Chatroom c = new Chatroom(goalId, stateBox.getValue());
>>>>>>> origin/chatroom
            c.setId(id);
            service.update(c);
            showStatus("Chatroom modifié ✅", true);
            clearFields();
<<<<<<< HEAD
=======
            loadAll();
>>>>>>> origin/chatroom
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
<<<<<<< HEAD
=======
            loadAll();
>>>>>>> origin/chatroom
        } catch (NumberFormatException e) {
            showStatus("ID invalide.", false);
        } catch (SQLException e) {
            showStatus("Erreur BD : " + e.getMessage(), false);
        }
    }

<<<<<<< HEAD
    private void showStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
=======
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
>>>>>>> origin/chatroom
    }

    private void clearFields() {
        goalIdField.clear();
        updateIdField.clear();
        stateBox.setValue("active");
    }
}

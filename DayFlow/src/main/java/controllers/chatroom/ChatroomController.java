package controllers.chatroom;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.chatroom.Chatroom;
import services.chatroom.ChatroomService;

import java.sql.SQLException;

public class ChatroomController {

    @FXML private TextField goalIdField;
    @FXML private ComboBox<String> stateBox;
    @FXML private TextField updateIdField;
    @FXML private Label statusLabel;

    @FXML private TableView<Chatroom> tableView;
    @FXML private TableColumn<Chatroom, Integer> colId;
    @FXML private TableColumn<Chatroom, Integer> colGoalId;
    @FXML private TableColumn<Chatroom, String>  colState;
    @FXML private TableColumn<Chatroom, String>  colCreatedAt;

    private ChatroomService service;

    @FXML
    public void initialize() {
        service = new ChatroomService();
        stateBox.getItems().addAll("active", "inactive");
        stateBox.setValue("active");

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colGoalId.setCellValueFactory(new PropertyValueFactory<>("goalId"));
        colState.setCellValueFactory(new PropertyValueFactory<>("state"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                updateIdField.setText(String.valueOf(sel.getId()));
                goalIdField.setText(String.valueOf(sel.getGoalId()));
                stateBox.setValue(sel.getState());
            }
        });
        loadAll();
    }

    @FXML
    public void addChatroom() {
        try {
            Chatroom c = new Chatroom(Integer.parseInt(goalIdField.getText().trim()), stateBox.getValue());
            service.create(c);
            showStatus("Chatroom ajouté ✅", true);
            clearFields(); loadAll();
        } catch (Exception e) { showStatus("Erreur : " + e.getMessage(), false); }
    }

    @FXML
    public void updateChatroom() {
        try {
            Chatroom c = new Chatroom(Integer.parseInt(goalIdField.getText().trim()), stateBox.getValue());
            c.setId(Integer.parseInt(updateIdField.getText().trim()));
            service.update(c);
            showStatus("Chatroom modifié ✅", true);
            clearFields(); loadAll();
        } catch (Exception e) { showStatus("Erreur : " + e.getMessage(), false); }
    }

    @FXML
    public void deleteChatroom() {
        try {
            service.delete(Integer.parseInt(updateIdField.getText().trim()));
            showStatus("Chatroom supprimé ✅", true);
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
        goalIdField.clear(); updateIdField.clear(); stateBox.setValue("active");
    }
}

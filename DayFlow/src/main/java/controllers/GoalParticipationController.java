package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.GoalParticipation;
import services.GoalParticipationService;

import java.sql.SQLException;

public class GoalParticipationController {

    @FXML private TextField userIdField;
    @FXML private TextField goalIdField;
    @FXML private ComboBox<String> roleBox;
    @FXML private ComboBox<String> statusBox;
    @FXML private TextField updateIdField;
    @FXML private Label statusLabel;

    @FXML private TableView<GoalParticipation> tableView;
    @FXML private TableColumn<GoalParticipation, Integer> colId;
    @FXML private TableColumn<GoalParticipation, Integer> colUserId;
    @FXML private TableColumn<GoalParticipation, Integer> colGoalId;
    @FXML private TableColumn<GoalParticipation, String>  colRole;
    @FXML private TableColumn<GoalParticipation, String>  colStatus;
    @FXML private TableColumn<GoalParticipation, String>  colCreatedAt;

    private GoalParticipationService service;

    @FXML
    public void initialize() {
        service = new GoalParticipationService();

        roleBox.getItems().addAll(GoalParticipation.ROLE_MEMBER, GoalParticipation.ROLE_ADMIN, GoalParticipation.ROLE_OWNER);
        statusBox.getItems().addAll(GoalParticipation.STATUS_PENDING, GoalParticipation.STATUS_APPROVED, GoalParticipation.STATUS_REJECTED);
        roleBox.setValue(GoalParticipation.ROLE_MEMBER);
        statusBox.setValue(GoalParticipation.STATUS_APPROVED);

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colGoalId.setCellValueFactory(new PropertyValueFactory<>("goalId"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // clic sur ligne → remplit les champs
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                updateIdField.setText(String.valueOf(selected.getId()));
                userIdField.setText(String.valueOf(selected.getUserId()));
                goalIdField.setText(String.valueOf(selected.getGoalId()));
                roleBox.setValue(selected.getRole());
                statusBox.setValue(selected.getStatus());
            }
        });

        loadAll();
    }

    @FXML
    public void addParticipation() {
        try {
            int userId = Integer.parseInt(userIdField.getText().trim());
            int goalId = Integer.parseInt(goalIdField.getText().trim());
            GoalParticipation gp = new GoalParticipation(userId, goalId);
            gp.setRole(roleBox.getValue());
            gp.setStatus(statusBox.getValue());
            service.create(gp);
            showStatus("Participation ajoutée ✅", true);
            clearFields();
            loadAll();
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
            GoalParticipation gp = new GoalParticipation();
            gp.setId(id);
            gp.setRole(roleBox.getValue());
            gp.setStatus(statusBox.getValue());
            service.update(gp);
            showStatus("Participation modifiée ✅", true);
            clearFields();
            loadAll();
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
        userIdField.clear();
        goalIdField.clear();
        updateIdField.clear();
        roleBox.setValue(GoalParticipation.ROLE_MEMBER);
        statusBox.setValue(GoalParticipation.STATUS_APPROVED);
    }
}

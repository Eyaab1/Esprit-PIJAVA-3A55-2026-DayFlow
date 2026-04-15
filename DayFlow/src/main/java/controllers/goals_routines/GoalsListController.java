package controllers.goals_routines;

import controllers.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.io.IOException;

public class GoalsListController {

    @FXML
    private void initialize() {
        // Initialize goals list data here
    }

    @FXML
    private void onApplyFilters() {
        // Apply search and filter logic
        new Alert(Alert.AlertType.INFORMATION, "Filtres appliqués").showAndWait();
    }

    @FXML
    private void onModifyGoal() {
        new Alert(Alert.AlertType.INFORMATION, "Modifier l'objectif — bientôt disponible").showAndWait();
    }

    @FXML
    private void onDuplicateGoal() {
        new Alert(Alert.AlertType.INFORMATION, "Dupliquer l'objectif — bientôt disponible").showAndWait();
    }

    @FXML
    private void onDeleteGoal() {
        new Alert(Alert.AlertType.INFORMATION, "Supprimer l'objectif — bientôt disponible").showAndWait();
    }

    @FXML
    private void onBackToDashboard() {
        try {
            NavigationManager.show("/user/goals_routines/goals_dashboard.fxml", "DayFlow — Mes objectifs");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }
}

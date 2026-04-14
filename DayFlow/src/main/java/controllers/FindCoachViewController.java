package controllers;

import controllers.navigation.NavigationManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class FindCoachViewController {

    /**
     * Navigation vers la page "Mes demandes"
     */
    @FXML
    private void handleMesDemandes(ActionEvent event) {
        try {
            NavigationManager.show("/views/mes_demandes.fxml", "DayFlow — Mes demandes");
        } catch (IOException | IllegalStateException e) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/mes_demandes.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Mes demandes de coaching");
                stage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}

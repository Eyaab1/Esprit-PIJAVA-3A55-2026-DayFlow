package controllers.account;

import controllers.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ShellController {

    @FXML
    private BorderPane shellRoot;
    @FXML
    private VBox navbarContainer;
    @FXML
    private HBox backBar;
    @FXML
    private Button backButton;

    public BorderPane getShellRoot() {
        return shellRoot;
    }

    @FXML
    private void onBack() {
        try {
            NavigationManager.goBack();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Affiche ou masque le bouton retour (historique de navigation).
     */
    public void setBackNavigationVisible(boolean visible) {
        if (backBar != null) {
            backBar.setVisible(visible);
            backBar.setManaged(visible);
        }
        if (backButton != null) {
            backButton.setDisable(!visible);
        }
        if (shellRoot != null) {
            shellRoot.requestLayout();
        }
    }

    /**
     * Affiche ou masque la navbar globale (masquée sur landing / login / signup).
     */
    public void setGlobalNavbarVisible(boolean visible) {
        if (navbarContainer != null) {
            navbarContainer.setVisible(visible);
            navbarContainer.setManaged(visible);
            if (shellRoot != null) {
                shellRoot.requestLayout();
            }
        }
    }
}

package controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class ShellController {

    @FXML
    private BorderPane shellRoot;
    @FXML
    private VBox navbarContainer;

    public BorderPane getShellRoot() {
        return shellRoot;
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

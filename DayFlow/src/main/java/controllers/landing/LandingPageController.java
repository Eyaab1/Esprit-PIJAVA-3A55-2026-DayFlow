package controllers.landing;

import controllers.auth.AuthNavigation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LandingPageController {

    @FXML
    private ScrollPane mainScroll;
    @FXML
    private VBox anchorFeatures;
    @FXML
    private VBox anchorHowItWorks;
    @FXML
    private VBox anchorTeam;
    @FXML
    private VBox anchorFaq;
    @FXML
    private VBox anchorTestimonials;

    @FXML
    private void onConnexion(ActionEvent e) throws Exception {
        AuthNavigation.showLogin(stageFrom(e));
    }

    @FXML
    private void onSignup(ActionEvent e) throws Exception {
        AuthNavigation.showSignup(stageFrom(e));
    }

    @FXML
    private void onVoirFonctionnalites(ActionEvent e) {
        scrollTo(anchorFeatures);
    }

    @FXML
    private void onNavFonctionnalites(ActionEvent e) {
        scrollTo(anchorFeatures);
    }

    @FXML
    private void onNavComment(ActionEvent e) {
        scrollTo(anchorHowItWorks);
    }

    @FXML
    private void onNavEquipe(ActionEvent e) {
        scrollTo(anchorTeam);
    }

    @FXML
    private void onNavAvis(ActionEvent e) {
        scrollTo(anchorTestimonials);
    }

    @FXML
    private void onNavFaq(ActionEvent e) {
        scrollTo(anchorFaq);
    }

    @FXML
    private void onFooterAccueil(ActionEvent e) {
        mainScroll.setVvalue(0);
    }

    @FXML
    private void onFooterFonctionnalites(ActionEvent e) {
        scrollTo(anchorFeatures);
    }

    @FXML
    private void onFooterEquipe(ActionEvent e) {
        scrollTo(anchorTeam);
    }

    @FXML
    private void onFooterFaq(ActionEvent e) {
        scrollTo(anchorFaq);
    }

    @FXML
    private void onFooterConnexion(ActionEvent e) throws Exception {
        AuthNavigation.showLogin(stageFrom(e));
    }

    @FXML
    private void onFooterInscription(ActionEvent e) throws Exception {
        AuthNavigation.showSignup(stageFrom(e));
    }

    @FXML
    private void onFooterGoogle(@SuppressWarnings("unused") ActionEvent e) {
        /* OAuth Google — à brancher */
    }

    private static Stage stageFrom(ActionEvent e) {
        return (Stage) ((Node) e.getSource()).getScene().getWindow();
    }

    private void scrollTo(Node node) {
        if (mainScroll == null || node == null) {
            return;
        }
        Node content = mainScroll.getContent();
        if (content == null) {
            return;
        }
        double contentH = content.getBoundsInLocal().getHeight();
        double viewportH = mainScroll.getViewportBounds().getHeight();
        if (contentH <= viewportH) {
            return;
        }
        double y = node.getBoundsInParent().getMinY();
        double v = y / (contentH - viewportH);
        mainScroll.setVvalue(Math.max(0, Math.min(1, v)));
    }
}

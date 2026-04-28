package controllers.interaction;

import controllers.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Point d’entrée « Community » : raccourcis vers le fil social et les goals / chat.
 */
public class CommunityController {

    @FXML
    private VBox cardsContainer;

    @FXML
    private void initialize() {
        addCard("📰  Fil de posts", "Voir et commenter les publications de la communauté.",
                "/user/interaction/posts_feed.fxml", "DayFlow — Posts");
        addCard("🎯  Objectifs & groupes", "Créer un goal, rejoindre un objectif et accéder aux chatrooms.",
                "/user/goals_routines/goals_dashboard.fxml", "DayFlow — Goals");
        addCard("💬  Salons de discussion", "Parcourir les objectifs, envoyer une demande au propriétaire et rejoindre le chat une fois accepté.",
                "/user/interaction/community_discussions.fxml", "DayFlow — Discussions");
        addCard("💬  Mes messages", "Ouvrir tes chats liés aux objectifs dont tu fais partie.",
                "/user/chatroom/chatroom_hub.fxml", "DayFlow — Chat");
    }

    private void addCard(String title, String desc, String fxml, String windowTitle) {
        VBox card = new VBox(6);
        card.getStyleClass().add("community-card");
        Label t = new Label(title);
        t.getStyleClass().add("community-card-title");
        Label d = new Label(desc);
        d.getStyleClass().add("community-card-desc");
        d.setWrapText(true);
        card.getChildren().addAll(t, d);
        card.setOnMouseClicked(ev -> go(fxml, windowTitle));
        cardsContainer.getChildren().add(card);
    }

    private void go(String path, String title) {
        try {
            NavigationManager.show(path, title);
        } catch (IOException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            new Alert(Alert.AlertType.ERROR,
                cause.getClass().getSimpleName() + ": " + cause.getMessage()).showAndWait();
            cause.printStackTrace();
        }
    }
}

package controllers.account;

import controllers.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

    @FXML
    public void openChatroom() throws IOException {
        openWindow("/user/chatroom/chatroom.fxml", "Chatrooms");
    }

    @FXML
    public void openMessage() throws IOException {
        openWindow("/user/chatroom/message.fxml", "Messages");
    }

    @FXML
    public void openGoalParticipation() throws IOException {
        openWindow("/user/goals_routines/goalparticipation.fxml", "Participations");
    }

    private void openWindow(String fxmlPath, String title) throws IOException {
        if (NavigationManager.isInitialized()) {
            NavigationManager.show(fxmlPath, title);
            return;
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }
}

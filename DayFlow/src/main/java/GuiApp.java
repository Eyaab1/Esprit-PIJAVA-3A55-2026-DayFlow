import controllers.account.ShellController;
import controllers.navigation.NavigationManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import java.io.IOException;

public class GuiApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader shellLoader = new FXMLLoader(GuiApp.class.getResource("/user/account/app_root.fxml"));
            Parent shellRoot = shellLoader.load();
            ShellController shell = shellLoader.getController();

            Scene scene = new Scene(shellRoot, 1100, 800);
            stage.setTitle("DayFlow");
            stage.setScene(scene);

            NavigationManager.init(stage, shell);
            NavigationManager.show("/user/account/landing.fxml", "DayFlow");

            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN),
                    () -> {
                        try {
                            if (NavigationManager.canGoBack()) {
                                NavigationManager.goBack();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

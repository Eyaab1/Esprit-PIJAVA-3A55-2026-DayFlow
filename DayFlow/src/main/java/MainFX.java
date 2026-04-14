import controllers.ShellController;
import controllers.navigation.NavigationManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader shellLoader = new FXMLLoader(
                getClass().getResource("/user/shell/app_root.fxml")
        );

        Scene scene = new Scene(shellLoader.load(), 1200, 750);

        ShellController shell = shellLoader.getController();

        NavigationManager.init(stage, shell);
        NavigationManager.show("/user/landingpage/landing.fxml", "DayFlow");

        stage.setTitle("DayFlow");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
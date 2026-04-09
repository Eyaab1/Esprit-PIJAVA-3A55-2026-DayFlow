package controllers.auth;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class AuthNavigation {

    private AuthNavigation() {
    }

    public static void showLogin(Stage stage) throws Exception {
        show(stage, "/user/login/login.fxml", "DayFlow — Connexion");
    }

    public static void showSignup(Stage stage) throws Exception {
        show(stage, "/user/signup/signup.fxml", "DayFlow — Inscription");
    }

    public static void showLanding(Stage stage) throws Exception {
        show(stage, "/user/landingpage/landing.fxml", "DayFlow");
    }

    private static void show(Stage stage, String resource, String title) throws Exception {
        Parent root = FXMLLoader.load(AuthNavigation.class.getResource(resource));
        Scene scene = new Scene(root);
        stage.setTitle(title);
        stage.setScene(scene);
    }
}

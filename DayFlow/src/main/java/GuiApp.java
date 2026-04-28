import config.DatabaseMigrator;
import controllers.account.AuthNavigation;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GuiApp extends Application {
    private String pendingResetToken;

    @Override
    public void init() {
        pendingResetToken = extractTokenFromArgs(getParameters().getRaw());
    }

    @Override
    public void start(Stage stage) {
        try {
            DatabaseMigrator.migrate();
            FXMLLoader shellLoader = new FXMLLoader(GuiApp.class.getResource("/user/account/app_root.fxml"));
            Parent shellRoot = shellLoader.load();
            ShellController shell = shellLoader.getController();

            Scene scene = new Scene(shellRoot, 1100, 800);
            stage.setTitle("DayFlow");
            stage.setScene(scene);

            NavigationManager.init(stage, shell);
            if (pendingResetToken != null && !pendingResetToken.isBlank()) {
                AuthNavigation.showResetPassword(pendingResetToken);
            } else {
                NavigationManager.show("/user/account/landing.fxml", "DayFlow");
            }

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

    private static String extractTokenFromArgs(List<String> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }
        for (String arg : args) {
            String token = extractTokenFromSingleArg(arg);
            if (token != null && !token.isBlank()) {
                return token;
            }
        }
        return null;
    }

    private static String extractTokenFromSingleArg(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int tokenIndex = value.indexOf("token=");
        if (tokenIndex < 0) {
            return null;
        }
        String tokenPart = value.substring(tokenIndex + "token=".length());
        int ampIndex = tokenPart.indexOf('&');
        String encodedToken = (ampIndex >= 0 ? tokenPart.substring(0, ampIndex) : tokenPart).trim();
        if (encodedToken.isBlank()) {
            return null;
        }
        return URLDecoder.decode(encodedToken, StandardCharsets.UTF_8);
    }
}

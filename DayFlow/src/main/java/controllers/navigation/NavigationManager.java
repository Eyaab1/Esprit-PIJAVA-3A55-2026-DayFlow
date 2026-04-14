package controllers.navigation;

import controllers.ShellController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Garde une seule {@link javafx.scene.Scene} : on ne remplace que le contenu du {@link StackPane} central.
 * La navbar globale (shell) est affichée ou masquée selon l'écran.
 */
public final class NavigationManager {

    private static Stage primaryStage;
    private static BorderPane shellRoot;
    private static ShellController shellController;

    private NavigationManager() {
    }

    public static void init(Stage stage, ShellController shell) {
        primaryStage = Objects.requireNonNull(stage);
        shellController = Objects.requireNonNull(shell);
        shellRoot = Objects.requireNonNull(shell.getShellRoot());
    }

    public static boolean isInitialized() {
        return shellRoot != null && primaryStage != null;
    }

    public static void show(String resourcePath, String title) throws IOException {
        showAndGetController(resourcePath, title);
    }

    public static <T> T showAndGetController(String resourcePath, String title) throws IOException {
        if (!isInitialized()) {
            throw new IllegalStateException("NavigationManager.init() doit être appelé au démarrage.");
        }
        URL url = NavigationManager.class.getResource(resourcePath);
        if (url == null) {
            throw new IOException("Ressource FXML introuvable : " + resourcePath);
        }
        FXMLLoader loader = new FXMLLoader(url);
        Parent view = loader.load();
        if (view instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
            r.setMaxHeight(Double.MAX_VALUE);
        }
        shellRoot.setCenter(view);
        if (title != null && !title.isBlank()) {
            primaryStage.setTitle(title);
        }
        applyNavbarVisibilityForPath(resourcePath);
        return loader.getController();
    }

    private static void applyNavbarVisibilityForPath(String resourcePath) {
        if (shellController == null || resourcePath == null) {
            return;
        }
        boolean pub = isPublicMarketingOrAuthScreen(resourcePath);
        shellController.setGlobalNavbarVisible(!pub);
    }

    /** Landing, login, inscription : pas de navbar globale (elles ont leur propre en-tête ou formulaire). */
    private static boolean isPublicMarketingOrAuthScreen(String path) {
        return path.contains("/user/landingpage")
                || path.contains("/user/login")
                || path.contains("/user/signup");
    }
}

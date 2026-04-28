package controllers.navigation;

import controllers.account.ShellController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Garde une seule {@link javafx.scene.Scene} : on ne remplace que le contenu du centre.
 * Historique pour le bouton « Retour » (pile des écrans précédents).
 */
public final class NavigationManager {

    private static Stage primaryStage;
    private static BorderPane shellRoot;
    private static ShellController shellController;

    private static String currentPath;
    private static String currentTitle;
    private static final Deque<NavEntry> backStack = new ArrayDeque<>();

    private record NavEntry(String path, String title) {
    }

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

    public static boolean canGoBack() {
        return !backStack.isEmpty();
    }

    public static void goBack() throws IOException {
        if (backStack.isEmpty()) {
            return;
        }
        NavEntry prev = backStack.removeLast();
        loadView(prev.path, prev.title, false);
    }

    /**
     * Efface l'historique puis affiche l'écran (déconnexion, retour accueil racine).
     */
    public static void resetTo(String resourcePath, String title) throws IOException {
        backStack.clear();
        loadView(resourcePath, title, false);
    }

    public static void show(String resourcePath, String title) throws IOException {
        showAndGetController(resourcePath, title);
    }

    public static <T> T showAndGetController(String resourcePath, String title) throws IOException {
        return loadView(resourcePath, title, true);
    }

    private static <T> T loadView(String resourcePath, String title, boolean recordHistory) throws IOException {
        if (!isInitialized()) {
            throw new IllegalStateException("NavigationManager.init() doit être appelé au démarrage.");
        }
        if (recordHistory && currentPath != null && !currentPath.equals(resourcePath)) {
            backStack.addLast(new NavEntry(currentPath, currentTitle));
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
        currentPath = resourcePath;
        currentTitle = title;
        if (title != null && !title.isBlank()) {
            primaryStage.setTitle(title);
        }
        applyNavbarVisibilityForPath(resourcePath);
        refreshBackNavigation();
        return loader.getController();
    }

    private static void refreshBackNavigation() {
        if (shellController != null) {
            shellController.setBackNavigationVisible(canGoBack());
        }
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
        return path.contains("/user/account/landing")
                || path.contains("/user/account/login")
                || path.contains("/user/account/signup")
                || path.contains("/user/account/forgot_password")
                || path.contains("/user/account/reset_password")
                || path.contains("/admin/");
    }
}

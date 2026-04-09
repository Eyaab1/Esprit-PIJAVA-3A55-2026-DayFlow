package controllers.auth;

import controllers.navigation.NavigationManager;

import java.io.IOException;

public final class AuthNavigation {

    private AuthNavigation() {
    }

    public static void showLogin() throws IOException {
        NavigationManager.show("/user/login/login.fxml", "DayFlow — Connexion");
    }

    public static void showSignup() throws IOException {
        NavigationManager.show("/user/signup/signup.fxml", "DayFlow — Inscription");
    }

    public static void showLanding() throws IOException {
        NavigationManager.show("/user/landingpage/landing.fxml", "DayFlow");
    }
}

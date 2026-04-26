package controllers.account;

import controllers.navigation.NavigationManager;

import java.io.IOException;

public final class AuthNavigation {

    private AuthNavigation() {
    }

    public static void showLogin() throws IOException {
        NavigationManager.show("/user/account/login.fxml", "DayFlow — Connexion");
    }

    public static void showSignup() throws IOException {
        NavigationManager.show("/user/account/signup.fxml", "DayFlow — Inscription");
    }

    public static void showForgotPassword() throws IOException {
        NavigationManager.show("/user/account/forgot_password.fxml", "DayFlow — Forgot password");
    }

    public static void showResetPassword() throws IOException {
        NavigationManager.show("/user/account/reset_password.fxml", "DayFlow — Reset password");
    }

    public static void showLanding() throws IOException {
        NavigationManager.resetTo("/user/account/landing.fxml", "DayFlow");
    }
}

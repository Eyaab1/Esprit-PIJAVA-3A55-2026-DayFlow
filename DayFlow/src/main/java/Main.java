import controllers.account.UserController;
import services.account.UserService;

/** Point d’entrée optionnel : test login/inscription en console (pas l’UI JavaFX). */
public class Main {
    public static void main(String[] args) {
        UserService userService = new UserService();
        UserController userController = new UserController(userService);
        userController.startConsole();
    }
}
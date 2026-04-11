import controllers.UserController;
import services.UserServices.UserService;

public class Main {

    public static void main(String[] args) {
        UserService userService = new UserService();
        UserController userController = new UserController(userService);
        userController.startConsole();
    }
}

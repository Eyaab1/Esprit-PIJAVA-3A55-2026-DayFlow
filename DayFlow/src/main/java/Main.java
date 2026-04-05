import controllers.UserController;
import services.UserDao;
import services.UserService;

public class Main {

    public static void main(String[] args) {
        UserService userService = new UserService(new UserDao());
        UserController userController = new UserController(userService);
        userController.startConsole();
    }
}

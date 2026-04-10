package controllers;

import model.user.User;
import services.UserServices.UserService;
import utils.DbConnexion;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Scanner;

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    public void startConsole() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println();
            System.out.println("1 — Connexion");
            System.out.println("2 — Inscription");
            System.out.println("0 — Quitter");
            System.out.print("Choix : ");
            String line = scanner.nextLine().trim();
            switch (line) {
                case "1" -> runLogin(scanner);
                case "2" -> runSignUp(scanner);
                case "0" -> {
                    System.out.println("Au revoir.");
                    DbConnexion.shutdown();
                    return;
                }
                default -> System.out.println("Option invalide.");
            }
        }
    }

    private void runLogin(Scanner scanner) {
        System.out.print("Email : ");
        String email = scanner.nextLine();
        System.out.print("Mot de passe : ");
        String password = scanner.nextLine();
        try {
            Optional<User> user = userService.login(email, password);
            if (user.isPresent()) {
                User u = user.get();
                System.out.println("Connecté : " + u.getFirstName() + " " + u.getLastName()
                        + " (" + u.getEmail() + ") — rôles : " + u.getRoles());
            } else {
                System.out.println("Email ou mot de passe incorrect.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur base de données : " + e.getMessage());
        }
    }

    private void runSignUp(Scanner scanner) {
        System.out.print("Prénom : ");
        String first = scanner.nextLine();
        System.out.print("Nom : ");
        String last = scanner.nextLine();
        System.out.print("Email : ");
        String email = scanner.nextLine();
        System.out.print("Mot de passe (min. 8 caractères) : ");
        String password = scanner.nextLine();
        try {
            User created = userService.signUp(first, last, email, password);
            System.out.println("Compte créé, id = " + created.getId() + " — vous pouvez vous connecter.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.err.println("Erreur base de données : " + e.getMessage());
        }
    }
}

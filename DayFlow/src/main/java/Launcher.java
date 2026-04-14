/**
 * Point d'entrée séparé — nécessaire pour JavaFX avec Java 11+.
 * IntelliJ doit lancer cette classe, pas MainFX directement.
 */
public class Launcher {
    public static void main(String[] args) {
        MainFX.main(args);
    }
}

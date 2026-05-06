package tests;

import services.EmailService;

/**
 * Test manuel de l'API Email DayFlow.
 * Clic droit → Run 'EmailServiceTest.main()'
 *
 * ⚠️ AVANT DE LANCER :
 * 1. Ouvrir EmailService.java
 * 2. Modifier FROM_EMAIL avec ton adresse Gmail
 * 3. Modifier APP_PASSWORD avec ton mot de passe d'application Google
 *    (Google Account → Sécurité → Mots de passe d'application)
 */
public class EmailServiceTest {

    public static void main(String[] args) {
        System.out.println("===== TEST EMAIL API DAYFLOW =====\n");

        // ⚠️ Remplacer par ton vrai email de destination
        String TO_EMAIL = "ayari.mariem.1@esprit.tn";

        try {
            // 🟢 TEST 1 : Email de bienvenue
            System.out.println("Test 1 : Envoi email de bienvenue...");
            EmailService.sendWelcome(TO_EMAIL, "Eya");
            System.out.println("  → Lancé ✅");

            // 🟢 TEST 2 : Participation acceptée
            System.out.println("Test 2 : Participation acceptée...");
            EmailService.sendParticipationAccepted(TO_EMAIL, "Eya", "Objectif Sport");
            System.out.println("  → Lancé ✅");

            // 🟢 TEST 3 : Participation refusée
            System.out.println("Test 3 : Participation refusée...");
            EmailService.sendParticipationRejected(TO_EMAIL, "Eya", "Objectif Lecture");
            System.out.println("  → Lancé ✅");

            // 🟢 TEST 4 : Notification nouveau message
            System.out.println("Test 4 : Notification nouveau message...");
            EmailService.sendNewMessageNotification(
                TO_EMAIL, "Eya", "Mariem",
                "Objectif Fitness",
                "Salut ! N'oublie pas notre réunion demain 😊");
            System.out.println("  → Lancé ✅");

            System.out.println("\n===== TOUS LES TESTS LANCÉS =====");
            System.out.println("⏳ Attente envoi (asynchrone)...");

            // Attendre que les emails soient envoyés (thread daemon)
            Thread.sleep(8000);
            System.out.println("📩 Vérifie ta boîte mail : " + TO_EMAIL);

        } catch (Exception e) {
            System.out.println("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}

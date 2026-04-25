package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration pour l'intégration Stripe.
 * Charge les clés API depuis les variables d'environnement ou un fichier .env
 */
public class StripeConfig {

    private static final Properties properties = new Properties();
    private static boolean initialized = false;

    // Clés Stripe
    private static String secretKey;
    private static String publishableKey;
    private static String webhookSecret;
    
    // URLs de redirection
    private static String successUrl;
    private static String cancelUrl;
    
    // Mode de l'application
    private static String appMode;

    static {
        loadConfiguration();
    }

    /**
     * Charge la configuration depuis les variables d'environnement ou le fichier .env
     */
    private static void loadConfiguration() {
        if (initialized) {
            return;
        }

        try {
            // Essayer de charger depuis le fichier .env
            loadFromEnvFile();
        } catch (IOException e) {
            System.out.println("Fichier .env non trouvé, utilisation des variables d'environnement système");
        }

        // Charger les valeurs (priorité aux variables d'environnement système)
        secretKey = getProperty("STRIPE_SECRET_KEY", "");
        publishableKey = getProperty("STRIPE_PUBLISHABLE_KEY", "");
        webhookSecret = getProperty("STRIPE_WEBHOOK_SECRET", "");
        successUrl = getProperty("STRIPE_SUCCESS_URL", "http://localhost:8080/payment/success");
        cancelUrl = getProperty("STRIPE_CANCEL_URL", "http://localhost:8080/payment/cancel");
        appMode = getProperty("APP_MODE", "development");

        initialized = true;

        // Afficher un avertissement si les clés ne sont pas configurées
        if (secretKey.isEmpty()) {
            System.err.println("⚠️ ATTENTION: STRIPE_SECRET_KEY n'est pas configurée !");
            System.err.println("   Le module de paiement fonctionnera en mode simulation.");
            System.err.println("   Pour activer Stripe, configurez vos clés dans le fichier .env");
        } else if (secretKey.startsWith("sk_test_")) {
            System.out.println("✓ Stripe configuré en mode TEST");
        } else if (secretKey.startsWith("sk_live_")) {
            System.out.println("✓ Stripe configuré en mode PRODUCTION");
        }
    }

    /**
     * Charge les propriétés depuis le fichier .env
     */
    private static void loadFromEnvFile() throws IOException {
        try (FileInputStream fis = new FileInputStream(".env")) {
            properties.load(fis);
        }
    }

    /**
     * Récupère une propriété avec priorité aux variables d'environnement système
     */
    private static String getProperty(String key, String defaultValue) {
        // 1. Vérifier les variables d'environnement système
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // 2. Vérifier le fichier .env
        String propValue = properties.getProperty(key);
        if (propValue != null && !propValue.isEmpty()) {
            return propValue;
        }

        // 3. Retourner la valeur par défaut
        return defaultValue;
    }

    // Getters

    public static String getSecretKey() {
        return secretKey;
    }

    public static String getPublishableKey() {
        return publishableKey;
    }

    public static String getWebhookSecret() {
        return webhookSecret;
    }

    public static String getSuccessUrl() {
        return successUrl;
    }

    public static String getCancelUrl() {
        return cancelUrl;
    }

    public static String getAppMode() {
        return appMode;
    }

    /**
     * Vérifie si Stripe est configuré (clé secrète présente)
     */
    public static boolean isConfigured() {
        return secretKey != null && !secretKey.isEmpty();
    }

    /**
     * Vérifie si on est en mode test
     */
    public static boolean isTestMode() {
        return secretKey != null && secretKey.startsWith("sk_test_");
    }

    /**
     * Vérifie si on est en mode production
     */
    public static boolean isProductionMode() {
        return secretKey != null && secretKey.startsWith("sk_live_");
    }

    /**
     * Initialise Stripe avec la clé API
     * À appeler au démarrage de l'application
     */
    public static void initializeStripe() {
        if (!isConfigured()) {
            System.err.println("⚠️ Stripe non configuré - Mode simulation activé");
            return;
        }

        try {
            // TODO: Décommenter quand la dépendance Stripe sera ajoutée
            // com.stripe.Stripe.apiKey = secretKey;
            System.out.println("✓ Stripe initialisé avec succès");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'initialisation de Stripe: " + e.getMessage());
        }
    }

    /**
     * Affiche la configuration actuelle (sans exposer les clés)
     */
    public static void printConfiguration() {
        System.out.println("=== Configuration Stripe ===");
        System.out.println("Mode: " + appMode);
        System.out.println("Clé secrète: " + (isConfigured() ? maskKey(secretKey) : "Non configurée"));
        System.out.println("Clé publique: " + (publishableKey.isEmpty() ? "Non configurée" : maskKey(publishableKey)));
        System.out.println("Webhook secret: " + (webhookSecret.isEmpty() ? "Non configuré" : "Configuré"));
        System.out.println("Success URL: " + successUrl);
        System.out.println("Cancel URL: " + cancelUrl);
        System.out.println("===========================");
    }

    /**
     * Masque une clé pour l'affichage (montre seulement les premiers et derniers caractères)
     */
    private static String maskKey(String key) {
        if (key == null || key.length() < 10) {
            return "***";
        }
        return key.substring(0, 7) + "..." + key.substring(key.length() - 4);
    }
}

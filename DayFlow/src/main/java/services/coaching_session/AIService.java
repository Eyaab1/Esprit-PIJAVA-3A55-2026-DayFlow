package services.coaching_session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import model.user.User;
import services.account.CoachService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service IA pour analyser une demande de coaching et recommander le coach le plus adapté.
 * <p>
 * La clé OpenAI est lue <strong>uniquement</strong> via {@code System.getenv("OPENAI_API_KEY")}.
 * Ne jamais commiter de clé dans le code ni dans le dépôt.
 * <p>
 * Exemples d'exécution : définir la variable <em>avant</em> le démarrage de la JVM
 * (PowerShell, configuration système Windows, ou variables d'environnement dans
 * l'exécution Cursor/IntelliJ). Pour un backend Spring Boot séparé, préférer
 * {@code ${OPENAI_API_KEY}} dans la configuration, sans mettre de valeur en clair dans Git.
 */
public class AIService {

    private static final String HF_URL = "https://router.huggingface.co/hf-inference/models/facebook/bart-large-mnli";

    

    private final CoachService coachService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;

    public AIService() {
        this(new CoachService(), readHuggingFaceApiKey());
    }

    public AIService(CoachService coachService, String apiKey) {
        this.coachService = coachService;
        this.apiKey = normalizeApiKey(apiKey);
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        System.out.println("HUGGINGFACE_API_KEY loaded: " + (this.apiKey != null));
    }

    /**
     * Clé lue exclusivement depuis l'environnement du processus (aucune clé en dur).
     */
    private static String readHuggingFaceApiKey() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();
        String key = dotenv.get("HUGGINGFACE_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getenv("HUGGINGFACE_API_KEY");
        }
        return normalizeApiKey(key);
    }

    private static String normalizeApiKey(String key) {
        if (key == null) {
            return null;
        }
        String t = key.trim();
        return t.isEmpty() ? null : t;
    }

    public RecommendationResult recommendCoach(String userMessage) throws SQLException, IOException, InterruptedException {
        validateMessage(userMessage);
        AIAnalysis analysis;
        try {
            analysis = analyzeUserMessage(userMessage);
        } catch (Exception e) {
            // Fallback local : la recommandation reste disponible même si l'API IA est indisponible.
            String fallbackNeed = inferNeedFromMessage(userMessage);
            analysis = new AIAnalysis(fallbackNeed, new ArrayList<>(tokenize(userMessage)));
            System.out.println("AI fallback active: " + e.getMessage());
        }

        List<User> coaches = coachService.getAllCoaches();
        System.out.println("========== COACHS DISPONIBLES ==========");
System.out.println("Nombre de coachs = " + coaches.size());

for (User coach : coaches) {
    System.out.println(
            "Coach: " + coach.getFirstName()
            + " " + coach.getLastName()
            + " | speciality: " + coach.getSpeciality()
            + " | bio: " + coach.getBio()
            + " | rating: " + coach.getRating()
            + " | reviewCount: " + coach.getReviewCount()
    );
}

System.out.println("========================================");
        if (coaches.isEmpty()) {
            throw new IllegalStateException("Aucun coach disponible pour le matching.");
        }

        List<CoachScore> scores = new ArrayList<>();
        for (User coach : coaches) {
            int score = computeCompatibilityScore(userMessage, coach);
            String why = buildCoachReason(analysis.detectedNeed(), coach);
            scores.add(new CoachScore(coach, score, why));
        }

        scores.sort(Comparator.comparingInt(CoachScore::score).reversed());

CoachScore best = scores.stream()
        .filter(score -> score.score() > 0)
        .findFirst()
        .orElse(null);

if (best == null) {
    throw new IllegalStateException(
            "Aucun coach compatible trouvé pour cette demande."
    );
}

        String justification = best.justification()
                + " Besoin détecté: " + analysis.detectedNeed() + ".";

        return new RecommendationResult(
                analysis.detectedNeed(),
                best.coach(),
                best.score(),
                justification,
                scores
        );
    }

    private AIAnalysis analyzeUserMessage(String userMessage) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("HUGGINGFACE_API_KEY manquante. Ajoute-la dans les variables d'environnement.");
        }
    
          String requestBody = objectMapper.createObjectNode()
        .put("inputs", userMessage)
        .set("parameters", objectMapper.createObjectNode()
                .set("candidate_labels",
                        objectMapper.createArrayNode()
                                .add("Gestion du stress")
                                .add("Confiance en soi")
                                .add("Développement personnel")
                                .add("Motivation")
                                .add("Organisation")
                                .add("Gestion des émotions")
                                .add("Burnout")
                                .add("Anxiété")
                )
        )
        .toString();

HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(HF_URL))
        .timeout(Duration.ofSeconds(25))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + apiKey)
        .POST(HttpRequest.BodyPublishers.ofString(
                requestBody,
                StandardCharsets.UTF_8
        ))
        .build(); 
                              
       

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Erreur Hugging Face (" + response.statusCode() + "): " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String need = "Besoin général";

if (root.isArray() && root.size() > 0) {
    JsonNode first = root.get(0);

    if (first.has("label")) {
        need = first.get("label").asText();
    } 
    else if (first.has("generated_text")) {
        need = first.get("generated_text").asText();
    }
}

List<String> keywords = new ArrayList<>();
keywords.add(need);
      

        if (keywords.isEmpty()) {
            keywords.add(need);
        }

        return new AIAnalysis(need.trim(), deduplicateLowercase(keywords));
    }

    private String inferNeedFromMessage(String userMessage) {
        Set<String> tokens = tokenize(userMessage);
        if (containsAny(tokens, "stress", "pression", "surcharge", "fatigue")) {
            return "Gestion du stress";
        }
        if (containsAny(tokens, "confiance", "estime", "timide", "oser")) {
            return "Confiance en soi";
        }
        if (containsAny(tokens, "organisation", "planifier", "discipline", "procrastination")) {
            return "Organisation";
        }
        if (containsAny(tokens, "motivation", "demotive", "objectif", "ambition")) {
            return "Motivation";
        }
        if (containsAny(tokens, "emotion", "emotions", "colere", "tristesse")) {
            return "Gestion des émotions";
        }
        if (containsAny(tokens, "burnout", "epuisement", "surmenage")) {
            return "Burnout";
        }
        if (containsAny(tokens, "anxiete", "angoisse", "panique", "peur")) {
            return "Anxiété";
        }
        return "Développement personnel";
    }

    private boolean containsAny(Set<String> tokens, String... candidates) {
        for (String candidate : candidates) {
            if (tokens.contains(normalize(candidate))) {
                return true;
            }
        }
        return false;
    }

private int computeCompatibilityScore(String userMessage, User coach) {

    Set<String> messageTokens = tokenize(userMessage);

    Set<String> coachTokens = tokenize(
            safe(coach.getSpeciality()) + " " +
            safe(coach.getBio())
    );

    if (messageTokens.isEmpty() || coachTokens.isEmpty()) {
        return 0;
    }

    long matches = messageTokens.stream()
            .filter(coachTokens::contains)
            .count();

    int score = (int) ((matches * 100.0) / messageTokens.size());

    if (score == 0) {
        return 0;
    }

    return Math.min(score, 100);
}

    

   

    private String buildCoachReason(String detectedNeed, User coach) {
        String fullName = formatCoachName(coach);
        String speciality = !safe(coach.getSpeciality()).isBlank()
                ? coach.getSpeciality()
                : "coaching personnalisé";
        return fullName + " est pertinent pour \"" + detectedNeed + "\" grâce à sa spécialité: " + speciality + ".";
    }

    private String formatCoachName(User coach) {
        String first = safe(coach.getFirstName());
        String last = safe(coach.getLastName());
        String full = (first + " " + last).trim();
        return full.isBlank() ? "Coach #" + coach.getId() : full;
    }

    private static Set<String> tokenize(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return new LinkedHashSet<>();
        }
        String[] parts = normalized.split("\\s+");
        Set<String> out = new LinkedHashSet<>();
        for (String part : parts) {
            if (part.length() >= 3) {
                out.add(part);
            }
        }
        return out;
    }

    private static List<String> deduplicateLowercase(List<String> values) {
        return values.stream()
                .map(AIService::normalize)
                .filter(v -> !v.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private static String normalize(String value) {
        String text = safe(value).toLowerCase(Locale.ROOT);
        String noAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return noAccents.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void validateMessage(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("Le message utilisateur est vide.");
        }
    }

    private record AIAnalysis(String detectedNeed, List<String> keywords) {
    }

    public record CoachScore(User coach, int score, String justification) {
    }

    public record RecommendationResult(
            String detectedNeed,
            User recommendedCoach,
            int compatibilityScore,
            String justification,
            List<CoachScore> ranking
    ) {
    }
}

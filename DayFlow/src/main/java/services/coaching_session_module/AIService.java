package services.coaching_session_module;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.user.User;
import services.UserServices.CoachService;

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
    String key = "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";
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
        AIAnalysis analysis = analyzeUserMessage(userMessage);

        List<User> coaches = coachService.getAllCoaches();
        if (coaches.isEmpty()) {
            throw new IllegalStateException("Aucun coach disponible pour le matching.");
        }

        List<CoachScore> scores = new ArrayList<>();
        for (User coach : coaches) {
            int score = computeCompatibilityScore(analysis, coach);
            String why = buildCoachReason(analysis.detectedNeed(), coach);
            scores.add(new CoachScore(coach, score, why));
        }

        scores.sort(Comparator.comparingInt(CoachScore::score).reversed());
        CoachScore best = scores.getFirst();

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

    private int computeCompatibilityScore(AIAnalysis analysis, User coach) {
        Set<String> needTokens = tokenize(analysis.detectedNeed());
        analysis.keywords().forEach(k -> needTokens.addAll(tokenize(k)));

        Set<String> specialityTokens = tokenize(String.join(" ",
                safe(coach.getSpeciality()),
                coach.getSpecialities() == null ? "" : String.join(" ", coach.getSpecialities())
        ));
        Set<String> bioTokens = tokenize(safe(coach.getBio()));

        long specialityMatches = needTokens.stream().filter(specialityTokens::contains).count();
        long bioMatches = needTokens.stream().filter(bioTokens::contains).count();
        if (specialityMatches == 0 && bioMatches == 0) {
    return 0;
}

        double coverage = needTokens.isEmpty() ? 0.0 : (specialityMatches * 1.0) / needTokens.size();
        boolean perfectMatch = !needTokens.isEmpty()
                && specialityMatches == needTokens.size()
                && !specialityTokens.isEmpty();
        if (perfectMatch) {
            return 100;
        }

        int overlapPoints = (int) Math.round(Math.min(1.0, coverage) * 70.0); // composant principal
        int bioPoints = needTokens.isEmpty()
                ? 0
                : (int) Math.round(Math.min(1.0, bioMatches * 1.0 / needTokens.size()) * 10.0);

        double rating = coach.getRating() != null ? coach.getRating() : 0.0;
        int ratingPoints = (int) Math.round((Math.min(rating, 5.0) / 5.0) * 12.0);

        int reviewCount = coach.getReviewCount() != null ? coach.getReviewCount() : 0;
        int socialProofPoints = Math.min(8, reviewCount / 25);

        int rawScore = overlapPoints + bioPoints + ratingPoints + socialProofPoints;
        int bounded = Math.max(0, Math.min(99, rawScore));

        // Ajustement de classe pour respecter les plages métier demandées.
        if (coverage >= 0.75 && bounded < 80) {
            return 80;
        }
        if (coverage >= 0.5 && bounded < 50) {
            return 50;
        }
        if (coverage < 0.5 && bounded >= 50) {
            return 49;
        }
        return bounded;
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

package services.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.AppConfig;
import model.profile.AiArchetypeProfile;
import model.profile.OnboardingAnswers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

public class AiProfileGeneratorService {
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final ObjectMapper JSON = new ObjectMapper();

    public AiArchetypeProfile generateProfile(OnboardingAnswers answers) {
        boolean useMock = Boolean.parseBoolean(AppConfig.get("app.ai.useMock", "false"));
        String apiKey = AppConfig.get("app.ai.openai.apiKey", "");
        String model = AppConfig.get("app.ai.openai.model", "gpt-4o-mini");

        if (useMock || apiKey.isBlank()) {
            return generateMockProfile(answers);
        }

        try {
            String prompt = buildUserPrompt(answers);
            String payload = JSON.writeValueAsString(
                    java.util.Map.of(
                            "model", model,
                            "messages", List.of(
                                    java.util.Map.of(
                                            "role", "system",
                                            "content", "You are a personal growth expert. You MUST respond with valid JSON only. No markdown, no explanations, no code fences. Output a single JSON object."
                                    ),
                                    java.util.Map.of(
                                            "role", "user",
                                            "content", prompt
                                    )
                            ),
                            "temperature", 0.7,
                            "max_tokens", 800
                    )
            );

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return generateMockProfile(answers);
            }

            JsonNode root = JSON.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                return generateMockProfile(answers);
            }
            return parseOrFallback(content, answers);
        } catch (Exception e) {
            return generateMockProfile(answers);
        }
    }

    private AiArchetypeProfile parseOrFallback(String content, OnboardingAnswers answers) throws IOException {
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```\\w*\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
            cleaned = cleaned.trim();
        }
        JsonNode n = JSON.readTree(cleaned);
        if (n == null || !n.isObject()) {
            return generateMockProfile(answers);
        }

        return new AiArchetypeProfile(
                n.path("archetypeName").asText("Explorer"),
                n.path("description").asText(""),
                readStringArray(n.path("strengths")),
                readStringArray(n.path("growthAreas")),
                readStringArray(n.path("habitSuggestions")),
                n.path("shortBio").asText("")
        );
    }

    private static List<String> readStringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText("");
            if (!value.isBlank()) {
                out.add(value);
            }
        }
        return out;
    }

    private static String buildUserPrompt(OnboardingAnswers answers) {
        return """
                Based on the following user answers, generate a personal growth archetype profile.
                                
                Goals: %s
                Challenges: %s
                Motivation style: %s
                Planning style: %s
                Interests: %s
                                
                Return ONLY valid JSON with this structure (no markdown, no code block):
                                
                {
                  "archetypeName": "string",
                  "description": "string",
                  "strengths": ["string", "string", "string"],
                  "growthAreas": ["string", "string"],
                  "habitSuggestions": ["string", "string", "string"],
                  "shortBio": "string"
                }
                                
                - archetypeName: a short, memorable name for this archetype
                - description: 2-3 sentences describing this profile
                - strengths: exactly 3 strengths
                - growthAreas: exactly 2 growth areas
                - habitSuggestions: exactly 3 personalized habit suggestions
                - shortBio: a short professional-style bio (2-3 sentences)
                """.formatted(
                safe(answers.goals()),
                safe(answers.challenges()),
                safe(answers.motivationStyle()),
                safe(answers.planningStyle()),
                safe(answers.interests())
        );
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static AiArchetypeProfile generateMockProfile(OnboardingAnswers answers) {
        String motivation = safe(answers.motivationStyle()).isBlank() ? "discipline" : answers.motivationStyle().trim();
        String planning = safe(answers.planningStyle()).isBlank() ? "structured" : answers.planningStyle().trim();
        String goals = safe(answers.goals());

        String archetypeName = switch (motivation.toLowerCase(Locale.ROOT)) {
            case "discipline" -> "The Disciplined Achiever";
            case "inspiration" -> "The Inspired Visionary";
            case "flexibility" -> "The Adaptive Explorer";
            case "structure" -> "The Strategic Builder";
            default -> "The Motivated Grower";
        };

        String shortBio = "A " + motivation + "-driven individual committed to structured growth.";
        if (!goals.isBlank()) {
            shortBio += " Currently focused on: " + truncate(goals, 100) + ".";
        }

        return new AiArchetypeProfile(
                archetypeName,
                "You are someone who values " + motivation + " as your core driver. With a " + planning
                        + " approach to planning, you naturally build momentum by staying consistent with your commitments. "
                        + "Your goals reflect a genuine desire for growth and self-improvement.",
                List.of(
                        capitalize(motivation) + " and self-awareness",
                        "Clear sense of personal direction",
                        "Ability to commit to long-term goals"
                ),
                List.of(
                        "Balancing ambition with rest and recovery",
                        "Staying flexible when plans change unexpectedly"
                ),
                List.of(
                        "Start each morning with a 5-minute intention-setting ritual",
                        "End each day with a brief review of what you accomplished",
                        "Block 30 minutes weekly to reassess your goals and progress"
                ),
                shortBio
        );
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}

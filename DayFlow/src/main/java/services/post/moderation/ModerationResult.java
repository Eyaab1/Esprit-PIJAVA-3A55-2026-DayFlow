package services.post.moderation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ModerationResult {

    private final boolean allowed;
    private final double threshold;
    private final Map<String, Double> toxicityScores;
    private final List<String> flaggedAttributes;
    private final String highestAttribute;
    private final double highestScore;
    private final String source;
    private final LocalDateTime analyzedAt;

    public ModerationResult(boolean allowed,
                            double threshold,
                            Map<String, Double> toxicityScores,
                            List<String> flaggedAttributes,
                            String highestAttribute,
                            double highestScore,
                            String source,
                            LocalDateTime analyzedAt) {
        this.allowed = allowed;
        this.threshold = threshold;
        this.toxicityScores = new LinkedHashMap<>(toxicityScores != null ? toxicityScores : Map.of());
        this.flaggedAttributes = new ArrayList<>(flaggedAttributes != null ? flaggedAttributes : List.of());
        this.highestAttribute = highestAttribute;
        this.highestScore = highestScore;
        this.source = source;
        this.analyzedAt = analyzedAt != null ? analyzedAt : LocalDateTime.now();
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isRejected() {
        return !allowed;
    }

    public double getThreshold() {
        return threshold;
    }

    public Map<String, Double> getToxicityScores() {
        return new LinkedHashMap<>(toxicityScores);
    }

    public List<String> getFlaggedAttributes() {
        return new ArrayList<>(flaggedAttributes);
    }

    public String getHighestAttribute() {
        return highestAttribute;
    }

    public double getHighestScore() {
        return highestScore;
    }

    public String getSource() {
        return source;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public String getUserMessage() {
        return "Le contenu contient un langage inapproprié et ne peut pas être publié ou mis à jour.";
    }

    public static ModerationResult merge(String source, double threshold, ModerationResult... results) {
        Map<String, Double> mergedScores = new LinkedHashMap<>();
        LinkedHashSet<String> mergedFlags = new LinkedHashSet<>();
        String highestAttribute = null;
        double highestScore = 0.0d;

        if (results != null) {
            for (ModerationResult result : results) {
                if (result == null) {
                    continue;
                }
                for (Map.Entry<String, Double> entry : result.getToxicityScores().entrySet()) {
                    mergedScores.merge(entry.getKey(), entry.getValue(), Math::max);
                }
                mergedFlags.addAll(result.getFlaggedAttributes());
                if (result.getHighestScore() > highestScore) {
                    highestScore = result.getHighestScore();
                    highestAttribute = result.getHighestAttribute();
                }
            }
        }

        for (Map.Entry<String, Double> entry : mergedScores.entrySet()) {
            if (entry.getValue() != null && entry.getValue() >= threshold) {
                mergedFlags.add(entry.getKey());
            }
            if (entry.getValue() != null && entry.getValue() >= highestScore) {
                highestScore = entry.getValue();
                highestAttribute = entry.getKey();
            }
        }

        boolean allowed = highestScore < threshold;
        return new ModerationResult(
                allowed,
                threshold,
                mergedScores,
                new ArrayList<>(mergedFlags),
                highestAttribute,
                highestScore,
                source,
                LocalDateTime.now()
        );
    }

    public static ModerationResult fromScores(String source, double threshold, Map<String, Double> scores) {
        Map<String, Double> safeScores = new LinkedHashMap<>(scores != null ? scores : Map.of());
        List<String> flagged = new ArrayList<>();
        String highestAttribute = null;
        double highestScore = 0.0d;

        for (Map.Entry<String, Double> entry : safeScores.entrySet()) {
            Double score = entry.getValue();
            if (score == null) {
                continue;
            }
            if (score >= threshold) {
                flagged.add(entry.getKey());
            }
            if (highestAttribute == null || score > highestScore) {
                highestAttribute = entry.getKey();
                highestScore = score;
            }
        }

        return new ModerationResult(
                highestScore < threshold,
                threshold,
                safeScores,
                flagged,
                highestAttribute,
                highestScore,
                source,
                LocalDateTime.now()
        );
    }

    @Override
    public String toString() {
        return "ModerationResult{" +
                "allowed=" + allowed +
                ", threshold=" + threshold +
                ", toxicityScores=" + toxicityScores +
                ", flaggedAttributes=" + flaggedAttributes +
                ", highestAttribute='" + highestAttribute + '\'' +
                ", highestScore=" + highestScore +
                ", source='" + source + '\'' +
                ", analyzedAt=" + analyzedAt +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowed, threshold, toxicityScores, flaggedAttributes, highestAttribute, highestScore, source, analyzedAt);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ModerationResult other)) {
            return false;
        }
        return allowed == other.allowed
                && Double.compare(other.threshold, threshold) == 0
                && Double.compare(other.highestScore, highestScore) == 0
                && Objects.equals(toxicityScores, other.toxicityScores)
                && Objects.equals(flaggedAttributes, other.flaggedAttributes)
                && Objects.equals(highestAttribute, other.highestAttribute)
                && Objects.equals(source, other.source)
                && Objects.equals(analyzedAt, other.analyzedAt);
    }
}

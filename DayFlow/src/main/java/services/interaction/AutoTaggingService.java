package services.interaction;

import model.interaction.Tag;
import services.interaction.ai.OpenAiSemanticClient;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

public class AutoTaggingService {

    private static final Logger LOG = Logger.getLogger(AutoTaggingService.class.getName());
    private static final int MAX_TAGS = 2;
    private static final int MIN_TAGS = 1;
    private static final double REUSE_THRESHOLD = 0.84d;
    private static final double BACKFILL_EXISTING_THRESHOLD = 0.72d;

    private final TagService tagService;
    private final OpenAiSemanticClient semanticClient;
    private final boolean failOpen;

    public AutoTaggingService() {
        this(new TagService(), new OpenAiSemanticClient(), readFailOpen());
    }

    public AutoTaggingService(TagService tagService, OpenAiSemanticClient semanticClient, boolean failOpen) {
        this.tagService = tagService;
        this.semanticClient = semanticClient;
        this.failOpen = failOpen;
    }

    public List<Tag> generateAndAttachTags(int postId, String title, String content) throws SQLException {
        try {
            return generateAndAttachTagsInternal(postId, title, content);
        } catch (SQLException e) {
            if (!failOpen) {
                throw e;
            }
            debug("Auto-tagging provider failed, fail-open active. Using fallback tag. reason=" + e.getMessage());
            return attachFallbackTag(postId);
        }
    }

    private List<Tag> generateAndAttachTagsInternal(int postId, String title, String content) throws SQLException {
        String postText = (title == null ? "" : title.trim()) + "\n\n" + (content == null ? "" : content.trim());
        debug("Semantic source text: " + preview(postText));

        List<Double> postEmbedding = semanticClient.embedding(postText);
        if (postEmbedding.isEmpty()) {
            throw new SQLException("Unable to generate post embedding for auto-tagging.");
        }

        List<Tag> existingTags = tagService.getAllTags();
        List<TagVector> existingTagVectors = new ArrayList<>();
        for (Tag t : existingTags) {
            if (t.getName() == null || t.getName().isBlank()) {
                continue;
            }
            List<Double> v = semanticClient.embedding(t.getName());
            if (!v.isEmpty()) {
                existingTagVectors.add(new TagVector(t, v));
            }
        }

        List<String> suggested = sanitizeCandidates(semanticClient.suggestTagCandidates(title, content, MAX_TAGS));
        debug("Suggested tags: " + suggested);

        List<Tag> selected = new ArrayList<>();
        Set<String> seenNames = new LinkedHashSet<>();

        for (String candidate : suggested) {
            List<Double> candidateEmbedding = semanticClient.embedding(candidate);
            if (candidateEmbedding.isEmpty()) {
                continue;
            }

            Match best = bestMatch(candidateEmbedding, existingTagVectors);
            if (best != null) {
                debug("Similarity candidate=\"" + candidate + "\" vs existing=\"" +
                        best.tag().getName() + "\" score=" + format(best.score()));
            } else {
                debug("Similarity candidate=\"" + candidate + "\" no existing match");
            }

            Tag resolved;
            if (best != null && best.score() >= REUSE_THRESHOLD) {
                resolved = best.tag();
                debug("Reused existing tag: " + resolved.getName());
            } else {
                resolved = createNewTag(candidate);
                debug("Created new tag: " + resolved.getName());
                List<Double> newVec = semanticClient.embedding(resolved.getName());
                if (!newVec.isEmpty()) {
                    existingTagVectors.add(new TagVector(resolved, newVec));
                }
            }

            if (seenNames.add(normalize(resolved.getName()))) {
                selected.add(resolved);
            }
            if (selected.size() >= MAX_TAGS) {
                break;
            }
        }

        if (selected.isEmpty()) {
            List<TagScore> ranked = rankExistingByPostSimilarity(postEmbedding, existingTagVectors);
            for (TagScore scored : ranked) {
                if (scored.score() < BACKFILL_EXISTING_THRESHOLD) {
                    break;
                }
                if (seenNames.add(normalize(scored.tag().getName()))) {
                    selected.add(scored.tag());
                }
                if (selected.size() >= MIN_TAGS) {
                    break;
                }
            }
        }

        if (selected.isEmpty()) {
            Tag fallback = createNewTag("General Wellbeing");
            debug("No strong semantic match, fallback created: General Wellbeing");
            selected.add(fallback);
        }

        List<Tag> attached = new ArrayList<>();
        for (Tag t : selected) {
            try {
                tagService.attachTagToPost(postId, t.getId());
                attached.add(t);
                debug("Attached tag to post#" + postId + ": " + t.getName());
            } catch (SQLException e) {
                if (!e.getMessage().toLowerCase(Locale.ROOT).contains("already associated")) {
                    throw e;
                }
                debug("Tag already attached, skipped: " + t.getName());
            }
        }
        return attached;
    }

    private static void debug(String message) {
        String line = "[AutoTagging] " + message;
        LOG.info(line);
        System.out.println(line);
    }

    private static boolean readFailOpen() {
        try (InputStream inputStream = AutoTaggingService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                return true;
            }
            java.util.Properties p = new java.util.Properties();
            p.load(inputStream);
            return Boolean.parseBoolean(p.getProperty("AUTO_TAGGING_FAIL_OPEN", "true").trim());
        } catch (IOException ignored) {
            return true;
        }
    }

    private List<Tag> attachFallbackTag(int postId) throws SQLException {
        Tag fallback = findExistingTagByName("General Wellbeing");
        if (fallback == null) {
            fallback = findExistingTagByName("Mental Health");
        }
        if (fallback == null) {
            fallback = createNewTag("General Wellbeing");
            debug("Fallback tag created: " + fallback.getName());
        } else {
            debug("Fallback tag reused: " + fallback.getName());
        }
        try {
            tagService.attachTagToPost(postId, fallback.getId());
            debug("Fallback tag attached to post#" + postId + ": " + fallback.getName());
        } catch (SQLException ex) {
            if (!ex.getMessage().toLowerCase(Locale.ROOT).contains("already associated")) {
                throw ex;
            }
            debug("Fallback tag already attached to post#" + postId + ": " + fallback.getName());
        }
        return List.of(fallback);
    }

    private Tag findExistingTagByName(String name) throws SQLException {
        for (Tag t : tagService.getAllTags()) {
            if (t.getName() != null && t.getName().trim().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    private Tag createNewTag(String name) throws SQLException {
        Tag tag = new Tag();
        tag.setName(cleanTagName(name));
        tag.setUsageCount(0);
        tagService.addTag(tag);
        return tag;
    }

    private static List<String> sanitizeCandidates(List<String> raw) {
        Set<String> uniq = new LinkedHashSet<>();
        for (String s : raw) {
            if (s == null) {
                continue;
            }
            String cleaned = cleanTagName(s);
            if (cleaned.isBlank()) {
                continue;
            }
            int tokenCount = cleaned.split("\\s+").length;
            if (tokenCount > 3) {
                continue;
            }
            uniq.add(cleaned);
            if (uniq.size() >= MAX_TAGS) {
                break;
            }
        }
        return new ArrayList<>(uniq);
    }

    private static String cleanTagName(String input) {
        String value = input == null ? "" : input.trim();
        value = value.replaceAll("[^\\p{L}\\p{N}\\s\\-]", " ");
        value = value.replaceAll("\\s+", " ").trim();
        if (value.isEmpty()) {
            return "";
        }
        String[] parts = value.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(" ");
            }
            out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return out.toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static Match bestMatch(List<Double> candidateVector, List<TagVector> existingTagVectors) {
        return existingTagVectors.stream()
                .map(v -> new Match(v.tag(), cosineSimilarity(candidateVector, v.vector())))
                .max(Comparator.comparingDouble(Match::score))
                .orElse(null);
    }

    private static List<TagScore> rankExistingByPostSimilarity(List<Double> postVector, List<TagVector> existingTagVectors) {
        return existingTagVectors.stream()
                .map(v -> new TagScore(v.tag(), cosineSimilarity(postVector, v.vector())))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();
    }

    private static double cosineSimilarity(List<Double> a, List<Double> b) {
        int size = Math.min(a.size(), b.size());
        if (size == 0) {
            return 0.0d;
        }
        double dot = 0.0d;
        double normA = 0.0d;
        double normB = 0.0d;
        for (int i = 0; i < size; i++) {
            double av = a.get(i);
            double bv = b.get(i);
            dot += av * bv;
            normA += av * av;
            normB += bv * bv;
        }
        if (normA <= 0.0d || normB <= 0.0d) {
            return 0.0d;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static String preview(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private record TagVector(Tag tag, List<Double> vector) {
    }

    private record Match(Tag tag, double score) {
    }

    private record TagScore(Tag tag, double score) {
    }
}

package model.profile;

import java.util.ArrayList;
import java.util.List;

public class AiArchetypeProfile {
    private String archetypeName;
    private String description;
    private List<String> strengths;
    private List<String> growthAreas;
    private List<String> habitSuggestions;
    private String shortBio;

    public AiArchetypeProfile() {
        this("Explorer", "", List.of(), List.of(), List.of(), "");
    }

    public AiArchetypeProfile(String archetypeName,
                              String description,
                              List<String> strengths,
                              List<String> growthAreas,
                              List<String> habitSuggestions,
                              String shortBio) {
        this.archetypeName = archetypeName == null || archetypeName.isBlank() ? "Explorer" : archetypeName;
        this.description = description == null ? "" : description;
        this.strengths = safeList(strengths);
        this.growthAreas = safeList(growthAreas);
        this.habitSuggestions = safeList(habitSuggestions);
        this.shortBio = shortBio == null ? "" : shortBio;
    }

    public String getArchetypeName() {
        return archetypeName;
    }

    public void setArchetypeName(String archetypeName) {
        this.archetypeName = archetypeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getStrengths() {
        return safeList(strengths);
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = safeList(strengths);
    }

    public List<String> getGrowthAreas() {
        return safeList(growthAreas);
    }

    public void setGrowthAreas(List<String> growthAreas) {
        this.growthAreas = safeList(growthAreas);
    }

    public List<String> getHabitSuggestions() {
        return safeList(habitSuggestions);
    }

    public void setHabitSuggestions(List<String> habitSuggestions) {
        this.habitSuggestions = safeList(habitSuggestions);
    }

    public String getShortBio() {
        return shortBio;
    }

    public void setShortBio(String shortBio) {
        this.shortBio = shortBio;
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}

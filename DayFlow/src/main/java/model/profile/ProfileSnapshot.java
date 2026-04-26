package model.profile;

import java.util.List;

/**
 * Lightweight input model for profile scoring logic.
 */
public record ProfileSnapshot(
        String bio,
        List<String> skills,
        List<String> preferences,
        int weeklyActivityCount
) {
}

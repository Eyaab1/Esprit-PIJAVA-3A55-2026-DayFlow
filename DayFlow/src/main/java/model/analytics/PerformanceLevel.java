package model.analytics;

/**
 * Performance Level Enum
 * Represents the performance evaluation classification for goals
 */
public enum PerformanceLevel {
    EXCELLENT_PROGRESS("Excellent Progress", "🌟"),
    ON_TRACK("On Track", "✅"),
    NEEDS_ATTENTION("Needs Attention", "⚠️"),
    CRITICAL_DELAY("Critical Delay", "🚨");

    private final String displayName;
    private final String icon;

    PerformanceLevel(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return icon + " " + displayName;
    }
}

package enums;

public enum PostStatus {
    DRAFT("draft", "Draft"),
    PUBLISHED("published", "Published"),
    SCHEDULED("scheduled", "Scheduled"),
    HIDDEN("hidden", "Hidden");

    public final String value;
    public final String label;

    PostStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static PostStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (PostStatus status : PostStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }
}

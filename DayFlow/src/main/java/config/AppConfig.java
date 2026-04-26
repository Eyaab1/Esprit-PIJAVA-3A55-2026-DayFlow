package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads application configuration from application.properties and environment variables.
 */
public final class AppConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load application.properties", e);
        }
    }

    private AppConfig() {
    }

    public static String get(String key, String defaultValue) {
        String envKeyCompact = key.toUpperCase().replace('.', '_');
        String envKeySnake = toSnakeEnvKey(key);
        String envValue = firstNonBlankEnv(envKeySnake, envKeyCompact);
        if (envValue != null && !envValue.isBlank()) {
            return sanitize(envValue);
        }
        String propValue = PROPS.getProperty(key);
        if (propValue != null && !propValue.isBlank()) {
            return sanitize(propValue);
        }
        return defaultValue;
    }

    /**
     * For sensitive local desktop configs (like OAuth), prefer file value first,
     * then fallback to environment variable.
     */
    public static String getPreferProperties(String key, String defaultValue) {
        String propValue = PROPS.getProperty(key);
        if (propValue != null && !propValue.isBlank()) {
            return sanitize(propValue);
        }
        String envKeyCompact = key.toUpperCase().replace('.', '_');
        String envKeySnake = toSnakeEnvKey(key);
        String envValue = firstNonBlankEnv(envKeySnake, envKeyCompact);
        if (envValue != null && !envValue.isBlank()) {
            return sanitize(envValue);
        }
        return defaultValue;
    }

    public static String require(String key) {
        String value = get(key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config key: " + key);
        }
        return value;
    }

    public static String dbUrl() {
        return require("app.db.url");
    }

    public static String dbUser() {
        return require("app.db.user");
    }

    public static String dbPassword() {
        return Objects.requireNonNullElse(get("app.db.password", ""), "");
    }

    private static String firstNonBlankEnv(String... keys) {
        for (String k : keys) {
            String v = System.getenv(k);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String toSnakeEnvKey(String key) {
        String[] parts = key.split("\\.");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                out.append('_');
            }
            out.append(camelToUpperSnake(parts[i]));
        }
        return out.toString();
    }

    private static String camelToUpperSnake(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                out.append('_');
            }
            out.append(Character.toUpperCase(c));
        }
        return out.toString();
    }

    private static String sanitize(String value) {
        String v = value.trim();
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            return v.substring(1, v.length() - 1).trim();
        }
        return v;
    }
}

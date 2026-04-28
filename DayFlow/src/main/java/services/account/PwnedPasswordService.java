package services.account;

import config.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Locale;

/**
 * Checks password exposure against HIBP Pwned Passwords API using k-anonymity.
 */
public class PwnedPasswordService {

    public PwnedCheckResult checkPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return new PwnedCheckResult(false, 0, false);
        }

        boolean enabled = Boolean.parseBoolean(AppConfig.get("app.security.pwned.enabled", "true"));
        if (!enabled) {
            return new PwnedCheckResult(false, 0, false);
        }

        try {
            String sha1 = sha1Hex(rawPassword);
            String prefix = sha1.substring(0, 5);
            String suffix = sha1.substring(5);

            String template = AppConfig.get("app.security.pwned.urlTemplate", "https://api.pwnedpasswords.com/range/%s");
            String url = template.contains("%s") ? template.formatted(prefix) : (template + prefix);

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Add-Padding", "true")
                    .header("User-Agent", "DayFlow-Security/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new PwnedCheckResult(false, 0, false);
            }

            String[] lines = response.body().split("\\R");
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                int idx = line.indexOf(':');
                if (idx <= 0) {
                    continue;
                }
                String returnedSuffix = line.substring(0, idx).trim().toUpperCase(Locale.ROOT);
                if (!returnedSuffix.equals(suffix)) {
                    continue;
                }
                String countPart = line.substring(idx + 1).trim();
                int count = 0;
                try {
                    count = Integer.parseInt(countPart);
                } catch (NumberFormatException ignored) {
                }
                return new PwnedCheckResult(true, Math.max(0, count), true);
            }

            return new PwnedCheckResult(false, 0, true);
        } catch (Exception ignored) {
            // Fallback-safe: do not block signup/reset if external API is unavailable.
            return new PwnedCheckResult(false, 0, false);
        }
    }

    private static String sha1Hex(String value) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public record PwnedCheckResult(boolean compromised, int breachCount, boolean checkedWithApi) {
    }
}

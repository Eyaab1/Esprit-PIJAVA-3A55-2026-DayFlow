package services.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.AppConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

public class IpGeolocationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public GeoInfo resolve(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return GeoInfo.unknown(ipAddress);
        }
        String normalized = ipAddress.trim();
        if (isLocalAddress(normalized)) {
            return new GeoInfo(normalized, "Localhost", "", false);
        }

        boolean enabled = Boolean.parseBoolean(AppConfig.get("app.security.ipgeo.enabled", "true"));
        if (!enabled) {
            return GeoInfo.unknown(normalized);
        }

        String template = AppConfig.get(
                "app.security.ipgeo.urlTemplate",
                "http://ip-api.com/json/%s?fields=status,message,country,city,query"
        );
        String encodedIp = URLEncoder.encode(normalized, StandardCharsets.UTF_8);
        String url = template.contains("%s") ? template.formatted(encodedIp) : template + encodedIp;

        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                return GeoInfo.unknown(normalized);
            }

            JsonNode root = MAPPER.readTree(resp.body());
            String status = root.path("status").asText("");
            if (!status.equalsIgnoreCase("success")) {
                return GeoInfo.unknown(normalized);
            }

            String country = root.path("country").asText("");
            String city = root.path("city").asText("");
            String queryIp = root.path("query").asText(normalized);
            String label = buildLocationLabel(country, city);
            return new GeoInfo(queryIp, label, country, true);
        } catch (Exception ignored) {
            return GeoInfo.unknown(normalized);
        }
    }

    private static boolean isLocalAddress(String ip) {
        String s = ip.toLowerCase(Locale.ROOT);
        return s.equals("127.0.0.1")
                || s.equals("::1")
                || s.equals("localhost")
                || s.startsWith("192.168.")
                || s.startsWith("10.")
                || s.startsWith("172.16.")
                || s.startsWith("172.17.")
                || s.startsWith("172.18.")
                || s.startsWith("172.19.")
                || s.startsWith("172.2")
                || s.startsWith("172.30.")
                || s.startsWith("172.31.");
    }

    private static String buildLocationLabel(String country, String city) {
        String c = country == null ? "" : country.trim();
        String ci = city == null ? "" : city.trim();
        if (!ci.isBlank() && !c.isBlank()) {
            return ci + ", " + c;
        }
        if (!c.isBlank()) {
            return c;
        }
        if (!ci.isBlank()) {
            return ci;
        }
        return "Unknown location";
    }

    public record GeoInfo(String ipAddress, String locationLabel, String country, boolean fromExternalApi) {
        public static GeoInfo unknown(String ipAddress) {
            return new GeoInfo(ipAddress == null ? "Unknown IP" : ipAddress, "Unknown location", "", false);
        }
    }
}

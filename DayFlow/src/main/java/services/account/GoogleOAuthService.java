package services.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import config.AppConfig;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GoogleOAuthService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();

    public GoogleProfile authenticateWithLocalCallback() throws Exception {
        String clientId = AppConfig.getPreferProperties("app.oauth.google.clientId", "");
        String clientSecret = AppConfig.getPreferProperties("app.oauth.google.clientSecret", "");
        int port = Integer.parseInt(AppConfig.getPreferProperties("app.oauth.google.callbackPort", "8765"));
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("Missing Google OAuth clientId/clientSecret in configuration.");
        }
        String redirectUri = "http://localhost:" + port + "/oauth/google/callback";
        System.out.println("[OAUTH] Google client id prefix: " + maskedClientId(clientId));
        System.out.println("[OAUTH] Redirect URI: " + redirectUri);
        String state = randomState();
        String authUrl = buildAuthUrl(clientId, redirectUri, state);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> codeRef = new AtomicReference<>();
        AtomicReference<String> stateRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/oauth/google/callback", exchange -> handleCallback(exchange, codeRef, stateRef, errorRef, latch));
        server.start();
        try {
            openBrowser(authUrl);
            boolean ok = latch.await(120, TimeUnit.SECONDS);
            if (!ok) {
                throw new IllegalStateException("Google authentication timed out.");
            }
            if (errorRef.get() != null) {
                throw new IllegalStateException("Google OAuth error: " + errorRef.get());
            }
            if (!state.equals(stateRef.get())) {
                throw new IllegalStateException("OAuth state mismatch.");
            }
            String code = codeRef.get();
            if (code == null || code.isBlank()) {
                throw new IllegalStateException("No authorization code received.");
            }
            String accessToken = exchangeCodeForAccessToken(clientId, clientSecret, redirectUri, code);
            return fetchGoogleProfile(accessToken);
        } finally {
            server.stop(0);
        }
    }

    private static void handleCallback(HttpExchange exchange,
                                       AtomicReference<String> codeRef,
                                       AtomicReference<String> stateRef,
                                       AtomicReference<String> errorRef,
                                       CountDownLatch latch) throws IOException {
        try {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            codeRef.set(params.get("code"));
            stateRef.set(params.get("state"));
            errorRef.set(params.get("error"));
            byte[] body = "<html><body><h2>Google login complete.</h2><p>You can return to DayFlow.</p></body></html>"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        } finally {
            latch.countDown();
            exchange.close();
        }
    }

    private static String exchangeCodeForAccessToken(String clientId, String clientSecret, String redirectUri, String code)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        String payload = "code=" + encode(code)
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&redirect_uri=" + encode(redirectUri)
                + "&grant_type=authorization_code";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Token exchange failed: " + response.body());
        }
        JsonNode root = JSON.readTree(response.body());
        String token = root.path("access_token").asText(null);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No access_token in Google response.");
        }
        return token;
    }

    private static GoogleProfile fetchGoogleProfile(String accessToken) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/oauth2/v2/userinfo"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Profile fetch failed: " + response.body());
        }
        JsonNode root = JSON.readTree(response.body());
        String googleId = root.path("id").asText("");
        String email = root.path("email").asText("");
        String givenName = root.path("given_name").asText("");
        String familyName = root.path("family_name").asText("");
        if (email.isBlank()) {
            throw new IllegalStateException("Google account has no accessible email.");
        }
        return new GoogleProfile(googleId, email, givenName, familyName);
    }

    private static String buildAuthUrl(String clientId, String redirectUri, String state) {
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode("openid email profile")
                + "&state=" + encode(state)
                + "&access_type=offline"
                + "&prompt=consent";
    }

    private static String randomState() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQuery(String query) {
        if (query == null || query.isBlank()) {
            return Map.of();
        }
        return java.util.Arrays.stream(query.split("&"))
                .map(part -> part.split("=", 2))
                .filter(arr -> arr.length == 2)
                .collect(java.util.stream.Collectors.toMap(
                        arr -> decode(arr[0]),
                        arr -> decode(arr[1]),
                        (a, b) -> b
                ));
    }

    private static String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void openBrowser(String url) throws IOException {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI.create(url));
            return;
        }
        throw new IllegalStateException("Desktop browser is not supported on this machine.");
    }

    public record GoogleProfile(String googleUserId, String email, String givenName, String familyName) {
    }

    private static String maskedClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return "<empty>";
        }
        if (clientId.length() <= 16) {
            return clientId;
        }
        return clientId.substring(0, 12) + "..." + clientId.substring(clientId.length() - 10);
    }
}

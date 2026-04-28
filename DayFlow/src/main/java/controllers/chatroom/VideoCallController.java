package controllers.chatroom;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

/**
 * Controller de la fenêtre de visioconférence.
 * Utilise Jitsi Meet (gratuit, open source, sans clé API).
 * URL : https://meet.jit.si/<roomName>
 */
public class VideoCallController {

    @FXML private WebView  webView;
    @FXML private Label    callTitleLabel;
    @FXML private Label    callStatusLabel;
    @FXML private Label    callDurationLabel;
    @FXML private Button   micBtn;
    @FXML private Button   camBtn;

    private String  roomName;
    private String  userName;
    private boolean audioOnly = false;
    private boolean micMuted  = false;
    private boolean camOff    = false;

    private Timeline durationTimer;
    private int      seconds = 0;

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        // Activer JavaScript + médias dans WebView
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().setUserAgent(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36");
    }

    /**
     * Démarre l'appel.
     * @param roomName  nom du salon (ex: "DayFlow-goal-3")
     * @param userName  nom de l'utilisateur
     * @param audioOnly true = appel audio uniquement
     */
    public void startCall(String roomName, String userName, boolean audioOnly) {
        this.roomName  = sanitizeRoomName(roomName);
        this.userName  = userName;
        this.audioOnly = audioOnly;

        callTitleLabel.setText(audioOnly ? "🎤 Appel Audio" : "🎥 Visioconférence");
        callStatusLabel.setText("Connexion en cours...");

        if (audioOnly) {
            camBtn.setDisable(true);
            camBtn.setOpacity(0.4);
        }

        // Charger Jitsi Meet
        String url = buildJitsiUrl();
        webView.getEngine().load(url);

        // Mettre à jour le statut quand la page est chargée
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                Platform.runLater(() -> {
                    callStatusLabel.setText("✅ Connecté");
                    callStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#48bb78;");
                    startDurationTimer();
                });
            } else if (state == javafx.concurrent.Worker.State.FAILED) {
                Platform.runLater(() -> {
                    callStatusLabel.setText("❌ Connexion échouée");
                    callStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ef4444;");
                });
            }
        });
    }

    // ── Contrôles ─────────────────────────────────────────────────────────

    @FXML
    private void onToggleMic() {
        micMuted = !micMuted;
        micBtn.setText(micMuted ? "🔇" : "🎤");
        micBtn.setStyle(micBtn.getStyle().replace(
            micMuted ? "#2d3748" : "#ef4444",
            micMuted ? "#ef4444" : "#2d3748"));
        // Injecter JS pour couper/activer le micro dans Jitsi
        webView.getEngine().executeScript(
            "try { APP.conference.toggleAudioMuted(); } catch(e) {}");
    }

    @FXML
    private void onToggleCamera() {
        if (audioOnly) return;
        camOff = !camOff;
        camBtn.setText(camOff ? "📵" : "📷");
        webView.getEngine().executeScript(
            "try { APP.conference.toggleVideoMuted(); } catch(e) {}");
    }

    @FXML
    private void onShareScreen() {
        webView.getEngine().executeScript(
            "try { APP.conference.toggleScreenSharing(); } catch(e) {}");
    }

    @FXML
    private void onHangUp() {
        if (durationTimer != null) durationTimer.stop();
        webView.getEngine().load("about:blank");
        // Fermer la fenêtre
        webView.getScene().getWindow().hide();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String buildJitsiUrl() {
        // Jitsi Meet public — gratuit, sans inscription
        String base = "https://meet.jit.si/" + roomName;
        String config = "#config.startWithAudioMuted=false" +
                        "&config.startWithVideoMuted=" + audioOnly +
                        "&config.prejoinPageEnabled=false" +
                        "&config.disableDeepLinking=true" +
                        "&userInfo.displayName=" +
                        java.net.URLEncoder.encode(userName, java.nio.charset.StandardCharsets.UTF_8);
        return base + config;
    }

    private String sanitizeRoomName(String name) {
        // Jitsi n'accepte que lettres, chiffres et tirets
        return name.replaceAll("[^a-zA-Z0-9-]", "-")
                   .replaceAll("-+", "-")
                   .toLowerCase();
    }

    private void startDurationTimer() {
        durationTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds++;
            int m = seconds / 60, s = seconds % 60;
            callDurationLabel.setText(String.format("%02d:%02d", m, s));
        }));
        durationTimer.setCycleCount(Timeline.INDEFINITE);
        durationTimer.play();
    }
}

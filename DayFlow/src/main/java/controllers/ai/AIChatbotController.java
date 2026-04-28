package controllers.ai;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import services.ai.MotivationAIService;
import session.AppSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIChatbotController {

    @FXML private VBox messagesBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private HBox suggestionsBar;

    private final MotivationAIService aiService = new MotivationAIService();
    private final List<Map<String, String>> history = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "moti-ai");
        t.setDaemon(true);
        return t;
    });

    private static final String[] SUGGESTIONS = {
        "💪 Motive-moi !",
        "🎯 Je procrastine...",
        "🧠 Améliorer ma concentration",
        "😓 Je suis épuisé"
    };

    @FXML
    private void initialize() {
        // Welcome message
        String firstName = AppSession.getCurrentUser()
                .map(u -> u.getFirstName() != null ? u.getFirstName() : "")
                .orElse("");
        String welcome = "Salut " + (firstName.isBlank() ? "toi" : firstName) + " ! 👋 Je suis Moti, ton assistant motivation.\n"
                + "Je suis là pour t'aider à rester focus, motivé et à vaincre la procrastination. Comment puis-je t'aider aujourd'hui ?";
        addBotMessage(welcome);

        // Suggestion chips
        for (String s : SUGGESTIONS) {
            Button chip = new Button(s);
            chip.getStyleClass().add("chat-suggestion-chip");
            chip.setOnAction(e -> sendMessage(s));
            suggestionsBar.getChildren().add(chip);
        }

        inputField.setOnAction(e -> onSend());
        scrollToBottom();
    }

    @FXML
    private void onSend() {
        String text = inputField.getText().trim();
        if (text.isBlank()) return;
        sendMessage(text);
    }

    private void sendMessage(String text) {
        inputField.clear();
        sendButton.setDisable(true);
        inputField.setDisable(true);
        suggestionsBar.setVisible(false);
        suggestionsBar.setManaged(false);

        addUserMessage(text);
        addTypingIndicator();

        List<Map<String, String>> historyCopy = new ArrayList<>(history);

        executor.submit(() -> {
            try {
                String response = aiService.chat(historyCopy, text);
                history.add(Map.of("role", "user", "content", text));
                history.add(Map.of("role", "assistant", "content", response));
                // Keep history bounded to last 10 exchanges
                while (history.size() > 20) history.remove(0);

                Platform.runLater(() -> {
                    removeTypingIndicator();
                    addBotMessage(response);
                    sendButton.setDisable(false);
                    inputField.setDisable(false);
                    inputField.requestFocus();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    removeTypingIndicator();
                    addBotMessage("Oups, je n'arrive pas à te répondre pour l'instant. 😅 Vérifie ta connexion et réessaie !");
                    sendButton.setDisable(false);
                    inputField.setDisable(false);
                });
            }
        });
    }

    private void addUserMessage(String text) {
        Label bubble = new Label(text);
        bubble.getStyleClass().add("chat-bubble-user");
        bubble.setWrapText(true);
        bubble.setMaxWidth(280);

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(2, 4, 2, 40));
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        Label avatar = new Label("🤖");
        avatar.getStyleClass().add("chat-avatar");

        Label bubble = new Label(text);
        bubble.getStyleClass().add("chat-bubble-bot");
        bubble.setWrapText(true);
        bubble.setMaxWidth(260);

        HBox row = new HBox(8, avatar, bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 40, 2, 4));
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addTypingIndicator() {
        Label dots = new Label("● ● ●");
        dots.getStyleClass().add("chat-typing");
        dots.setId("typing-indicator");

        Label avatar = new Label("🤖");
        avatar.getStyleClass().add("chat-avatar");

        HBox row = new HBox(8, avatar, dots);
        row.setId("typing-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 40, 2, 4));
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void removeTypingIndicator() {
        messagesBox.getChildren().removeIf(n -> "typing-row".equals(n.getId()));
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}

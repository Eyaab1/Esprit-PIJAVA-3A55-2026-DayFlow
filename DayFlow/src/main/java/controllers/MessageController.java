package controllers;

<<<<<<< HEAD
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.Message;
import services.ChatroomService;
import services.MessageService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MessageController {

    // ── FXML ──────────────────────────────────────────────────────────────
    @FXML private VBox        messagesBox;
    @FXML private ScrollPane  scrollPane;
    @FXML private TextField   messageField;
    @FXML private ComboBox<String> chatroomSelector;
    @FXML private Label       chatroomTitle;
    @FXML private Label       chatroomSub;

    // ── State ─────────────────────────────────────────────────────────────
    private MessageService  messageService;
    private ChatroomService chatroomService;

    // ID utilisateur simulé (à remplacer par la session réelle)
    private static final int CURRENT_USER_ID = 1;

    private int currentChatroomId = -1;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    // ── Init ──────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        messageService  = new MessageService();
        chatroomService = new ChatroomService();
        loadChatroomList();
    }

    /** Charge la liste des chatrooms dans le ComboBox du header */
    private void loadChatroomList() {
        try {
            chatroomService.getAll().forEach(c ->
                chatroomSelector.getItems().add("Chatroom #" + c.getId())
            );
        } catch (SQLException e) {
            showSystemMessage("Impossible de charger les chatrooms : " + e.getMessage());
        }
    }

    /** Appelé quand l'utilisateur choisit un chatroom */
    @FXML
    public void onChatroomSelected() {
        String selected = chatroomSelector.getValue();
        if (selected == null) return;

        // "Chatroom #3" → 3
        currentChatroomId = Integer.parseInt(selected.replaceAll("\\D+", ""));
        chatroomTitle.setText(selected);
        chatroomSub.setText("chatroom actif");
        loadMessages();
    }

    /** Charge et affiche tous les messages du chatroom sélectionné */
    public void loadMessages() {
        messagesBox.getChildren().clear();
        if (currentChatroomId < 0) return;
        try {
            List<Message> all = messageService.getAll();
            all.stream()
               .filter(m -> m.getChatroomId() == currentChatroomId)
               .forEach(m -> addBubble(m, false));
            scrollToBottom();
        } catch (SQLException e) {
            showSystemMessage("Erreur chargement : " + e.getMessage());
        }
    }

    /** Envoie un message (bouton ➤ ou touche Entrée) */
    @FXML
    public void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        if (currentChatroomId < 0) {
            showSystemMessage("⚠️ Sélectionne un chatroom d'abord.");
            return;
        }
        try {
            Message m = new Message(text, currentChatroomId, CURRENT_USER_ID);
            messageService.create(m);
            messageField.clear();
            // affiche la bulle immédiatement (optimistic UI)
            addBubble(m, true);
            scrollToBottom();
        } catch (IllegalArgumentException e) {
            showSystemMessage("Erreur : " + e.getMessage());
        } catch (SQLException e) {
            showSystemMessage("Erreur BD : " + e.getMessage());
        }
    }

    // ── Bubble builder ────────────────────────────────────────────────────

    /**
     * Crée et ajoute une bulle de message dans messagesBox.
     * @param m        le message
     * @param animate  true = bulle envoyée (droite), false = reçue (gauche)
     *                 La distinction sent/received est basée sur authorId.
     */
    private void addBubble(Message m, boolean animate) {
        boolean isMine = (m.getAuthorId() == CURRENT_USER_ID);

        // ── Texte du message ──
        Label textLabel = new Label(m.getContent());
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(380);
        textLabel.getStyleClass().add(isMine ? "bubble-sent-text" : "bubble-received-text");

        // ── Heure ──
        String timeStr = m.getCreatedAt() != null
                ? m.getCreatedAt().format(TIME_FMT) : "";
        Label timeLabel = new Label(timeStr);
        timeLabel.getStyleClass().add("bubble-time");

        // ── Bulle (VBox) ──
        VBox bubble = new VBox(2, textLabel, timeLabel);
        bubble.getStyleClass().add(isMine ? "bubble-sent" : "bubble-received");
        bubble.setMaxWidth(400);

        // ── Avatar ──
        String initials = "U" + m.getAuthorId();
        Label avatarLabel = new Label(initials.length() > 2 ? initials.substring(0, 2) : initials);
        avatarLabel.getStyleClass().add("avatar-text");
        StackPane avatar = new StackPane(avatarLabel);
        avatar.getStyleClass().add("avatar");

        // ── Row ──
        HBox row = new HBox(10);
        row.setMaxWidth(Double.MAX_VALUE);

        if (isMine) {
            HBox.setHgrow(bubble, Priority.NEVER);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(spacer, bubble, avatar);
        } else {
            // Auteur au-dessus pour les messages reçus
            Label authorLabel = new Label("User " + m.getAuthorId());
            authorLabel.getStyleClass().add("bubble-author");
            VBox withAuthor = new VBox(2, authorLabel, bubble);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(avatar, withAuthor);
        }

        VBox.setMargin(row, new Insets(2, 0, 2, 0));
        messagesBox.getChildren().add(row);
    }

    /** Message système (erreur, info) centré en gris */
    private void showSystemMessage(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 12px; -fx-font-style: italic;");
        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER);
        messagesBox.getChildren().add(row);
    }

    /** Scroll automatique vers le bas */
    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
=======
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Message;
import services.MessageService;

import java.sql.SQLException;

public class MessageController {

    @FXML private TextField contentField;
    @FXML private TextField chatroomIdField;
    @FXML private TextField authorIdField;
    @FXML private CheckBox  pinnedCheck;
    @FXML private TextField updateIdField;
    @FXML private Label     statusLabel;

    @FXML private TableView<Message> tableView;
    @FXML private TableColumn<Message, Integer> colId;
    @FXML private TableColumn<Message, String>  colContent;
    @FXML private TableColumn<Message, Integer> colChatroomId;
    @FXML private TableColumn<Message, Integer> colAuthorId;
    @FXML private TableColumn<Message, Boolean> colPinned;
    @FXML private TableColumn<Message, String>  colCreatedAt;

    private MessageService service;

    @FXML
    public void initialize() {
        service = new MessageService();

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colContent.setCellValueFactory(new PropertyValueFactory<>("content"));
        colChatroomId.setCellValueFactory(new PropertyValueFactory<>("chatroomId"));
        colAuthorId.setCellValueFactory(new PropertyValueFactory<>("authorId"));
        colPinned.setCellValueFactory(new PropertyValueFactory<>("pinned"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // clic sur ligne → remplit les champs
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                updateIdField.setText(String.valueOf(selected.getId()));
                contentField.setText(selected.getContent());
                chatroomIdField.setText(String.valueOf(selected.getChatroomId()));
                authorIdField.setText(String.valueOf(selected.getAuthorId()));
                pinnedCheck.setSelected(selected.isPinned());
            }
        });

        loadAll();
    }

    @FXML
    public void addMessage() {
        try {
            String content = contentField.getText();
            int chatroomId = Integer.parseInt(chatroomIdField.getText().trim());
            int authorId   = Integer.parseInt(authorIdField.getText().trim());
            Message m = new Message(content, chatroomId, authorId);
            service.create(m);
            showStatus("Message envoyé ✅", true);
            clearFields();
            loadAll();
        } catch (IllegalArgumentException e) {
            showStatus("Erreur : " + e.getMessage(), false);
        } catch (SQLException e) {
            showStatus("Erreur BD : " + e.getMessage(), false);
        }
    }

    @FXML
    public void updateMessage() {
        try {
            int id = Integer.parseInt(updateIdField.getText().trim());
            Message m = new Message();
            m.setId(id);
            m.setContent(contentField.getText());
            m.setPinned(pinnedCheck.isSelected());
            m.setEdited(true);
            service.update(m);
            showStatus("Message modifié ✅", true);
            clearFields();
            loadAll();
        } catch (IllegalArgumentException e) {
            showStatus("Erreur : " + e.getMessage(), false);
        } catch (SQLException e) {
            showStatus("Erreur BD : " + e.getMessage(), false);
        }
    }

    @FXML
    public void deleteMessage() {
        try {
            int id = Integer.parseInt(updateIdField.getText().trim());
            service.delete(id);
            showStatus("Message supprimé ✅", true);
            clearFields();
            loadAll();
        } catch (NumberFormatException e) {
            showStatus("ID invalide.", false);
        } catch (SQLException e) {
            showStatus("Erreur BD : " + e.getMessage(), false);
        }
    }

    @FXML
    public void loadAll() {
        try {
            tableView.setItems(FXCollections.observableArrayList(service.getAll()));
        } catch (SQLException e) {
            showStatus("Erreur chargement : " + e.getMessage(), false);
        }
    }

    private void showStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().setAll(success ? "status-ok" : "status-err");
    }

    private void clearFields() {
        contentField.clear();
        chatroomIdField.clear();
        authorIdField.clear();
        updateIdField.clear();
        pinnedCheck.setSelected(false);
>>>>>>> origin/chatroom
    }
}

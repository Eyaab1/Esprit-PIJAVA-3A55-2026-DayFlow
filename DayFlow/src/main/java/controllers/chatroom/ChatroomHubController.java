package controllers.chatroom;

import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
<<<<<<< HEAD
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
=======
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
>>>>>>> origin/chatroom
import javafx.util.Duration;
import model.chatroom.Chatroom;
import model.chatroom.Message;
import model.goals_activity_management.GoalParticipation;
import model.user.User;
import services.UserServices.UserService;
import services.chatroom_module.ChatroomService;
import services.chatroom_module.ChatroomService.ChatroomListItem;
import services.chatroom_module.GoalChatroomLifecycleService;
import services.chatroom_module.GoalParticipationService;
import services.chatroom_module.MessageService;
import session.AppSession;
import session.ChatroomNav;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
<<<<<<< HEAD
import java.util.List;
import java.util.Locale;
=======
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
>>>>>>> origin/chatroom
import java.util.Optional;

public class ChatroomHubController {

<<<<<<< HEAD
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);

    private final ChatroomService chatroomService = new ChatroomService();
    private final MessageService messageService = new MessageService();
    private final GoalParticipationService participationService = new GoalParticipationService();
    private final GoalChatroomLifecycleService lifecycle = new GoalChatroomLifecycleService();
    private final UserService userService = new UserService();

    @FXML
    private ListView<ChatroomListItem> chatListView;
    @FXML
    private Label headerTitleLabel;
    @FXML
    private Label headerSubLabel;
    @FXML
    private ScrollPane messagesScroll;
    @FXML
    private VBox messagesBox;
    @FXML
    private TextField messageField;
    @FXML
    private Button pendingBtn;
    @FXML
    private Button lockBtn;
    @FXML
    private Button archiveBtn;
    @FXML
    private Button deleteBtn;

    private ChatroomListItem current;
    private Timeline messagePollTimeline;

    @FXML
    private void initialize() {
        chatListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ChatroomListItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.goalTitle());
            }
        });
=======
    // ── Formatters ────────────────────────────────────────────────────────
    private static final DateTimeFormatter TIME_SHORT = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

    // ── Services ──────────────────────────────────────────────────────────
    private final ChatroomService            chatroomService    = new ChatroomService();
    private final MessageService             messageService     = new MessageService();
    private final GoalParticipationService   participationService = new GoalParticipationService();
    private final GoalChatroomLifecycleService lifecycle        = new GoalChatroomLifecycleService();
    private final UserService                userService        = new UserService();

    // ── FXML ──────────────────────────────────────────────────────────────
    @FXML private ListView<ChatroomListItem> chatListView;
    @FXML private Label      headerTitleLabel;
    @FXML private Label      headerSubLabel;
    @FXML private Label      headerAvatarLabel;
    @FXML private ScrollPane messagesScroll;
    @FXML private VBox       messagesBox;
    @FXML private TextField  messageField;
    @FXML private TextField  searchField;
    @FXML private Button     pendingBtn;
    @FXML private Button     lockBtn;
    @FXML private Button     archiveBtn;
    @FXML private Button     deleteBtn;
    @FXML private ListView<GoalParticipation> membersListView;
    @FXML private Label      memberCountLabel;
    @FXML private Label      photoCount;
    @FXML private Label      videoCount;

    // ── State ─────────────────────────────────────────────────────────────
    private ChatroomListItem                  current;
    private Timeline                          messagePollTimeline;
    private ObservableList<ChatroomListItem>  allRooms;
    private FilteredList<ChatroomListItem>    roomFilter;
    /** Cache nom utilisateur pour éviter N requêtes BD par refresh */
    private final Map<Integer, String>        userNameCache = new HashMap<>();

    // ══════════════════════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void initialize() {
        setupChatListCells();
        setupMembersListCells();
>>>>>>> origin/chatroom

        Integer uid = AppSession.getCurrentUser().map(User::getId).orElse(null);
        if (uid == null) {
            headerTitleLabel.setText("Non connecté");
<<<<<<< HEAD
            return;
        }

        try {
            List<ChatroomListItem> items = chatroomService.findAccessibleForUser(uid);
            chatListView.setItems(FXCollections.observableArrayList(items));
        } catch (SQLException e) {
            headerTitleLabel.setText("Erreur chargement");
            return;
        }

        chatListView.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                selectRoom(b);
            }
        });

        Integer openGid = ChatroomNav.pullOpenGoalId();
        if (openGid != null) {
            for (ChatroomListItem it : chatListView.getItems()) {
                if (it.goalId() == openGid) {
                    chatListView.getSelectionModel().select(it);
                    break;
                }
            }
        }
        if (current == null && !chatListView.getItems().isEmpty()) {
            chatListView.getSelectionModel().selectFirst();
        }

        messageField.setOnAction(e -> onSendMessage());

        messagePollTimeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
            if (current != null) {
                Platform.runLater(this::refreshMessages);
            }
=======
            messageField.setDisable(true);
            return;
        }

        // Charger les chatrooms accessibles
        try {
            allRooms = FXCollections.observableArrayList(chatroomService.findAccessibleForUser(uid));
            roomFilter = new FilteredList<>(allRooms, p -> true);
            chatListView.setItems(roomFilter);
            if (allRooms.isEmpty()) {
                Label ph = new Label("Aucun salon pour l’instant. Créez un objectif ou rejoignez-en un depuis les objectifs.");
                ph.setWrapText(true);
                ph.getStyleClass().add("empty-state");
                chatListView.setPlaceholder(ph);
                messageField.setDisable(true);
            }
        } catch (SQLException e) {
            headerTitleLabel.setText("Erreur chargement");
            messageField.setDisable(true);
            return;
        }

        // Sélection d'un chatroom
        chatListView.getSelectionModel().selectedItemProperty()
                .addListener((o, a, b) -> { if (b != null) selectRoom(b); });

        // Recherche dynamique dans la sidebar
        searchField.textProperty().addListener((obs, old, query) -> filterRooms(query));

        // Navigation directe depuis un autre écran
        Integer openGid = ChatroomNav.pullOpenGoalId();
        if (openGid != null) {
            allRooms.stream()
                    .filter(it -> it.goalId() == openGid)
                    .findFirst()
                    .ifPresent(it -> chatListView.getSelectionModel().select(it));
        }
        if (current == null && !allRooms.isEmpty()) {
            chatListView.getSelectionModel().selectFirst();
        }

        // Enter pour envoyer
        messageField.setOnAction(e -> onSendMessage());

        // Polling messages toutes les 4 secondes
        messagePollTimeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
            if (current != null) Platform.runLater(this::refreshMessages);
>>>>>>> origin/chatroom
        }));
        messagePollTimeline.setCycleCount(Timeline.INDEFINITE);
        messagePollTimeline.play();
    }

<<<<<<< HEAD
    private void selectRoom(ChatroomListItem item) {
        current = item;
        headerTitleLabel.setText(item.goalTitle());
        try {
            int approved = participationService.countApprovedByGoal(item.goalId());
            headerSubLabel.setText(approved + " membre(s)");
            int uid = AppSession.getCurrentUser().map(User::getId).orElse(0);
            boolean admin = participationService.isOwnerOrAdmin(uid, item.goalId());
            pendingBtn.setVisible(admin);
            pendingBtn.setManaged(admin);
            lockBtn.setVisible(admin);
            lockBtn.setManaged(admin);
            archiveBtn.setVisible(admin);
            archiveBtn.setManaged(admin);
            deleteBtn.setVisible(admin);
            deleteBtn.setManaged(admin);

=======
    // ══════════════════════════════════════════════════════════════════════
    // SIDEBAR — Custom cells
    // ══════════════════════════════════════════════════════════════════════

    private void setupChatListCells() {
        chatListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ChatroomListItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                // Avatar
                String initials = item.goalTitle().length() >= 2
                        ? item.goalTitle().substring(0, 2).toUpperCase()
                        : item.goalTitle().toUpperCase();
                Label avatarLbl = new Label(initials);
                avatarLbl.getStyleClass().add("avatar-text");
                StackPane avatar = new StackPane(avatarLbl);
                avatar.getStyleClass().add("avatar");
                avatar.setMinSize(40, 40);
                avatar.setMaxSize(40, 40);

                // Nom + preview
                Label nameLbl = new Label(item.goalTitle());
                nameLbl.getStyleClass().add("sidebar-item-name");

                // Dernier message (préchargé en SQL)
                String preview = formatSnippet(item.lastMessageSnippet());
                Label previewLbl = new Label(preview);
                previewLbl.getStyleClass().add("sidebar-item-preview");
                previewLbl.setMaxWidth(150);

                VBox textBox = new VBox(3, nameLbl, previewLbl);
                textBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                HBox cell = new HBox(10, avatar, textBox);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setPadding(new Insets(6, 4, 6, 4));

                setGraphic(cell);
                setText(null);
            }
        });
    }

    private static String formatSnippet(String content) {
        if (content == null || content.isBlank()) {
            return "Pas encore de messages";
        }
        String t = content.trim();
        return t.length() > 35 ? t.substring(0, 35) + "…" : t;
    }

    /** Met à jour l’extrait du dernier message pour la ligne du salon (liste de gauche). */
    private void replaceRoomSnippet(int chatroomId, String fullContent) {
        for (int i = 0; i < allRooms.size(); i++) {
            ChatroomListItem it = allRooms.get(i);
            if (it.chatroomId() != chatroomId) {
                continue;
            }
            ChatroomListItem updated = new ChatroomListItem(
                    it.chatroomId(), it.goalId(), it.goalTitle(), fullContent);
            allRooms.set(i, updated);
            if (current != null && current.chatroomId() == chatroomId) {
                current = updated;
            }
            int sel = chatListView.getSelectionModel().getSelectedIndex();
            if (sel >= 0) {
                chatListView.getSelectionModel().clearAndSelect(sel);
            }
            break;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // MEMBERS PANEL — Custom cells
    // ══════════════════════════════════════════════════════════════════════

    private void setupMembersListCells() {
        membersListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GoalParticipation gp, boolean empty) {
                super.updateItem(gp, empty);
                if (empty || gp == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                String name = resolveUserName(gp.getUserId());

                // Avatar
                String initials = name.length() >= 2
                        ? name.substring(0, 2).toUpperCase()
                        : name.toUpperCase();
                Label avatarLbl = new Label(initials);
                avatarLbl.getStyleClass().add("avatar-text");
                avatarLbl.setStyle("-fx-font-size: 10px;");
                StackPane avatar = new StackPane(avatarLbl);
                avatar.getStyleClass().add("avatar");
                avatar.setMinSize(32, 32);
                avatar.setMaxSize(32, 32);

                // Nom + rôle
                Label nameLbl = new Label(name);
                nameLbl.getStyleClass().add("member-name");

                boolean isAdmin = GoalParticipation.ROLE_OWNER.equals(gp.getRole())
                        || GoalParticipation.ROLE_ADMIN.equals(gp.getRole());
                Label roleLbl = new Label(gp.getRole().toLowerCase());
                roleLbl.getStyleClass().add(isAdmin ? "member-role-admin" : "member-role");

                VBox textBox = new VBox(2, nameLbl, roleLbl);
                HBox cell = new HBox(10, avatar, textBox);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setPadding(new Insets(4, 0, 4, 0));

                setGraphic(cell);
                setText(null);
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // SEARCH
    // ══════════════════════════════════════════════════════════════════════

    private void filterRooms(String query) {
        if (roomFilter == null) {
            return;
        }
        if (query == null || query.isBlank()) {
            roomFilter.setPredicate(p -> true);
        } else {
            String lower = query.toLowerCase();
            roomFilter.setPredicate(it -> it.goalTitle().toLowerCase().contains(lower));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SELECT ROOM
    // ══════════════════════════════════════════════════════════════════════

    private void selectRoom(ChatroomListItem item) {
        current = item;

        // Header
        headerTitleLabel.setText(item.goalTitle());
        String initials = item.goalTitle().length() >= 2
                ? item.goalTitle().substring(0, 2).toUpperCase()
                : item.goalTitle().toUpperCase();
        headerAvatarLabel.setText(initials);

        try {
            // Membres
            List<GoalParticipation> members = participationService.findApprovedByGoal(item.goalId());
            headerSubLabel.setText(members.size() + " membre(s)");
            memberCountLabel.setText(members.size() + " membres");
            membersListView.setItems(FXCollections.observableArrayList(members));

            // Droits admin
            int uid = AppSession.getCurrentUser().map(User::getId).orElse(0);
            boolean admin = participationService.isOwnerOrAdmin(uid, item.goalId());
            setAdminControls(admin);

            // État du salon
>>>>>>> origin/chatroom
            boolean open = chatroomService.findById(item.chatroomId())
                    .map(c -> "active".equalsIgnoreCase(c.getState()))
                    .orElse(false);
            messageField.setDisable(!open);
<<<<<<< HEAD
            messageField.setPromptText(open ? "Écrire un message…" : "Salon verrouillé ou archivé — envoi désactivé.");
=======
            messageField.setPromptText(open ? "Votre message..." : "Salon verrouillé — envoi désactivé.");

>>>>>>> origin/chatroom
        } catch (SQLException e) {
            headerSubLabel.setText("");
            messageField.setDisable(true);
        }
<<<<<<< HEAD
        refreshMessages();
    }

    private void refreshMessages() {
        messagesBox.getChildren().clear();
        if (current == null) {
            messagesBox.getChildren().add(new Label("Sélectionnez un chat."));
=======

        refreshMessages();
    }

    private void setAdminControls(boolean visible) {
        pendingBtn.setVisible(visible); pendingBtn.setManaged(visible);
        lockBtn.setVisible(visible);    lockBtn.setManaged(visible);
        archiveBtn.setVisible(visible); archiveBtn.setManaged(visible);
        deleteBtn.setVisible(visible);  deleteBtn.setManaged(visible);
    }

    // ══════════════════════════════════════════════════════════════════════
    // MESSAGES
    // ══════════════════════════════════════════════════════════════════════

    private void refreshMessages() {
        messagesBox.getChildren().clear();
        if (current == null) {
            photoCount.setText("— photos");
            videoCount.setText("— vidéos");
            messagesBox.getChildren().add(emptyState("Sélectionnez un chat."));
>>>>>>> origin/chatroom
            return;
        }
        try {
            List<Message> msgs = messageService.findByChatroomId(current.chatroomId());
<<<<<<< HEAD
            if (msgs.isEmpty()) {
                Label empty = new Label("Pas encore de messages. Lancez la conversation !");
                empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
                messagesBox.getChildren().add(empty);
                return;
            }
            for (Message m : msgs) {
                String author = userService.findById(m.getAuthorId())
                        .map(u -> formatUser(u))
                        .orElse("Utilisateur #" + m.getAuthorId());
                String line = "[" + m.getCreatedAt().format(TF) + "] " + author + " : " + m.getContent();
                Label l = new Label(line);
                l.setWrapText(true);
                l.getStyleClass().add("chat-msg-line");
                messagesBox.getChildren().add(l);
            }
        } catch (SQLException e) {
            messagesBox.getChildren().add(new Label("Erreur : " + e.getMessage()));
=======
            updateFileStatsLabels(msgs);
            if (msgs.isEmpty()) {
                messagesBox.getChildren().add(emptyState("Pas encore de messages. Lancez la conversation !"));
                return;
            }
            int currentUid = AppSession.getCurrentUser().map(User::getId).orElse(-1);
            for (Message m : msgs) {
                boolean isMine = m.getAuthorId() == currentUid;
                String authorName = resolveUserName(m.getAuthorId());
                messagesBox.getChildren().add(buildBubble(m, isMine, authorName));
            }
        } catch (SQLException e) {
            messagesBox.getChildren().add(emptyState("Erreur : " + e.getMessage()));
>>>>>>> origin/chatroom
        }
        scrollMessagesToBottom();
    }

<<<<<<< HEAD
    private void scrollMessagesToBottom() {
        if (messagesScroll == null) {
            return;
        }
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    private static String formatUser(User u) {
        String a = u.getFirstName() != null ? u.getFirstName() : "";
        String b = u.getLastName() != null ? u.getLastName() : "";
        String s = (a + " " + b).trim();
        return s.isEmpty() ? ("#" + u.getId()) : s;
    }

    @FXML
    private void onSendMessage() {
        if (current == null) {
            return;
        }
        Optional<Integer> uid = AppSession.getCurrentUser().map(User::getId);
        if (uid.isEmpty()) {
            return;
        }
        String text = messageField.getText() != null ? messageField.getText().trim() : "";
        if (text.isEmpty()) {
            return;
        }
        try {
            messageService.postMessage(uid.get(), current.chatroomId(), text);
            messageField.clear();
            refreshMessages();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        } catch (IllegalArgumentException e) {
=======
    /** Compte simple basé sur les liens dans le texte (pas de pièces jointes dédiées en base). */
    private void updateFileStatsLabels(List<Message> msgs) {
        int photos = 0;
        int videos = 0;
        for (Message m : msgs) {
            String c = m.getContent() != null ? m.getContent().toLowerCase(Locale.ROOT) : "";
            if (contentLooksLikeImageLink(c)) {
                photos++;
            } else if (contentLooksLikeVideoLink(c)) {
                videos++;
            }
        }
        photoCount.setText(photos + " photo" + (photos != 1 ? "s" : ""));
        videoCount.setText(videos + " vidéo" + (videos != 1 ? "s" : ""));
    }

    private static boolean contentLooksLikeImageLink(String c) {
        return c.contains(".jpg") || c.contains(".jpeg") || c.contains(".png")
                || c.contains(".gif") || c.contains(".webp");
    }

    private static boolean contentLooksLikeVideoLink(String c) {
        return c.contains(".mp4") || c.contains(".webm") || c.contains(".mov");
    }

    /** Construit une bulle de message style Discord */
    private HBox buildBubble(Message m, boolean isMine, String authorName) {

        // Contenu
        Label content = new Label(m.getContent());
        content.setWrapText(true);
        content.setMaxWidth(380);
        content.getStyleClass().add(isMine ? "bubble-sent-text" : "bubble-received-text");

        // Heure + checkmarks
        String timeStr = m.getCreatedAt() != null
                ? m.getCreatedAt().format(TIME_SHORT) + (m.isEdited() ? "  ✎" : "  ✓✓")
                : "✓✓";
        Label time = new Label(timeStr);
        time.getStyleClass().add(isMine ? "bubble-time" : "bubble-time-received");

        // Footer heure aligné à droite
        Region timeSpacer = new Region();
        HBox.setHgrow(timeSpacer, Priority.ALWAYS);
        HBox footer = new HBox(timeSpacer, time);

        // Contenu bulle
        VBox bubbleContent;
        if (m.isPinned()) {
            Label pin = new Label("📌 Épinglé");
            pin.getStyleClass().add("bubble-pin");
            bubbleContent = new VBox(3, pin, content, footer);
        } else {
            bubbleContent = new VBox(3, content, footer);
        }
        bubbleContent.getStyleClass().add(isMine ? "bubble-sent" : "bubble-received");
        bubbleContent.setMaxWidth(420);
        bubbleContent.setMinWidth(120);

        // Avatar
        String initials = authorName.length() >= 2
                ? authorName.substring(0, 2).toUpperCase()
                : authorName.toUpperCase();
        Label avatarLbl = new Label(initials);
        avatarLbl.getStyleClass().add("avatar-text");
        StackPane avatar = new StackPane(avatarLbl);
        avatar.getStyleClass().add("avatar");
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);

        // Row
        HBox row = new HBox(10);
        row.setMaxWidth(Double.MAX_VALUE);

        if (isMine) {
            Label nameLbl = new Label("Vous");
            nameLbl.getStyleClass().add("bubble-author-sent");
            VBox withName = new VBox(3, nameLbl, bubbleContent);
            withName.setAlignment(Pos.CENTER_RIGHT);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(spacer, withName, avatar);
        } else {
            Label nameLbl = new Label(authorName);
            nameLbl.getStyleClass().add("bubble-author");
            VBox withName = new VBox(3, nameLbl, bubbleContent);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(avatar, withName);
        }

        VBox.setMargin(row, new Insets(4, 0, 4, 0));
        return row;
    }

    private Label emptyState(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("empty-state");
        return l;
    }

    private void scrollMessagesToBottom() {
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    // ══════════════════════════════════════════════════════════════════════
    // SEND MESSAGE
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void onSendMessage() {
        if (current == null) return;
        Optional<Integer> uid = AppSession.getCurrentUser().map(User::getId);
        if (uid.isEmpty()) return;
        String text = messageField.getText() != null ? messageField.getText().trim() : "";
        if (text.isEmpty()) return;
        try {
            messageService.postMessage(uid.get(), current.chatroomId(), text);
            messageField.clear();
            replaceRoomSnippet(current.chatroomId(), text);
            refreshMessages();
            chatListView.refresh();
        } catch (SQLException | IllegalArgumentException e) {
>>>>>>> origin/chatroom
            new Alert(Alert.AlertType.WARNING, e.getMessage()).showAndWait();
        }
    }

<<<<<<< HEAD
    @FXML
    private void onPendingRequests() {
        if (current == null) {
            return;
        }
        int goalId = current.goalId();
        try {
            List<GoalParticipation> pending = participationService.findPendingByGoal(goalId);
=======
    // ══════════════════════════════════════════════════════════════════════
    // ADMIN ACTIONS
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void onPendingRequests() {
        if (current == null) return;
        try {
            List<GoalParticipation> pending = participationService.findPendingByGoal(current.goalId());
>>>>>>> origin/chatroom
            if (pending.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Aucune demande en attente.").showAndWait();
                return;
            }
            ButtonType accept = new ButtonType("Accepter");
            ButtonType refuse = new ButtonType("Refuser");
            for (GoalParticipation gp : pending) {
<<<<<<< HEAD
                String name = userService.findById(gp.getUserId()).map(ChatroomHubController::formatUser).orElse("#" + gp.getUserId());
                Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                a.setTitle("Demande de participation");
                a.setHeaderText(name);
                a.setContentText("Cet utilisateur souhaite rejoindre l'objectif « " + current.goalTitle() + " ».");
                a.getButtonTypes().setAll(accept, refuse, ButtonType.CANCEL);
                Optional<ButtonType> res = a.showAndWait();
                if (res.isEmpty() || res.get() == ButtonType.CANCEL) {
                    break;
                }
                try {
                    if (res.get() == accept) {
                        lifecycle.approve(gp.getId());
                    } else if (res.get() == refuse) {
                        lifecycle.reject(gp.getId());
                    }
=======
                String name = resolveUserName(gp.getUserId());
                Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                a.setTitle("Demande de participation");
                a.setHeaderText(name);
                a.setContentText("Cet utilisateur souhaite rejoindre « " + current.goalTitle() + " ».");
                a.getButtonTypes().setAll(accept, refuse, ButtonType.CANCEL);
                Optional<ButtonType> res = a.showAndWait();
                if (res.isEmpty() || res.get() == ButtonType.CANCEL) break;
                try {
                    if (res.get() == accept) lifecycle.approve(gp.getId());
                    else if (res.get() == refuse) lifecycle.reject(gp.getId());
>>>>>>> origin/chatroom
                } catch (SQLException ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            }
<<<<<<< HEAD
            if (current != null) {
                selectRoom(current);
            }
=======
            if (current != null) selectRoom(current);
>>>>>>> origin/chatroom
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

<<<<<<< HEAD
    @FXML
    private void onLock() {
        setRoomState("inactive");
    }

    @FXML
    private void onArchive() {
        setRoomState("inactive");
    }

    private void setRoomState(String state) {
        if (current == null) {
            return;
        }
        try {
            Optional<Chatroom> opt = chatroomService.findById(current.chatroomId());
            if (opt.isEmpty()) {
                return;
            }
            Chatroom c = opt.get();
            c.setState(state);
            chatroomService.update(c);
            new Alert(Alert.AlertType.INFORMATION, "Chatroom mis à jour.").showAndWait();
=======
    @FXML private void onLock()    { setRoomState("inactive"); }
    @FXML private void onArchive() { setRoomState("inactive"); }

    private void setRoomState(String state) {
        if (current == null) return;
        try {
            Optional<Chatroom> opt = chatroomService.findById(current.chatroomId());
            if (opt.isEmpty()) return;
            Chatroom c = opt.get();
            c.setState(state);
            chatroomService.update(c);
            selectRoom(current); // refresh header + input state
>>>>>>> origin/chatroom
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onDeleteRoom() {
<<<<<<< HEAD
        if (current == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer définitivement ce chat et ses messages ?");
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) {
            return;
        }
        try {
            messageService.deleteByChatroomId(current.chatroomId());
            chatroomService.delete(current.chatroomId());
            chatListView.getItems().remove(current);
            current = null;
            headerTitleLabel.setText("—");
            messagesBox.getChildren().clear();
=======
        if (current == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer définitivement ce chat et ses messages ?");
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) return;
        try {
            messageService.deleteByChatroomId(current.chatroomId());
            chatroomService.delete(current.chatroomId());
            allRooms.remove(current);
            current = null;
            headerTitleLabel.setText("—");
            headerSubLabel.setText("");
            headerAvatarLabel.setText("?");
            messagesBox.getChildren().clear();
            membersListView.getItems().clear();
            memberCountLabel.setText("Membres");
            photoCount.setText("— photos");
            videoCount.setText("— vidéos");
            messageField.setDisable(true);
            if (!allRooms.isEmpty()) {
                chatListView.getSelectionModel().selectFirst();
            }
>>>>>>> origin/chatroom
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }
<<<<<<< HEAD
=======

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /** Résout le nom d'un utilisateur avec cache pour éviter les requêtes répétées */
    private String resolveUserName(int userId) {
        return userNameCache.computeIfAbsent(userId, id -> {
            try {
                return userService.findById(id)
                        .map(ChatroomHubController::formatUser)
                        .orElse("User #" + id);
            } catch (Exception e) {
                return "User #" + id;
            }
        });
    }

    private static String formatUser(User u) {
        String a = u.getFirstName() != null ? u.getFirstName() : "";
        String b = u.getLastName()  != null ? u.getLastName()  : "";
        String s = (a + " " + b).trim();
        return s.isEmpty() ? ("#" + u.getId()) : s;
    }
>>>>>>> origin/chatroom
}

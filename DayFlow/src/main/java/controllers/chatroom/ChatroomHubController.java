package controllers.chatroom;

import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
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
import services.chatroom_module.ReactionService;
import session.AppSession;
import session.ChatroomNav;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ChatroomHubController {

    // ── Formatters ────────────────────────────────────────────────────────
    private static final DateTimeFormatter TIME_SHORT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

    // ── Avatar color palette (like the template image) ────────────────────
    private static final String[] AVATAR_COLORS = {
        "#6c63ff", "#f97316", "#10b981", "#ef4444",
        "#3b82f6", "#8b5cf6", "#ec4899", "#14b8a6"
    };

    // ── Services ──────────────────────────────────────────────────────────
    private final ChatroomService              chatroomService     = new ChatroomService();
    private final MessageService               messageService      = new MessageService();
    private final GoalParticipationService     participationService = new GoalParticipationService();
    private final GoalChatroomLifecycleService lifecycle           = new GoalChatroomLifecycleService();
    private final UserService                  userService         = new UserService();
    private final ReactionService              reactionService     = new ReactionService();

    // ── FXML ──────────────────────────────────────────────────────────────
    @FXML private ListView<ChatroomListItem>   chatListView;
    @FXML private Label                        headerTitleLabel;
    @FXML private Label                        headerSubLabel;
    @FXML private Label                        headerAvatarLabel;
    @FXML private ScrollPane                   messagesScroll;
    @FXML private VBox                         messagesBox;
    @FXML private TextField                    messageField;
    @FXML private TextField                    searchField;
    @FXML private Label                        emptyLabel;
    @FXML private Label                        searchErrorLabel;
    @FXML private Button                       pendingBtn;
    @FXML private Button                       lockBtn;
    @FXML private Button                       archiveBtn;
    @FXML private Button                       deleteBtn;
    @FXML private ListView<GoalParticipation>  membersListView;
    @FXML private Label                        memberCountLabel;
    @FXML private Label                        photoCount;
    @FXML private Label                        videoCount;
    // ── Recherche messages ────────────────────────────────────────────────
    @FXML private HBox                         searchMsgBar;
    @FXML private TextField                    msgSearchField;
    @FXML private Label                        searchResultLabel;
    // ── Bannière épinglé ──────────────────────────────────────────────────
    @FXML private VBox                         pinnedBanner;
    @FXML private Label                        pinnedBannerText;
    /** ID du message actuellement épinglé affiché dans la bannière */
    private int                                pinnedMessageId = -1;

    // ── State ─────────────────────────────────────────────────────────────
    private ChatroomListItem                   current;
    private Timeline                           messagePollTimeline;
    private ObservableList<ChatroomListItem>   allRooms;
    private FilteredList<ChatroomListItem>     roomFilter;
    private final Map<Integer, String>         userNameCache = new HashMap<>();

    // ══════════════════════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void initialize() {
        setupChatListCells();
        setupMembersListCells();

        Integer uid = AppSession.getCurrentUser().map(User::getId).orElse(null);
        if (uid == null) {
            messageField.setDisable(true);
            // Pas connecté → rediriger vers login
            Platform.runLater(() -> {
                try {
                    controllers.navigation.NavigationManager.show(
                            "/user/login/login.fxml", "DayFlow — Connexion");
                } catch (Exception e) {
                    headerTitleLabel.setText("Non connecté — veuillez vous reconnecter.");
                }
            });
            return;
        }

        try {
            allRooms   = FXCollections.observableArrayList(chatroomService.findAccessibleForUser(uid));
            roomFilter = new FilteredList<>(allRooms, p -> true);
            chatListView.setItems(roomFilter);
            if (allRooms.isEmpty()) {
                emptyLabel.setVisible(true);
                emptyLabel.setManaged(true);
                chatListView.setVisible(false);
                chatListView.setManaged(false);
                messageField.setDisable(true);
                headerTitleLabel.setText("Aucun salon");
                headerSubLabel.setText("Créez un objectif pour commencer");
            } else {
                emptyLabel.setVisible(false);
                emptyLabel.setManaged(false);
                chatListView.setVisible(true);
                chatListView.setManaged(true);
                messageField.setDisable(false);
            }
        } catch (SQLException e) {
            headerTitleLabel.setText("Erreur chargement : " + e.getMessage());
            messageField.setDisable(true);
            return;
        }

        chatListView.getSelectionModel().selectedItemProperty()
                .addListener((o, a, b) -> { if (b != null) selectRoom(b); });

        searchField.textProperty().addListener((obs, old, q) -> onSearchInput(q));

        Integer openGid = ChatroomNav.pullOpenGoalId();
        if (openGid != null) {
            allRooms.stream().filter(it -> it.goalId() == openGid).findFirst()
                    .ifPresent(it -> chatListView.getSelectionModel().select(it));
        }
        if (current == null && !allRooms.isEmpty()) {
            chatListView.getSelectionModel().selectFirst();
        }

        messageField.setOnAction(e -> onSendMessage());

        // Compteur de caractères en temps réel
        messageField.textProperty().addListener((obs, old, val) -> {
            int len = val != null ? val.length() : 0;
            if (len > MSG_MAX) {
                messageField.setText(old); // bloquer au-delà du max
                return;
            }
            if (len > MSG_MAX * 0.9) { // >90% → avertissement orange
                messageField.setStyle("-fx-border-color:#f97316; -fx-border-radius:22;");
                messageField.setPromptText(len + "/" + MSG_MAX);
            } else {
                messageField.setStyle("");
                messageField.setPromptText("Your message...");
            }
        });

        messagePollTimeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
            if (current != null) Platform.runLater(this::refreshMessages);
        }));
        messagePollTimeline.setCycleCount(Timeline.INDEFINITE);
        messagePollTimeline.play();
    }

    // ══════════════════════════════════════════════════════════════════════
    // SIDEBAR CELLS — avatar coloré + nom + preview + badge
    // ══════════════════════════════════════════════════════════════════════

    private void setupChatListCells() {
        chatListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ChatroomListItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }

                // Avatar coloré
                String initials = initials2(item.goalTitle());
                String color    = avatarColor(item.goalId());
                Label  avatarLbl = new Label(initials);
                avatarLbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:13px;");
                StackPane avatar = new StackPane(avatarLbl);
                avatar.setStyle("-fx-background-color:" + color + "; -fx-background-radius:50%;");
                avatar.setMinSize(44, 44); avatar.setMaxSize(44, 44);

                // Nom
                Label nameLbl = new Label(item.goalTitle());
                nameLbl.getStyleClass().add("sidebar-item-name");

                // Preview
                Label previewLbl = new Label(formatSnippet(item.lastMessageSnippet()));
                previewLbl.getStyleClass().add("sidebar-item-preview");
                previewLbl.setMaxWidth(140);

                VBox textBox = new VBox(3, nameLbl, previewLbl);
                textBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                // Badge non lus
                HBox badgeBox = new HBox();
                badgeBox.setAlignment(Pos.TOP_RIGHT);
                if (item.unreadCount() > 0) {
                    Label badge = new Label(item.unreadCount() > 99 ? "99+" : String.valueOf(item.unreadCount()));
                    badge.getStyleClass().add("badge");
                    badgeBox.getChildren().add(badge);
                }

                HBox cell = new HBox(12, avatar, textBox, badgeBox);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setPadding(new Insets(6, 4, 6, 4));
                setGraphic(cell); setText(null);
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // MEMBERS CELLS — avatar coloré + nom + rôle
    // ══════════════════════════════════════════════════════════════════════

    private void setupMembersListCells() {
        membersListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GoalParticipation gp, boolean empty) {
                super.updateItem(gp, empty);
                if (empty || gp == null) { setGraphic(null); setText(null); return; }

                String name  = resolveUserName(gp.getUserId());
                String color = avatarColor(gp.getUserId());

                Label avatarLbl = new Label(initials2(name));
                avatarLbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:10px;");
                StackPane avatar = new StackPane(avatarLbl);
                avatar.setStyle("-fx-background-color:" + color + "; -fx-background-radius:50%;");
                avatar.setMinSize(34, 34); avatar.setMaxSize(34, 34);

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
                setGraphic(cell); setText(null);
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // SEARCH
    // ══════════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════════
    // SEARCH — contrôle de saisie
    // ══════════════════════════════════════════════════════════════════════

    private static final int SEARCH_MAX_LENGTH = 50;
    private static final java.util.regex.Pattern SEARCH_INVALID =
            java.util.regex.Pattern.compile("[<>\"'%;()&+]");

    private void onSearchInput(String raw) {
        if (roomFilter == null) return;

        // 1. Champ vide → réinitialiser
        if (raw == null || raw.isBlank()) {
            searchField.setStyle("");
            searchField.setTooltip(null);
            hideSearchError();
            roomFilter.setPredicate(p -> true);
            return;
        }

        // 2. Longueur max → tronquer silencieusement
        if (raw.length() > SEARCH_MAX_LENGTH) {
            searchField.setText(raw.substring(0, SEARCH_MAX_LENGTH));
            return;
        }

        // 3. Caractères interdits → supprimer + feedback
        if (SEARCH_INVALID.matcher(raw).find()) {
            searchField.setStyle("-fx-border-color:#ef4444; -fx-border-radius:8;");
            showSearchError("Caractères interdits supprimés : < > \" ' % ; ( ) & +");
            String cleaned = SEARCH_INVALID.matcher(raw).replaceAll("");
            searchField.setText(cleaned);
            return;
        }

        // 4. Trop court (1 seul char) → avertissement
        if (raw.trim().length() < 2) {
            searchField.setStyle("-fx-border-color:#f97316; -fx-border-radius:8;");
            showSearchError("Entrez au moins 2 caractères.");
            roomFilter.setPredicate(p -> true);
            return;
        }

        // 5. Saisie valide
        searchField.setStyle("-fx-border-color:#6c63ff; -fx-border-radius:8;");
        hideSearchError();
        filterRooms(raw.trim());
    }

    private void showSearchError(String msg) {
        if (searchErrorLabel == null) return;
        searchErrorLabel.setText(msg);
        searchErrorLabel.setVisible(true);
        searchErrorLabel.setManaged(true);
        // Auto-hide après 3s
        new Timeline(new KeyFrame(Duration.seconds(3), e -> hideSearchError())).play();
    }

    private void hideSearchError() {
        if (searchErrorLabel == null) return;
        searchErrorLabel.setVisible(false);
        searchErrorLabel.setManaged(false);
        searchErrorLabel.setText("");
    }

    private void filterRooms(String query) {
        if (roomFilter == null) return;
        if (query == null || query.isBlank()) {
            roomFilter.setPredicate(p -> true);
            chatListView.setPlaceholder(null);
        } else {
            String lower = query.toLowerCase();
            roomFilter.setPredicate(it -> it.goalTitle().toLowerCase().contains(lower));
            // Message si aucun résultat
            Label noResult = new Label("Aucun chat trouvé pour « " + query.trim() + " »");
            noResult.setStyle("-fx-text-fill:#9ca3af; -fx-font-size:12px; -fx-font-style:italic;");
            chatListView.setPlaceholder(noResult);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SELECT ROOM
    // ══════════════════════════════════════════════════════════════════════

    private void selectRoom(ChatroomListItem item) {
        current = item;
        headerTitleLabel.setText(item.goalTitle());
        headerAvatarLabel.setText(initials2(item.goalTitle()));

        try {
            List<GoalParticipation> members = participationService.findApprovedByGoal(item.goalId());
            headerSubLabel.setText(members.size() + " membres");
            memberCountLabel.setText(members.size() + " membres");
            membersListView.setItems(FXCollections.observableArrayList(members));

            int uid   = AppSession.getCurrentUser().map(User::getId).orElse(0);
            boolean admin = participationService.isOwnerOrAdmin(uid, item.goalId());
            setAdminControls(admin);

            boolean open = chatroomService.findById(item.chatroomId())
                    .map(c -> "active".equalsIgnoreCase(c.getState())).orElse(true);
            messageField.setDisable(false);
            messageField.setPromptText("Your message...");
        } catch (Exception e) {
            headerSubLabel.setText("");
            messageField.setDisable(true);
        }
        refreshMessages();
    }

    private void setAdminControls(boolean v) {
        pendingBtn.setVisible(v); pendingBtn.setManaged(v);
        lockBtn.setVisible(v);    lockBtn.setManaged(v);
        archiveBtn.setVisible(v); archiveBtn.setManaged(v);
        deleteBtn.setVisible(v);  deleteBtn.setManaged(v);
    }

    // ══════════════════════════════════════════════════════════════════════
    // MESSAGES
    // ══════════════════════════════════════════════════════════════════════

    private void refreshMessages() {
        messagesBox.getChildren().clear();
        if (current == null) {
            resetStats();
            messagesBox.getChildren().add(emptyState("Sélectionnez un chat."));
            return;
        }
        try {
            List<Message> msgs = messageService.findByChatroomId(current.chatroomId());
            updateFileStats(msgs);
            if (msgs.isEmpty()) {
                messagesBox.getChildren().add(emptyState("Pas encore de messages. Lancez la conversation !"));
                return;
            }
            int me = AppSession.getCurrentUser().map(User::getId).orElse(-1);
            for (Message m : msgs) {
                messagesBox.getChildren().add(buildBubble(m, m.getAuthorId() == me));
            }
            // Bannière message épinglé
            updatePinnedBanner(msgs);
        } catch (SQLException e) {
            messagesBox.getChildren().add(emptyState("Erreur : " + e.getMessage()));
        }
        scrollToBottom();
    }

    private HBox buildBubble(Message m, boolean isMine) {
        String authorName = resolveUserName(m.getAuthorId());
        String color      = avatarColor(m.getAuthorId());
        int    me         = AppSession.getCurrentUser().map(User::getId).orElse(-1);

        // ── Preview réponse ───────────────────────────────────────────────
        VBox replyPreview = null;
        if (m.getReplyToId() > 0) {
            try {
                String parentContent = messageService.findContentById(m.getReplyToId());
                if (parentContent != null) {
                    String preview = parentContent.length() > 50
                            ? parentContent.substring(0, 50) + "…" : parentContent;
                    Label replyLbl = new Label("↪  " + preview);
                    replyLbl.setStyle(
                        "-fx-font-size:11px; -fx-text-fill:" + (isMine ? "rgba(255,255,255,0.7)" : "#6c63ff") + ";" +
                        "-fx-background-color:" + (isMine ? "rgba(255,255,255,0.15)" : "#f0f1f8") + ";" +
                        "-fx-background-radius:6; -fx-padding:4 8;");
                    replyLbl.setWrapText(true);
                    replyLbl.setMaxWidth(380);
                    replyPreview = new VBox(replyLbl);
                }
            } catch (Exception ignored) {}
        }

        // ── Texte ─────────────────────────────────────────────────────────
        Label content = new Label(m.getContent());
        content.setWrapText(true);
        content.setMaxWidth(400);
        content.getStyleClass().add(isMine ? "bubble-sent-text" : "bubble-received-text");

        // ── Heure ─────────────────────────────────────────────────────────
        String timeStr = m.getCreatedAt() != null
                ? m.getCreatedAt().format(TIME_SHORT) + (m.isEdited() ? " ✎" : " ✓✓") : "✓✓";
        Label time = new Label(timeStr);
        time.getStyleClass().add(isMine ? "bubble-time" : "bubble-time-received");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(spacer, time);

        // ── Réactions ─────────────────────────────────────────────────────
        HBox reactionsRow = buildReactionsRow(m, me);

        // ── Bulle ─────────────────────────────────────────────────────────
        VBox bubble = new VBox(3);
        if (m.isPinned()) {
            Label pin = new Label("📌 Épinglé");
            pin.getStyleClass().add("bubble-pin");
            bubble.getChildren().add(pin);
        }
        if (replyPreview != null) bubble.getChildren().add(replyPreview);
        bubble.getChildren().addAll(content, footer);
        if (!reactionsRow.getChildren().isEmpty()) bubble.getChildren().add(reactionsRow);

        bubble.getStyleClass().add(isMine ? "bubble-sent" : "bubble-received");
        bubble.setMaxWidth(440);
        bubble.setMinWidth(100);

        // ── Avatar ────────────────────────────────────────────────────────
        Label avatarLbl = new Label(initials2(authorName));
        avatarLbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:11px;");
        StackPane avatar = new StackPane(avatarLbl);
        avatar.setStyle("-fx-background-color:" + color + "; -fx-background-radius:50%;");
        avatar.setMinSize(36, 36); avatar.setMaxSize(36, 36);

        // ── Row ───────────────────────────────────────────────────────────
        HBox row = new HBox(10);
        row.setMaxWidth(Double.MAX_VALUE);

        if (isMine) {
            Label nameLbl = new Label("Vous");
            nameLbl.getStyleClass().add("bubble-author-sent");
            VBox withName = new VBox(3, nameLbl, bubble);
            withName.setAlignment(Pos.CENTER_RIGHT);
            Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(s, withName, avatar);
        } else {
            Label nameLbl = new Label(authorName);
            nameLbl.getStyleClass().add("bubble-author");
            VBox withName = new VBox(3, nameLbl, bubble);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(avatar, withName);
        }
        VBox.setMargin(row, new Insets(4, 0, 4, 0));

        // ── Menu contextuel ───────────────────────────────────────────────
        ContextMenu menu = new ContextMenu();

        // Répondre (tout le monde)
        MenuItem replyItem = new MenuItem("↪  Répondre");
        replyItem.setOnAction(e -> onReply(m));
        menu.getItems().add(replyItem);

        // Réagir (tout le monde)
        Menu reactMenu = new Menu("😊  Réagir");
        for (String emoji : model.chatroom.Reaction.VALID_TYPES) {
            MenuItem emojiItem = new MenuItem(emoji);
            emojiItem.setOnAction(e -> onReact(m.getId(), me, emoji));
            reactMenu.getItems().add(emojiItem);
        }
        // Supprimer réaction si existante
        try {
            String myReaction = reactionService.getUserReaction(m.getId(), me);
            if (myReaction != null) {
                MenuItem removeReact = new MenuItem("✕  Retirer " + myReaction);
                removeReact.setOnAction(e -> onRemoveReaction(m.getId(), me));
                reactMenu.getItems().addAll(new SeparatorMenuItem(), removeReact);
            }
        } catch (Exception ignored) {}
        menu.getItems().add(reactMenu);

        if (isMine) {
            menu.getItems().add(new SeparatorMenuItem());
            MenuItem editItem = new MenuItem("✏️  Modifier");
            editItem.setOnAction(e -> onEditMessage(m));
            MenuItem pinItem = new MenuItem(m.isPinned() ? "📌  Désépingler" : "📌  Épingler");
            pinItem.setOnAction(e -> onTogglePin(m));
            MenuItem deleteItem = new MenuItem("🗑  Supprimer");
            deleteItem.setStyle("-fx-text-fill:#ef4444;");
            deleteItem.setOnAction(e -> onDeleteMessage(m.getId()));
            menu.getItems().addAll(editItem, pinItem, deleteItem);
        }

        row.setOnContextMenuRequested(e -> menu.show(row, e.getScreenX(), e.getScreenY()));
        return row;
    }

    /** Construit la ligne de réactions sous la bulle */
    private HBox buildReactionsRow(Message m, int me) {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER_LEFT);
        try {
            Map<String, Integer> counts = reactionService.countByMessage(m.getId());
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                Label badge = new Label(entry.getKey() + " " + entry.getValue());
                badge.setStyle(
                    "-fx-background-color:rgba(108,99,255,0.12); -fx-background-radius:12;" +
                    "-fx-padding:2 8; -fx-font-size:11px; -fx-cursor:hand;");
                badge.setOnMouseClicked(e -> onReact(m.getId(), me, entry.getKey()));
                row.getChildren().add(badge);
            }
        } catch (Exception ignored) {}
        return row;
    }

    /** Met à jour la bannière épinglée en haut de la zone messages */
    private void updatePinnedBanner(List<Message> msgs) {
        msgs.stream()
            .filter(Message::isPinned)
            .findFirst()
            .ifPresentOrElse(m -> {
                pinnedMessageId = m.getId();
                String preview = m.getContent().length() > 60
                        ? m.getContent().substring(0, 60) + "…"
                        : m.getContent();
                pinnedBannerText.setText(preview);
                pinnedBanner.setVisible(true);
                pinnedBanner.setManaged(true);
            }, () -> {
                pinnedBanner.setVisible(false);
                pinnedBanner.setManaged(false);
                pinnedMessageId = -1;
            });
    }

    @FXML
    private void onUnpinMessage() {
        if (pinnedMessageId < 0) return;
        try {
            messageService.togglePin(pinnedMessageId, false);
            pinnedBanner.setVisible(false);
            pinnedBanner.setManaged(false);
            pinnedMessageId = -1;
            refreshMessages();
        } catch (SQLException e) {
            messagesBox.getChildren().add(emptyState("⚠️ " + e.getMessage()));
        }
    }

    private void updateFileStats(List<Message> msgs) {
        int photos = 0, videos = 0;
        for (Message m : msgs) {
            String c = m.getContent() != null ? m.getContent().toLowerCase(Locale.ROOT) : "";
            if (c.contains(".jpg") || c.contains(".jpeg") || c.contains(".png") || c.contains(".gif")) photos++;
            else if (c.contains(".mp4") || c.contains(".webm") || c.contains(".mov")) videos++;
        }
        photoCount.setText(photos + " photo" + (photos != 1 ? "s" : ""));
        videoCount.setText(videos + " vidéo" + (videos != 1 ? "s" : ""));
    }

    private void resetStats() {
        photoCount.setText("— photos");
        videoCount.setText("— vidéos");
    }

    private Label emptyState(String text) {
        Label l = new Label(text); l.getStyleClass().add("empty-state"); return l;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    // ══════════════════════════════════════════════════════════════════════
    // ══════════════════════════════════════════════════════════════════════
    // RECHERCHE MESSAGES
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void onToggleSearch() {
        boolean visible = !searchMsgBar.isVisible();
        searchMsgBar.setVisible(visible);
        searchMsgBar.setManaged(visible);
        if (visible) {
            msgSearchField.requestFocus();
            // Recherche en temps réel
            msgSearchField.textProperty().addListener((obs, old, val) -> doSearch(val));
        } else {
            msgSearchField.clear();
            searchResultLabel.setText("");
            refreshMessages(); // restaurer tous les messages
        }
    }

    @FXML
    private void onCloseSearch() {
        searchMsgBar.setVisible(false);
        searchMsgBar.setManaged(false);
        msgSearchField.clear();
        searchResultLabel.setText("");
        refreshMessages();
    }

    private void doSearch(String keyword) {
        if (current == null) return;

        // Champ vide → afficher tous les messages
        if (keyword == null || keyword.isBlank()) {
            searchResultLabel.setText("");
            msgSearchField.setStyle("");
            refreshMessages();
            return;
        }

        // Contrôle de saisie
        try {
            services.chatroom_module.MessageValidator.validateSearch(keyword);
        } catch (IllegalArgumentException e) {
            searchResultLabel.setText("⚠️ " + e.getMessage());
            searchResultLabel.setStyle("-fx-text-fill:#ef4444;");
            msgSearchField.setStyle("-fx-border-color:#ef4444; -fx-border-radius:8;");
            return;
        }

        // Style valide
        msgSearchField.setStyle("-fx-border-color:#6c63ff; -fx-border-radius:8;");

        // Recherche en BD
        try {
            java.util.List<model.chatroom.Message> results =
                    messageService.rechercher(keyword, current.chatroomId());

            messagesBox.getChildren().clear();
            if (results.isEmpty()) {
                searchResultLabel.setText("Aucun résultat pour « " + keyword + " »");
                searchResultLabel.setStyle("-fx-text-fill:#9ca3af;");
                messagesBox.getChildren().add(emptyState("🔍 Aucun message trouvé pour « " + keyword + " »"));
            } else {
                searchResultLabel.setText(results.size() + " résultat(s)");
                searchResultLabel.setStyle("-fx-text-fill:#6c63ff;");
                int me = AppSession.getCurrentUser().map(User::getId).orElse(-1);
                for (model.chatroom.Message m : results) {
                    messagesBox.getChildren().add(buildBubble(m, m.getAuthorId() == me));
                }
                scrollToBottom();
            }
        } catch (java.sql.SQLException e) {
            searchResultLabel.setText("Erreur : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SEND — contrôle de saisie
    // ══════════════════════════════════════════════════════════════════════

    private static final int MSG_MAX = 1000;
    private static final int MSG_MIN = 1;

    @FXML
    private void onSendMessage() {
        String text = messageField.getText() != null ? messageField.getText().trim() : "";

        // ── Contrôle de saisie ──────────────────────────────────────────
        String validated;
        try {
            validated = services.chatroom_module.MessageValidator.validateContent(text);
        } catch (IllegalArgumentException e) {
            showFieldError(e.getMessage());
            shakeField();
            return;
        }

        if (current == null) {
            if (!allRooms.isEmpty()) chatListView.getSelectionModel().selectFirst();
            if (current == null) return;
        }

        Optional<Integer> uid = AppSession.getCurrentUser().map(User::getId);
        if (uid.isEmpty()) return;

        // ── Envoi ───────────────────────────────────────────────────────
        try {
            messageField.setStyle(""); // reset style
            if (replyingToId > 0) {
                messageService.postReply(uid.get(), current.chatroomId(), validated, replyingToId);
                replyingToId = -1;
                replyingToPreview = "";
                messageField.setPromptText("Your message...");
            } else {
                messageService.postMessage(uid.get(), current.chatroomId(), validated);
            }
            messageField.clear();
            replaceSnippet(current.chatroomId(), text);
            refreshMessages();
            chatListView.refresh();
        } catch (SQLException | IllegalArgumentException e) {
            messagesBox.getChildren().add(emptyState("⚠️ " + e.getMessage()));
        }
    }

    /** Bordure rouge + tooltip sur le champ message */
    private void showFieldError(String msg) {
        messageField.setStyle("-fx-border-color:#ef4444; -fx-border-radius:22;");
        Tooltip tip = new Tooltip(msg);
        tip.setStyle("-fx-background-color:#ef4444; -fx-text-fill:white;");
        messageField.setTooltip(tip);
        Tooltip.install(messageField, tip);
        // Reset après 3 secondes
        new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            messageField.setStyle("");
            messageField.setTooltip(null);
        })).play();
    }

    /** Animation shake si champ vide */
    private void shakeField() {
        javafx.animation.TranslateTransition shake =
                new javafx.animation.TranslateTransition(Duration.millis(60), messageField);
        shake.setFromX(0); shake.setByX(8); shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.play();
    }

    // ══════════════════════════════════════════════════════════════════════
    // EDIT / DELETE MESSAGE
    // ══════════════════════════════════════════════════════════════════════

    private void onEditMessage(Message m) {
        // Dialog custom stylé
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Modifier le message");
        dialog.setResizable(false);

        // Champ de saisie
        TextField editor = new TextField(m.getContent());
        editor.setStyle(
            "-fx-background-color:#f0f1f8; -fx-background-radius:10;" +
            "-fx-border-color:#6c63ff; -fx-border-radius:10;" +
            "-fx-padding:10 14; -fx-font-size:14px;");
        editor.setPrefWidth(420);

        // Compteur
        Label counter = new Label(m.getContent().length() + "/" + MSG_MAX);
        counter.setStyle("-fx-font-size:11px; -fx-text-fill:#9ca3af;");
        editor.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > MSG_MAX) {
                editor.setText(old);
                return;
            }
            int len = val != null ? val.length() : 0;
            counter.setText(len + "/" + MSG_MAX);
            counter.setStyle("-fx-font-size:11px; -fx-text-fill:" +
                (len > MSG_MAX * 0.9 ? "#f97316" : "#9ca3af") + ";");
        });

        // Erreur
        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill:#ef4444; -fx-font-size:12px;");
        errorLbl.setWrapText(true);

        // Boutons
        Button btnOk = new Button("✅  Enregistrer");
        btnOk.setStyle(
            "-fx-background-color:linear-gradient(to right,#6c63ff,#764ba2);" +
            "-fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:10;" +
            "-fx-padding:10 24; -fx-cursor:hand;");

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle(
            "-fx-background-color:#f0f1f8; -fx-text-fill:#6c63ff;" +
            "-fx-font-weight:bold; -fx-background-radius:10;" +
            "-fx-padding:10 24; -fx-cursor:hand;");
        btnCancel.setOnAction(e -> dialog.close());

        btnOk.setOnAction(e -> {
            String trimmed = editor.getText() != null ? editor.getText().trim() : "";
            try {
                String validated = services.chatroom_module.MessageValidator.validateContent(trimmed);
                Message updated = new Message();
                updated.setId(m.getId());
                updated.setContent(validated);
                updated.setEdited(true);
                updated.setPinned(m.isPinned());
                messageService.update(updated);
                dialog.close();
                refreshMessages();
            } catch (IllegalArgumentException ex) {
                errorLbl.setText("⚠️ " + ex.getMessage());
            } catch (Exception ex) {
                errorLbl.setText("⚠️ " + ex.getMessage());
            }
        });

        // Layout
        HBox buttons = new HBox(12, btnOk, btnCancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(14);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color:white; -fx-background-radius:16;");

        Label title = new Label("✏️  Modifier le message");
        title.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a1a2e;");

        Label hint = new Label("Modifiez votre message ci-dessous :");
        hint.setStyle("-fx-font-size:12px; -fx-text-fill:#9ca3af;");

        HBox counterRow = new HBox();
        counterRow.setAlignment(Pos.CENTER_RIGHT);
        counterRow.getChildren().add(counter);

        content.getChildren().addAll(title, hint, editor, counterRow, errorLbl, buttons);

        javafx.scene.Scene scene = new javafx.scene.Scene(content, 480, 240);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        editor.requestFocus();
        editor.selectAll();
        dialog.showAndWait();
    }

    private void onDeleteMessage(int messageId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer ce message ?");
        confirm.setTitle("Supprimer");
        confirm.setHeaderText(null);
        confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            try {
                // Soft delete — le message reste en BD avec is_spam=true
                messageService.softDelete(messageId);
                refreshMessages();
                chatListView.refresh();
            } catch (SQLException e) {
                messagesBox.getChildren().add(emptyState("⚠️ " + e.getMessage()));
            }
        });
    }

    private void onTogglePin(Message m) {
        try {
            boolean newPin = !m.isPinned();
            messageService.togglePin(m.getId(), newPin);
            refreshMessages();
        } catch (SQLException e) {
            messagesBox.getChildren().add(emptyState("⚠️ " + e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // REPLY
    // ══════════════════════════════════════════════════════════════════════

    /** État de réponse en cours */
    private int replyingToId = -1;
    private String replyingToPreview = "";

    private void onReply(Message m) {
        replyingToId = m.getId();
        replyingToPreview = m.getContent().length() > 40
                ? m.getContent().substring(0, 40) + "…" : m.getContent();
        messageField.setPromptText("↪ Réponse à : " + replyingToPreview);
        messageField.setStyle("-fx-border-color:#6c63ff; -fx-border-radius:22;");
        messageField.requestFocus();
    }

    // ══════════════════════════════════════════════════════════════════════
    // REACTIONS
    // ══════════════════════════════════════════════════════════════════════

    private void onReact(int messageId, int userId, String emoji) {
        if (userId <= 0) return;
        try {
            reactionService.addOrUpdate(messageId, userId, emoji);
            refreshMessages();
        } catch (Exception e) {
            messagesBox.getChildren().add(emptyState("⚠️ " + e.getMessage()));
        }
    }

    private void onRemoveReaction(int messageId, int userId) {
        try {
            reactionService.delete(messageId, userId);
            refreshMessages();
        } catch (Exception e) {
            messagesBox.getChildren().add(emptyState("⚠️ " + e.getMessage()));
        }
    }

    private void replaceSnippet(int chatroomId, String content) {
        for (int i = 0; i < allRooms.size(); i++) {
            ChatroomListItem it = allRooms.get(i);
            if (it.chatroomId() != chatroomId) continue;
            ChatroomListItem updated = new ChatroomListItem(
                    it.chatroomId(), it.goalId(), it.goalTitle(), content, it.unreadCount());
            allRooms.set(i, updated);
            if (current != null && current.chatroomId() == chatroomId) current = updated;
            break;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ADMIN
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void onPendingRequests() {
        if (current == null) return;
        try {
            List<GoalParticipation> pending = participationService.findPendingByGoal(current.goalId());
            if (pending.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Aucune demande en attente.").showAndWait();
                return;
            }
            ButtonType accept = new ButtonType("Accepter");
            ButtonType refuse = new ButtonType("Refuser");
            for (GoalParticipation gp : pending) {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                a.setTitle("Demande"); a.setHeaderText(resolveUserName(gp.getUserId()));
                a.setContentText("Rejoindre « " + current.goalTitle() + " » ?");
                a.getButtonTypes().setAll(accept, refuse, ButtonType.CANCEL);
                Optional<ButtonType> res = a.showAndWait();
                if (res.isEmpty() || res.get() == ButtonType.CANCEL) break;
                try {
                    if (res.get() == accept) lifecycle.approve(gp.getId());
                    else lifecycle.reject(gp.getId());
                } catch (SQLException ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            }
            if (current != null) selectRoom(current);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML private void onLock()    { setRoomState("inactive"); }
    @FXML private void onArchive() { setRoomState("inactive"); }

    private void setRoomState(String state) {
        if (current == null) return;
        try {
            Optional<Chatroom> opt = chatroomService.findById(current.chatroomId());
            if (opt.isEmpty()) return;
            Chatroom c = opt.get(); c.setState(state); chatroomService.update(c);
            selectRoom(current);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onDeleteRoom() {
        if (current == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce chat ?");
        if (confirm.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) return;
        try {
            messageService.deleteByChatroomId(current.chatroomId());
            chatroomService.delete(current.chatroomId());
            allRooms.remove(current);
            current = null;
            headerTitleLabel.setText("—"); headerSubLabel.setText("");
            messagesBox.getChildren().clear();
            membersListView.getItems().clear();
            memberCountLabel.setText("Members");
            resetStats();
            messageField.setDisable(true);
            if (!allRooms.isEmpty()) {
                chatListView.getSelectionModel().selectFirst();
            } else {
                emptyLabel.setVisible(true);
                emptyLabel.setManaged(true);
                chatListView.setVisible(false);
                chatListView.setManaged(false);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private String resolveUserName(int userId) {
        return userNameCache.computeIfAbsent(userId, id -> {
            try {
                return userService.findById(id).map(ChatroomHubController::formatUser)
                        .orElse("User #" + id);
            } catch (Exception e) { return "User #" + id; }
        });
    }

    private static String formatUser(User u) {
        String a = u.getFirstName() != null ? u.getFirstName() : "";
        String b = u.getLastName()  != null ? u.getLastName()  : "";
        String s = (a + " " + b).trim();
        return s.isEmpty() ? "#" + u.getId() : s;
    }

    private static String initials2(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        return name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
    }

    private String avatarColor(int id) {
        return AVATAR_COLORS[Math.abs(id) % AVATAR_COLORS.length];
    }

    private static String formatSnippet(String content) {
        if (content == null || content.isBlank()) return "Pas encore de messages";
        String t = content.trim();
        return t.length() > 35 ? t.substring(0, 35) + "…" : t;
    }
}

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
import services.chatroom_module.AudioRecorderService;
import services.chatroom_module.ReactionService;
import services.chatroom_module.TranslationService;
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
    private final TranslationService           translationService  = new TranslationService();

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
    // ── Zone traduction ───────────────────────────────────────────────────
    @FXML private VBox                         translationZone;
    @FXML private Label                        translationStatusLabel;
    @FXML private Label                        translationResultLabel;
    @FXML private Label                        translationOriginalLabel;
    // ── Enregistrement audio ──────────────────────────────────────────────
    @FXML private Button                       recordBtn;
    private final AudioRecorderService         audioRecorder = new AudioRecorderService();
    private Timeline                           recordingTimer;
    private int                                recordingSeconds = 0;

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
                int    me    = AppSession.getCurrentUser().map(User::getId).orElse(-1);
                boolean isMeAdmin = current != null && isCurrentUserAdmin();

                // Avatar
                Label avatarLbl = new Label(initials2(name));
                avatarLbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:10px;");
                StackPane avatar = new StackPane(avatarLbl);
                avatar.setStyle("-fx-background-color:" + color + "; -fx-background-radius:50%;");
                avatar.setMinSize(34, 34); avatar.setMaxSize(34, 34);

                // Nom + rôle
                Label nameLbl = new Label(name);
                nameLbl.getStyleClass().add("member-name");
                boolean isAdmin = GoalParticipation.ROLE_OWNER.equals(gp.getRole())
                        || GoalParticipation.ROLE_ADMIN.equals(gp.getRole());
                Label roleLbl = new Label(gp.getRole().toLowerCase());
                roleLbl.getStyleClass().add(isAdmin ? "member-role-admin" : "member-role");

                VBox textBox = new VBox(2, nameLbl, roleLbl);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                // Bouton action (quitter pour moi, gérer pour admin)
                HBox cell = new HBox(10, avatar, textBox);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setPadding(new Insets(4, 0, 4, 0));

                // Menu contextuel sur les membres
                ContextMenu memberMenu = new ContextMenu();

                // Quitter (pour l'utilisateur lui-même, pas l'owner)
                if (gp.getUserId() == me && !GoalParticipation.ROLE_OWNER.equals(gp.getRole())) {
                    MenuItem leaveItem = new MenuItem("🚪  Quitter l'objectif");
                    leaveItem.setStyle("-fx-text-fill:#ef4444;");
                    leaveItem.setOnAction(e -> onLeaveGoal(gp));
                    memberMenu.getItems().add(leaveItem);
                }

                // Actions admin sur les autres membres
                if (isMeAdmin && gp.getUserId() != me) {
                    if (GoalParticipation.ROLE_MEMBER.equals(gp.getRole())) {
                        MenuItem promoteItem = new MenuItem("⬆️  Promouvoir admin");
                        promoteItem.setOnAction(e -> onPromote(gp));
                        memberMenu.getItems().add(promoteItem);
                    } else if (GoalParticipation.ROLE_ADMIN.equals(gp.getRole())) {
                        MenuItem demoteItem = new MenuItem("⬇️  Rétrograder membre");
                        demoteItem.setOnAction(e -> onDemote(gp));
                        memberMenu.getItems().add(demoteItem);
                    }
                    if (!GoalParticipation.ROLE_OWNER.equals(gp.getRole())) {
                        MenuItem kickItem = new MenuItem("❌  Exclure");
                        kickItem.setStyle("-fx-text-fill:#ef4444;");
                        kickItem.setOnAction(e -> onKickMember(gp));
                        memberMenu.getItems().add(kickItem);
                    }
                }

                if (!memberMenu.getItems().isEmpty()) {
                    cell.setOnContextMenuRequested(e ->
                        memberMenu.show(cell, e.getScreenX(), e.getScreenY()));
                }

                setGraphic(cell); setText(null);
            }
        });
    }

    /** 📊 Suivi d'activité des membres */
    @FXML
    private void onShowActivity() {
        if (current == null) return;
        try {
            List<int[]> stats = participationService.getActivityStats(current.goalId());
            if (stats.isEmpty()) {
                showNotification("Aucune activité enregistrée.");
                return;
            }
            StringBuilder sb = new StringBuilder("📊 Activité des membres\n\n");
            for (int[] row : stats) {
                String name = resolveUserName(row[0]);
                String bar = "█".repeat(Math.min(row[1], 20));
                sb.append(String.format("%-20s %s %d msg%n", name, bar, row[1]));
            }
            Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString());
            a.setTitle("Suivi d'activité");
            a.setHeaderText(current.goalTitle());
            a.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private boolean isCurrentUserAdmin() {
        if (current == null) return false;
        int uid = AppSession.getCurrentUser().map(User::getId).orElse(0);
        try { return participationService.isOwnerOrAdmin(uid, current.goalId()); }
        catch (Exception e) { return false; }
    }

    private void onLeaveGoal(GoalParticipation gp) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Quitter cet objectif ?");
        confirm.setHeaderText(null);
        confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            try {
                participationService.leaveGoal(gp.getUserId(), gp.getGoalId());
                // Retirer le chatroom de la sidebar
                allRooms.removeIf(it -> it.goalId() == gp.getGoalId());
                current = null;
                headerTitleLabel.setText("—");
                messagesBox.getChildren().clear();
                if (!allRooms.isEmpty()) chatListView.getSelectionModel().selectFirst();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            }
        });
    }

    private void onPromote(GoalParticipation gp) {
        try {
            participationService.promoteToAdmin(gp.getId());
            selectRoom(current);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void onDemote(GoalParticipation gp) {
        try {
            participationService.demoteToMember(gp.getId());
            selectRoom(current);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void onKickMember(GoalParticipation gp) {
        String name = resolveUserName(gp.getUserId());
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Exclure " + name + " de l'objectif ?");
        confirm.setHeaderText(null);
        confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            try {
                participationService.delete(gp.getId());
                selectRoom(current);
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
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

        int uid = AppSession.getCurrentUser().map(User::getId).orElse(0);

        try {
            // ── 🚫 Restriction accès : APPROVED uniquement ────────────
            boolean approved = participationService.isApprovedMember(uid, item.goalId());
            if (!approved) {
                messageField.setDisable(true);
                messageField.setPromptText("⛔ Accès refusé — demande en attente.");
                headerSubLabel.setText("Accès restreint");
                setAdminControls(false);
                messagesBox.getChildren().clear();
                Label restricted = new Label(
                    "⛔  Accès restreint\n\nVotre demande est en attente de validation par l'administrateur.");
                restricted.setWrapText(true);
                restricted.setStyle(
                    "-fx-text-fill:#9ca3af; -fx-font-size:14px; -fx-text-alignment:center;");
                HBox center = new HBox(restricted);
                center.setAlignment(Pos.CENTER);
                messagesBox.getChildren().add(center);
                return;
            }

            // ── Membres ───────────────────────────────────────────────
            List<GoalParticipation> members = participationService.findApprovedByGoal(item.goalId());
            headerSubLabel.setText(members.size() + " membres");
            memberCountLabel.setText(members.size() + " membres");
            membersListView.setItems(FXCollections.observableArrayList(members));

            // ── Droits admin ──────────────────────────────────────────
            boolean admin = participationService.isOwnerOrAdmin(uid, item.goalId());
            setAdminControls(admin);

            // ── 🔔 Notification admin : demandes en attente ───────────
            if (admin) {
                int pending = participationService.countPendingByGoal(item.goalId());
                if (pending > 0) {
                    pendingBtn.setText("⏳ Demandes (" + pending + ")");
                    showNotification("🔔 " + pending + " demande(s) en attente d'approbation");
                } else {
                    pendingBtn.setText("⏳ Demandes");
                }
            }

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

        // ── Contenu principal (texte ou média) ────────────────────────────
        javafx.scene.Node mainContent = buildMediaContent(m, isMine);

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
        bubble.getChildren().addAll(mainContent, footer);
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

        // ── Menu contextuel custom stylé ─────────────────────────────────
        row.setOnContextMenuRequested(e ->
            showMessageMenu(m, isMine, me, row, e.getScreenX(), e.getScreenY()));
        return row;
    }

    /** Menu contextuel custom style iOS */
    private void showMessageMenu(Message m, boolean isMine, int me,
                                  javafx.scene.Node anchor, double x, double y) {
        Stage menuStage = new Stage();
        menuStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        menuStage.initOwner(anchor.getScene().getWindow());

        VBox menuBox = new VBox(0);
        menuBox.setStyle(
            "-fx-background-color:#f2f2f7; -fx-background-radius:14;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.22),16,0,0,6);" +
            "-fx-min-width:240;");

        // ── Important (étoile) ────────────────────────────────────────
        String starLabel = m.isStarred() ? "★  Important" : "☆  Important";
        HBox starRow = menuRowWithIcon(starLabel, m.isStarred() ? "#f59e0b" : "#374151");
        starRow.setOnMouseClicked(e -> { menuStage.close(); onToggleStar(m); });
        menuBox.getChildren().add(starRow);

        // ── Répondre ──────────────────────────────────────────────────
        HBox replyRow = menuRowWithIcon("↩  Répondre", "#374151");
        replyRow.setOnMouseClicked(e -> { menuStage.close(); onReply(m); });
        menuBox.getChildren().addAll(menuSep(), replyRow);

        // ── Transférer ────────────────────────────────────────────────
        HBox forwardRow = menuRowWithIcon("↪  Transférer", "#374151");
        forwardRow.setOnMouseClicked(e -> { menuStage.close(); onForward(m, me); });
        menuBox.getChildren().addAll(menuSep(), forwardRow);

        // ── Copier ────────────────────────────────────────────────────
        HBox copyRow = menuRowWithIcon("⎘  Copier", "#374151");
        copyRow.setOnMouseClicked(e -> {
            menuStage.close();
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(m.getContent()); cb.setContent(cc);
        });
        menuBox.getChildren().addAll(menuSep(), copyRow);

        if (isMine) {
            HBox editRow = menuRowWithIcon("✎  Modifier", "#374151");
            editRow.setOnMouseClicked(e -> { menuStage.close(); onEditMessage(m); });
            menuBox.getChildren().addAll(menuSep(), editRow);

            HBox pinRow = menuRowWithIcon(m.isPinned() ? "✦  Désépingler" : "✦  Épingler", "#374151");
            pinRow.setOnMouseClicked(e -> { menuStage.close(); onTogglePin(m); });
            menuBox.getChildren().addAll(menuSep(), pinRow);
        }

        // ── Traduire ──────────────────────────────────────────────────
        HBox translateRow = menuRowWithIcon("🌐  Traduire", "#374151");
        translateRow.setOnMouseClicked(e -> { menuStage.close(); onTranslate(m); });
        menuBox.getChildren().addAll(menuSep(), translateRow);

        // ── Infos ─────────────────────────────────────────────────────
        HBox infoRow = menuRowWithIcon("ⓘ  Infos", "#374151");
        infoRow.setOnMouseClicked(e -> { menuStage.close(); showMessageInfo(m); });
        menuBox.getChildren().addAll(menuSep(), infoRow);

        // ── Supprimer (rouge) ─────────────────────────────────────────
        HBox deleteRow = menuRowWithIcon("⊠  Supprimer", "#ef4444");
        deleteRow.setStyle(deleteRow.getStyle() + "-fx-background-radius:0 0 14 14;");
        deleteRow.setOnMouseClicked(e -> { menuStage.close(); onDeleteMessage(m.getId()); });
        menuBox.getChildren().addAll(menuSep(), deleteRow);

        javafx.scene.Scene ms = new javafx.scene.Scene(menuBox);
        ms.setFill(javafx.scene.paint.Color.TRANSPARENT);
        menuStage.setScene(ms);
        menuStage.setX(x); menuStage.setY(y);
        menuStage.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) menuStage.close();
        });
        menuBox.setOpacity(0); menuBox.setScaleY(0.85);
        menuStage.show();
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(150), menuBox);
        ft.setToValue(1);
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(150), menuBox);
        st.setToY(1); st.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        new javafx.animation.ParallelTransition(ft, st).play();
    }

    private HBox menuRowWithIcon(String text, String textColor) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:15px; -fx-text-fill:" + textColor + ";");
        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(13, 20, 13, 20));
        row.setStyle("-fx-cursor:hand; -fx-background-color:white;");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        row.setOnMouseEntered(e -> row.setStyle("-fx-cursor:hand; -fx-background-color:#f0f1f8;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-cursor:hand; -fx-background-color:white;"));
        return row;
    }

    private javafx.scene.shape.Rectangle menuSep() {
        javafx.scene.shape.Rectangle r = new javafx.scene.shape.Rectangle(240, 0.5);
        r.setStyle("-fx-fill:#e5e7eb;");
        return r;
    }

    // ── Nouvelles actions ─────────────────────────────────────────────────

    /** 🌐 Traduction du message */
    private void onTranslate(Message m) {
        // ── Sélecteur de langue ───────────────────────────────────────
        Stage langStage = new Stage();
        langStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        langStage.initOwner(messagesBox.getScene().getWindow());
        langStage.setTitle("Traduire");

        Label title = new Label("🌐  Traduire vers...");
        title.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1a1a2e;");

        // Aperçu du message original
        Label original = new Label("« " + (m.getContent().length() > 60
                ? m.getContent().substring(0, 60) + "…" : m.getContent()) + " »");
        original.setStyle("-fx-font-size:12px; -fx-text-fill:#9ca3af; -fx-font-style:italic;");
        original.setWrapText(true);

        // Boutons de langue
        javafx.scene.layout.FlowPane langButtons = new javafx.scene.layout.FlowPane(8, 8);
        langButtons.setPrefWrapLength(320);

        for (Map.Entry<String, String> lang : TranslationService.LANGUAGES.entrySet()) {
            Button btn = new Button(lang.getKey());
            btn.setStyle(
                "-fx-background-color:#f0f1f8; -fx-text-fill:#6c63ff;" +
                "-fx-background-radius:20; -fx-padding:6 14;" +
                "-fx-font-size:13px; -fx-cursor:hand;");
            btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:#6c63ff; -fx-text-fill:white;" +
                "-fx-background-radius:20; -fx-padding:6 14;" +
                "-fx-font-size:13px; -fx-cursor:hand;"));
            btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color:#f0f1f8; -fx-text-fill:#6c63ff;" +
                "-fx-background-radius:20; -fx-padding:6 14;" +
                "-fx-font-size:13px; -fx-cursor:hand;"));
            btn.setOnAction(e -> {
                langStage.close();
                doTranslate(m, lang.getValue(), lang.getKey());
            });
            langButtons.getChildren().add(btn);
        }

        VBox container = new VBox(14, title, original, langButtons);
        container.setPadding(new Insets(20));
        container.setStyle(
            "-fx-background-color:white; -fx-background-radius:16;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.2),16,0,0,6);");

        javafx.scene.Scene sc = new javafx.scene.Scene(container, 360, 220);
        sc.setFill(javafx.scene.paint.Color.TRANSPARENT);
        langStage.setScene(sc);
        langStage.focusedProperty().addListener((obs, old, f) -> { if (!f) langStage.close(); });

        // Centrer sur l'écran
        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        langStage.setX((screen.getWidth() - 360) / 2);
        langStage.setY((screen.getHeight() - 220) / 2);

        container.setOpacity(0); container.setScaleX(0.85); container.setScaleY(0.85);
        langStage.show();
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(180), container);
        ft.setToValue(1);
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(180), container);
        st.setToX(1); st.setToY(1); st.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        new javafx.animation.ParallelTransition(ft, st).play();
    }

    private void doTranslate(Message m, String langCode, String langName) {
        // Afficher la zone avec indicateur de chargement
        translationResultLabel.setText("");
        translationOriginalLabel.setText("");
        translationStatusLabel.setText("🌐 Traduction en cours...");
        translationStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#7c3aed; -fx-font-style:italic;");
        translationZone.setVisible(true);
        translationZone.setManaged(true);

        // Appel API en arrière-plan
        Thread thread = new Thread(() -> {
            try {
                String translated = translationService.translate(m.getContent(), langCode);
                Platform.runLater(() -> showTranslationResult(m, translated, langName));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    translationStatusLabel.setText("⚠️ Traduction échouée : " + e.getMessage());
                    translationStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ef4444;");
                    translationResultLabel.setText("");
                    translationOriginalLabel.setText("");
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void showTranslationResult(Message original, String translated, String langName) {
        // Mise à jour de la zone dédiée
        translationStatusLabel.setText("🌐 Traduit en " + langName);
        translationStatusLabel.setStyle(
            "-fx-font-size:11px; -fx-text-fill:#7c3aed; -fx-font-weight:bold;");

        translationResultLabel.setText(translated);
        translationResultLabel.setStyle(
            "-fx-font-size:14px; -fx-text-fill:#1a1a2e; -fx-font-weight:bold; -fx-padding:4 0 2 0;");

        String orig = original.getContent().length() > 60
                ? original.getContent().substring(0, 60) + "…"
                : original.getContent();
        translationOriginalLabel.setText("Original : " + orig);
        translationOriginalLabel.setStyle(
            "-fx-font-size:11px; -fx-text-fill:#9ca3af; -fx-font-style:italic;");

        // Animation d'apparition
        translationZone.setOpacity(0);
        javafx.animation.FadeTransition ft =
                new javafx.animation.FadeTransition(Duration.millis(300), translationZone);
        ft.setToValue(1);
        ft.play();
    }

    @FXML
    private void onCloseTranslation() {
        javafx.animation.FadeTransition ft =
                new javafx.animation.FadeTransition(Duration.millis(200), translationZone);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            translationZone.setVisible(false);
            translationZone.setManaged(false);
            translationResultLabel.setText("");
            translationOriginalLabel.setText("");
            translationStatusLabel.setText("");
        });
        ft.play();
    }

    private void onToggleStar(Message m) {
        try {
            messageService.toggleStar(m.getId(), !m.isStarred());
            refreshMessages();
        } catch (Exception e) {
            messagesBox.getChildren().add(emptyState("⚠️ " + e.getMessage()));
        }
    }

    private void onForward(Message m, int me) {
        if (allRooms.isEmpty()) return;
        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(
            allRooms.get(0).goalTitle(),
            allRooms.stream().map(ChatroomListItem::goalTitle).toList());
        dialog.setTitle("Transférer le message");
        dialog.setHeaderText(null);
        dialog.setContentText("Choisir le salon :");
        dialog.showAndWait().ifPresent(chosen ->
            allRooms.stream().filter(it -> it.goalTitle().equals(chosen)).findFirst()
                .ifPresent(target -> {
                    try {
                        messageService.forwardMessage(m.getId(), target.chatroomId(), me);
                        showNotification("Message transféré vers " + chosen);
                    } catch (Exception e) {
                        messagesBox.getChildren().add(emptyState("⚠️ " + e.getMessage()));
                    }
                }));
    }

    private void showMessageInfo(Message m) {
        try {
            int[] stats = messageService.getStats(m.getChatroomId());
            String info = String.format(
                "📅 Envoyé : %s\n✎ Modifié : %s\n📌 Épinglé : %s\n★ Important : %s\n\n" +
                "📊 Stats du salon :\n   Messages : %d\n   Membres actifs : %d",
                m.getCreatedAt() != null ? m.getCreatedAt().format(TIME_SHORT) : "—",
                m.isEdited() ? "Oui" : "Non",
                m.isPinned() ? "Oui" : "Non",
                m.isStarred() ? "Oui" : "Non",
                stats[0], stats[1]);
            Alert a = new Alert(Alert.AlertType.INFORMATION, info);
            a.setTitle("Infos du message"); a.setHeaderText(null); a.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void showNotification(String msg) {
        Label toast = new Label("🔔 " + msg);
        toast.setStyle(
            "-fx-background-color:#1a1a2e; -fx-text-fill:white;" +
            "-fx-background-radius:20; -fx-padding:8 18; -fx-font-size:12px;");
        HBox toastBox = new HBox(toast);
        toastBox.setAlignment(Pos.CENTER);
        messagesBox.getChildren().add(toastBox);
        scrollToBottom();
        new Timeline(new KeyFrame(Duration.seconds(3), e ->
            messagesBox.getChildren().remove(toastBox))).play();
    }

    /** Construit le contenu principal d'une bulle selon le type de message */
    private javafx.scene.Node buildMediaContent(Message m, boolean isMine) {
        String type = m.getAttachmentType();
        String path = m.getAttachmentPath();

        if (path != null && !path.isBlank()) {
            switch (type != null ? type : "") {

                case Message.TYPE_IMAGE: {
                    try {
                        javafx.scene.image.Image img = new javafx.scene.image.Image(
                                "file:" + path, 280, 200, true, true, true);
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                        iv.setFitWidth(280); iv.setPreserveRatio(true);
                        iv.setStyle("-fx-background-radius:10;");
                        return iv;
                    } catch (Exception e) {
                        return fallbackLabel("🖼️ " + m.getAttachmentOriginalName(), isMine);
                    }
                }

                case Message.TYPE_AUDIO: {
                    int dur = m.getAudioDuration();
                    String durStr = dur > 0
                            ? String.format("%d:%02d", dur / 60, dur % 60) : "—";
                    HBox audioRow = new HBox(10);
                    audioRow.setAlignment(Pos.CENTER_LEFT);
                    Label playBtn = new Label("▶");
                    playBtn.setStyle(
                        "-fx-background-color:" + (isMine ? "rgba(255,255,255,0.3)" : "#6c63ff") + ";" +
                        "-fx-text-fill:white; -fx-background-radius:50%;" +
                        "-fx-min-width:36; -fx-min-height:36; -fx-max-width:36; -fx-max-height:36;" +
                        "-fx-alignment:center; -fx-font-size:14px; -fx-cursor:hand;");
                    playBtn.setOnMouseClicked(e -> playAudio(path));
                    Label durLbl = new Label("🎤  " + durStr);
                    durLbl.setStyle("-fx-font-size:12px; -fx-text-fill:" +
                            (isMine ? "rgba(255,255,255,0.8)" : "#374151") + ";");
                    audioRow.getChildren().addAll(playBtn, durLbl);
                    return audioRow;
                }

                case Message.TYPE_VIDEO: {
                    Label videoLbl = new Label("🎥  " + m.getAttachmentOriginalName());
                    videoLbl.setStyle("-fx-font-size:13px; -fx-text-fill:" +
                            (isMine ? "white" : "#374151") + "; -fx-cursor:hand;");
                    videoLbl.setOnMouseClicked(e -> openFile(path));
                    return videoLbl;
                }

                case Message.TYPE_FILE: {
                    HBox fileRow = new HBox(8);
                    fileRow.setAlignment(Pos.CENTER_LEFT);
                    fileRow.setStyle("-fx-cursor:hand;");
                    Label icon = new Label("📎");
                    icon.setStyle("-fx-font-size:18px;");
                    Label nameLbl = new Label(m.getAttachmentOriginalName());
                    nameLbl.setStyle("-fx-font-size:13px; -fx-text-fill:" +
                            (isMine ? "white" : "#374151") + "; -fx-underline:true;");
                    fileRow.getChildren().addAll(icon, nameLbl);
                    fileRow.setOnMouseClicked(e -> openFile(path));
                    return fileRow;
                }
            }
        }

        // Texte par défaut
        Label content = new Label(m.getContent() != null ? m.getContent() : "");
        content.setWrapText(true);
        content.setMaxWidth(400);
        content.getStyleClass().add(isMine ? "bubble-sent-text" : "bubble-received-text");
        return content;
    }

    private Label fallbackLabel(String text, boolean isMine) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px; -fx-text-fill:" + (isMine ? "white" : "#374151") + ";");
        return l;
    }

    private void playAudio(String path) {
        try {
            javafx.scene.media.Media media = new javafx.scene.media.Media(new java.io.File(path).toURI().toString());
            javafx.scene.media.MediaPlayer player = new javafx.scene.media.MediaPlayer(media);
            player.play();
        } catch (Exception e) {
            openFile(path);
        }
    }

    private void openFile(String path) {
        try {
            java.awt.Desktop.getDesktop().open(new java.io.File(path));
        } catch (Exception e) {
            messagesBox.getChildren().add(emptyState("⚠️ Impossible d'ouvrir : " + e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ENVOI MÉDIAS
    // ══════════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════════
    // EMOJI PICKER
    // ══════════════════════════════════════════════════════════════════════

    private static final String[][] EMOJI_CATEGORIES = {
        // Smileys & Emotions (60 emojis)
        {"😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃",
         "😉","😊","😇","🥰","😍","🤩","😘","😗","😚","😙",
         "🥲","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤫",
         "🤔","🤐","🤨","😐","😑","😶","😏","😒","🙄","😬",
         "🤥","😌","😔","😪","🤤","😴","😷","🤒","🤕","🤢",
         "🤮","🤧","🥵","🥶","🥴","😵","🤯","🤠","🥳","🥸"},
        // Gestes & Mains
        {"👍","👎","👌","✌️","🤞","🤟","🤘","🤙","👈","👉",
         "👆","🖕","👇","☝️","👋","🤚","🖐️","✋","🖖","👏",
         "🙌","🤲","🤝","🙏","✍️","💅","🤳","💪","🦾","🦿",
         "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔",
         "❣️","💕","💞","💓","💗","💖","💘","💝","💟","☮️"},
        // Objets & Symboles
        {"🎉","🎊","🎁","🏆","🥇","⭐","🌟","✨","💫","🔥",
         "💯","💢","💥","💦","💨","🕳️","💬","💭","💤","🔔",
         "🎵","🎶","🎸","🎹","🎺","🎻","🥁","🎤","🎧","📱",
         "💻","⌨️","🖥️","🖨️","📷","📸","📹","🎥","📽️","🎬",
         "🌈","☀️","🌙","⭐","🌊","🌸","🌺","🌻","🌹","🍀"},
    };

    @FXML
    private void onEmojiPicker() {
        Stage picker = new Stage();
        picker.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        picker.initOwner(messageField.getScene().getWindow());

        // ── Onglets catégories ────────────────────────────────────────
        String[] catIcons  = {"😀", "👍", "🎉"};
        String[] catLabels = {"Smileys", "Gestes", "Objets"};

        javafx.scene.control.TabPane tabs = new javafx.scene.control.TabPane();
        tabs.setTabClosingPolicy(javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-tab-min-width:80; -fx-tab-max-width:80;");

        for (int c = 0; c < EMOJI_CATEGORIES.length; c++) {
            javafx.scene.layout.TilePane tile = new javafx.scene.layout.TilePane(4, 4);
            tile.setPrefColumns(10);
            tile.setPadding(new Insets(10));
            tile.setStyle("-fx-background-color:#f8f9ff;");

            for (String emoji : EMOJI_CATEGORIES[c]) {
                tile.getChildren().add(emojiButton(emoji, picker));
            }

            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(tile);
            scroll.setFitToWidth(true);
            scroll.setPrefHeight(200);
            scroll.setStyle("-fx-background-color:#f8f9ff; -fx-border-color:transparent;");
            scroll.getStyleClass().add("emoji-scroll");

            javafx.scene.control.Tab tab = new javafx.scene.control.Tab(
                    catIcons[c] + " " + catLabels[c], scroll);
            tabs.getTabs().add(tab);
        }

        // ── Barre de recherche ────────────────────────────────────────
        javafx.scene.control.TextField searchField = new javafx.scene.control.TextField();
        searchField.setPromptText("🔍  Rechercher un emoji...");
        searchField.setStyle(
            "-fx-background-color:#f0f1f8; -fx-background-radius:10;" +
            "-fx-border-color:transparent; -fx-padding:8 14;" +
            "-fx-font-size:13px;");

        javafx.scene.layout.TilePane searchTile = new javafx.scene.layout.TilePane(4, 4);
        searchTile.setPrefColumns(10);
        searchTile.setPadding(new Insets(8));
        searchTile.setVisible(false); searchTile.setManaged(false);

        javafx.scene.control.ScrollPane searchScroll = new javafx.scene.control.ScrollPane(searchTile);
        searchScroll.setFitToWidth(true);
        searchScroll.setPrefHeight(200);
        searchScroll.setStyle("-fx-background-color:#f8f9ff; -fx-border-color:transparent;");
        searchScroll.setVisible(false); searchScroll.setManaged(false);

        searchField.textProperty().addListener((obs, old, q) -> {
            if (q == null || q.isBlank()) {
                searchScroll.setVisible(false); searchScroll.setManaged(false);
                tabs.setVisible(true); tabs.setManaged(true);
                return;
            }
            searchTile.getChildren().clear();
            for (String[] cat : EMOJI_CATEGORIES)
                for (String emoji : cat)
                    searchTile.getChildren().add(emojiButton(emoji, picker));
            tabs.setVisible(false); tabs.setManaged(false);
            searchScroll.setVisible(true); searchScroll.setManaged(true);
        });

        // ── Emojis récents (ligne rapide) ─────────────────────────────
        String[] recent = {"😀","😂","❤️","👍","🔥","🎉","😍","🥰","😭","✨"};
        HBox recentRow = new HBox(4);
        recentRow.setPadding(new Insets(6, 10, 2, 10));
        Label recentLbl = new Label("Récents :");
        recentLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#9ca3af; -fx-padding:6 4 0 0;");
        recentRow.getChildren().add(recentLbl);
        for (String e : recent) recentRow.getChildren().add(emojiButton(e, picker));

        // ── Container principal ───────────────────────────────────────
        VBox container = new VBox(0, searchField, recentRow,
                new javafx.scene.shape.Rectangle(400, 0.5,
                        javafx.scene.paint.Color.web("#e5e7eb")),
                tabs, searchScroll);
        container.setPrefWidth(420);
        container.setStyle(
            "-fx-background-color:white; -fx-background-radius:16;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.2),18,0,0,6);");
        VBox.setMargin(searchField, new Insets(10, 10, 6, 10));
        VBox.setMargin(recentRow, new Insets(0, 0, 4, 0));

        javafx.scene.Scene sc = new javafx.scene.Scene(container);
        sc.setFill(javafx.scene.paint.Color.TRANSPARENT);
        picker.setScene(sc);

        javafx.geometry.Bounds b = messageField.localToScreen(messageField.getBoundsInLocal());
        picker.setX(b.getMinX() - 20);
        picker.setY(b.getMinY() - 310);

        picker.focusedProperty().addListener((obs, old, f) -> { if (!f) picker.close(); });

        container.setOpacity(0); container.setScaleY(0.85);
        picker.show();
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(180), container);
        ft.setToValue(1);
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(180), container);
        st.setToY(1); st.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        new javafx.animation.ParallelTransition(ft, st).play();
    }

    // Mapping emoji → code PNG Twemoji
    private static final java.util.Map<String,String> EMOJI_PNG_MAP = new java.util.HashMap<>();
    static {
        String[][] d = {
            {"😀","1f600"},{"😃","1f603"},{"😄","1f604"},{"😁","1f601"},{"😆","1f606"},
            {"😅","1f605"},{"🤣","1f923"},{"😂","1f602"},{"🙂","1f642"},{"🙃","1f643"},
            {"😉","1f609"},{"😊","1f60a"},{"😇","1f607"},{"🥰","1f970"},{"😍","1f60d"},
            {"🤩","1f929"},{"😘","1f618"},{"😗","1f617"},{"😚","1f61a"},{"😙","1f619"},
            {"🥲","1f972"},{"😋","1f60b"},{"😛","1f61b"},{"😜","1f61c"},{"🤪","1f92a"},
            {"😝","1f61d"},{"🤑","1f911"},{"🤗","1f917"},{"🤭","1f92d"},{"🤫","1f92b"},
            {"🤔","1f914"},{"🤐","1f910"},{"🤨","1f928"},{"😐","1f610"},{"😑","1f611"},
            {"😶","1f636"},{"😏","1f60f"},{"😒","1f612"},{"🙄","1f644"},{"😬","1f62c"},
            {"😮","1f62e"},{"😯","1f62f"},{"😧","1f627"},{"😨","1f628"},{"😰","1f630"},
            {"😥","1f625"},{"😢","1f622"},{"😭","1f62d"},{"😱","1f631"},{"😖","1f616"},
            {"😣","1f623"},{"😞","1f61e"},{"😓","1f613"},{"😩","1f629"},{"😫","1f62b"},
            {"🥱","1f971"},{"😤","1f624"},{"😡","1f621"},{"😠","1f620"},{"🤬","1f92c"},
            {"😈","1f608"},{"👿","1f47f"},{"💀","1f480"},{"💩","1f4a9"},{"🤡","1f921"},
            {"👹","1f479"},{"👺","1f47a"},{"👻","1f47b"},{"👽","1f47d"},
            {"👍","1f44d"},{"👎","1f44e"},{"👌","1f44c"},{"✌️","270c"},{"🤞","1f91e"},
            {"👏","1f44f"},{"🙌","1f64c"},{"🤝","1f91d"},{"🙏","1f64f"},{"💪","1f4aa"},
            {"❤️","2764"},{"🧡","1f9e1"},{"💛","1f49b"},{"💚","1f49a"},{"💙","1f499"},
            {"💜","1f49c"},{"🖤","1f5a4"},{"🤍","1f90d"},{"🤎","1f90e"},{"💔","1f494"},
            {"🎉","1f389"},{"🎊","1f38a"},{"🎁","1f381"},{"🏆","1f3c6"},{"⭐","2b50"},
            {"✨","2728"},{"💯","1f4af"},{"🔥","1f525"},{"💥","1f4a5"},{"🔔","1f514"},
            {"🎵","1f3b5"},{"🎶","1f3b6"},{"📱","1f4f1"},{"💻","1f4bb"},{"📷","1f4f7"},
            {"🌈","1f308"},{"☀️","2600"},{"🌙","1f319"},{"🌸","1f338"},{"🌻","1f33b"}
        };
        for (String[] e : d) EMOJI_PNG_MAP.put(e[0], e[1]);
    }

    /** Crée un bouton emoji avec image PNG Twemoji colorée */
    private StackPane emojiButton(String emoji, Stage picker) {
        StackPane btn = new StackPane();
        btn.setMinSize(40, 40); btn.setMaxSize(40, 40);
        btn.setStyle("-fx-cursor:hand; -fx-background-radius:8;");

        String code = EMOJI_PNG_MAP.get(emoji);
        if (code != null) {
            java.net.URL url = getClass().getResource("/emoji/" + code + ".png");
            if (url != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(url.toExternalForm(), 30, 30, true, true));
                btn.getChildren().add(iv);
            } else {
                Label l = new Label(emoji); l.setStyle("-fx-font-size:22px;");
                btn.getChildren().add(l);
            }
        } else {
            Label l = new Label(emoji); l.setStyle("-fx-font-size:22px;");
            btn.getChildren().add(l);
        }

        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-cursor:hand; -fx-background-color:#ede9fe; -fx-background-radius:8;");
            btn.setScaleX(1.2); btn.setScaleY(1.2);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-cursor:hand; -fx-background-radius:8;");
            btn.setScaleX(1.0); btn.setScaleY(1.0);
        });
        btn.setOnMouseClicked(e -> insertEmoji(emoji, picker));
        return btn;
    }

    /** Insère un emoji à la position du curseur */
    private void insertEmoji(String emoji, Stage picker) {
        int pos = messageField.getCaretPosition();
        String cur = messageField.getText() != null ? messageField.getText() : "";
        messageField.setText(cur.substring(0, pos) + emoji + cur.substring(pos));
        messageField.positionCaret(pos + emoji.length());
        messageField.requestFocus();
        picker.close();
    }

    // ══════════════════════════════════════════════════════════════════════
    // ENREGISTREMENT VOCAL
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void onToggleRecord() {
        if (current == null) { showNotification("Sélectionnez un salon d'abord."); return; }
        int uid = AppSession.getCurrentUser().map(User::getId).orElse(0);
        if (uid <= 0) return;

        if (!audioRecorder.isRecording()) {
            // ── Démarrer l'enregistrement ─────────────────────────────
            if (!AudioRecorderService.isMicAvailable()) {
                showNotification("⚠️ Microphone non disponible.");
                return;
            }
            try {
                audioRecorder.startRecording();
                recordingSeconds = 0;

                // Bouton rouge + timer
                recordBtn.setText("⏹");
                recordBtn.setStyle(
                    "-fx-background-color:#ef4444; -fx-text-fill:white;" +
                    "-fx-background-radius:50%; -fx-font-size:16px;" +
                    "-fx-min-width:36; -fx-min-height:36; -fx-max-width:36; -fx-max-height:36;" +
                    "-fx-cursor:hand;");
                messageField.setPromptText("🔴 Enregistrement en cours...");
                messageField.setDisable(true);

                // Timer affiché dans le prompt
                recordingTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    recordingSeconds++;
                    int m = recordingSeconds / 60, s = recordingSeconds % 60;
                    messageField.setPromptText(String.format("🔴 %d:%02d — Cliquez ⏹ pour arrêter", m, s));
                }));
                recordingTimer.setCycleCount(Timeline.INDEFINITE);
                recordingTimer.play();

            } catch (Exception e) {
                showNotification("⚠️ Erreur microphone : " + e.getMessage());
            }
        } else {
            // ── Arrêter et envoyer ────────────────────────────────────
            if (recordingTimer != null) recordingTimer.stop();
            int duration = audioRecorder.stopRecording();

            // Reset bouton
            recordBtn.setText("🎤");
            recordBtn.setStyle(
                "-fx-background-color:transparent; -fx-font-size:18px;" +
                "-fx-cursor:hand; -fx-opacity:0.6; -fx-padding:0 6;");
            messageField.setPromptText("Your message...");
            messageField.setDisable(false);

            if (duration < 1) { showNotification("Enregistrement trop court."); return; }

            java.io.File audioFile = audioRecorder.getOutputFile();
            if (audioFile == null || !audioFile.exists()) {
                showNotification("⚠️ Fichier audio introuvable.");
                return;
            }

            // Envoi en arrière-plan
            final int dur = duration;
            Thread t = new Thread(() -> {
                try {
                    messageService.sendMedia(uid, current.chatroomId(),
                            audioFile.getAbsolutePath(),
                            Message.TYPE_AUDIO,
                            audioFile.getName(),
                            dur);
                    Platform.runLater(() -> {
                        refreshMessages();
                        chatListView.refresh();
                        showNotification("🎤 Message vocal envoyé (" + dur + "s)");
                    });
                } catch (Exception e) {
                    Platform.runLater(() ->
                        messagesBox.getChildren().add(emptyState("⚠️ " + e.getMessage())));
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    @FXML
    private void onAttachMenu() {
        if (current == null) { showNotification("Sélectionnez un salon d'abord."); return; }

        Stage popup = new Stage();
        popup.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        popup.initOwner(messageField.getScene().getWindow());

        // ── 3 options ─────────────────────────────────────────────────
        String[][] items = {
            {"📎", "Fichier",  "PDF, DOC, ZIP…"},
            {"🖼",  "Image",   "PNG, JPG, GIF…"},
            {"🎥", "Vidéo",   "MP4, AVI, MOV…"},
        };

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setStyle(
            "-fx-background-color:white; -fx-background-radius:20;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),14,0,0,5);");

        for (String[] item : items) {
            VBox btn = new VBox(6);
            btn.setAlignment(Pos.CENTER);
            btn.setMinWidth(72); btn.setMaxWidth(72);
            btn.setStyle("-fx-cursor:hand; -fx-background-radius:14; -fx-padding:10 8;");

            Label icon = new Label(item[0]);
            icon.setStyle("-fx-font-size:26px;");

            Label title = new Label(item[1]);
            title.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#374151;");

            Label sub = new Label(item[2]);
            sub.setStyle("-fx-font-size:10px; -fx-text-fill:#9ca3af;");

            btn.getChildren().addAll(icon, title, sub);

            btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-cursor:hand; -fx-background-color:#f0f1f8; -fx-background-radius:14; -fx-padding:10 8;"));
            btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-cursor:hand; -fx-background-radius:14; -fx-padding:10 8;"));

            String type = item[1].equals("Fichier") ? Message.TYPE_FILE
                        : item[1].equals("Image")   ? Message.TYPE_IMAGE
                        : Message.TYPE_VIDEO;

            btn.setOnMouseClicked(e -> {
                popup.close();
                sendMediaWithChooser(type, extensionFilter(type));
            });
            row.getChildren().add(btn);
        }

        javafx.scene.Scene sc = new javafx.scene.Scene(row);
        sc.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(sc);

        // Position au-dessus du bouton 📎
        javafx.geometry.Bounds b = messageField.localToScreen(messageField.getBoundsInLocal());
        popup.setX(b.getMinX() - 40);
        popup.setY(b.getMinY() - 130);

        popup.focusedProperty().addListener((obs, old, f) -> { if (!f) popup.close(); });

        row.setOpacity(0); row.setScaleY(0.8);
        popup.show();
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(150), row);
        ft.setToValue(1);
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(150), row);
        st.setToY(1); st.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        new javafx.animation.ParallelTransition(ft, st).play();
    }

    private javafx.stage.FileChooser.ExtensionFilter extensionFilter(String type) {
        return switch (type) {
            case Message.TYPE_IMAGE -> new javafx.stage.FileChooser.ExtensionFilter(
                    "Images", "*.png","*.jpg","*.jpeg","*.gif","*.webp");
            case Message.TYPE_VIDEO -> new javafx.stage.FileChooser.ExtensionFilter(
                    "Vidéos", "*.mp4","*.avi","*.mov","*.mkv","*.webm");
            default -> new javafx.stage.FileChooser.ExtensionFilter(
                    "Fichiers", "*.pdf","*.doc","*.docx","*.zip","*.txt","*.*");
        };
    }

    private void sendMediaWithChooser(String type, javafx.stage.FileChooser.ExtensionFilter filter) {
        if (current == null) { showNotification("Sélectionnez un salon d'abord."); return; }
        int uid = AppSession.getCurrentUser().map(User::getId).orElse(0);
        if (uid <= 0) return;

        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Choisir un fichier");
        chooser.getExtensionFilters().add(filter);
        java.io.File file = chooser.showOpenDialog(messagesBox.getScene().getWindow());
        if (file == null) return;

        // Envoi en arrière-plan
        Thread t = new Thread(() -> {
            try {
                messageService.sendMedia(uid, current.chatroomId(),
                        file.getAbsolutePath(), type, file.getName(), 0);
                Platform.runLater(() -> {
                    refreshMessages();
                    chatListView.refresh();
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    messagesBox.getChildren().add(emptyState("⚠️ " + e.getMessage())));
            }
        });
        t.setDaemon(true);
        t.start();
        showNotification("Envoi en cours...");
    }
    private HBox buildReactionsRow(Message m, int me) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 0, 0));

        // Couleurs par emoji
        java.util.Map<String, String[]> emojiStyle = new java.util.HashMap<>();
        emojiStyle.put("❤️", new String[]{"❤", "#fee2e2", "#ef4444"});
        emojiStyle.put("😂", new String[]{"😄", "#fef9c3", "#ca8a04"});
        emojiStyle.put("😮", new String[]{"😮", "#fef3c7", "#d97706"});
        emojiStyle.put("😢", new String[]{"😢", "#dbeafe", "#3b82f6"});
        emojiStyle.put("🔥", new String[]{"🔥", "#ffedd5", "#ea580c"});
        emojiStyle.put("👍", new String[]{"👍", "#ede9fe", "#6c63ff"});

        try {
            Map<String, Integer> counts = reactionService.countByMessage(m.getId());
            String myReaction = reactionService.getUserReaction(m.getId(), me);

            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                boolean isMine = entry.getKey().equals(myReaction);
                String[] style = emojiStyle.getOrDefault(entry.getKey(),
                        new String[]{entry.getKey(), "#f0f1f8", "#6c63ff"});

                String bg   = isMine ? "white"              : "rgba(255,255,255,0.25)";
                String fg   = isMine ? style[2]             : "white";
                String border = isMine ? style[2]           : "rgba(255,255,255,0.4)";
                String sym  = style[0];

                Label badge = new Label(sym + "  " + entry.getValue());
                badge.setStyle(
                    "-fx-background-color:" + bg + ";" +
                    "-fx-text-fill:" + fg + ";" +
                    "-fx-border-color:" + border + "; -fx-border-radius:20; -fx-border-width:1;" +
                    "-fx-background-radius:20; -fx-padding:4 12;" +
                    "-fx-font-size:12px; -fx-font-weight:bold; -fx-cursor:hand;");

                badge.setOnMouseClicked(e -> {
                    if (isMine) onRemoveReaction(m.getId(), me);
                    else onReact(m.getId(), me, entry.getKey());
                });
                row.getChildren().add(badge);
            }

            // Bouton + pour ajouter
            Label addBtn = new Label("☺ +");
            addBtn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.25);" +
                "-fx-background-radius:20; -fx-padding:5 12;" +
                "-fx-font-size:13px; -fx-text-fill:white;" +
                "-fx-border-color:rgba(255,255,255,0.4); -fx-border-radius:20; -fx-border-width:1;" +
                "-fx-cursor:hand;");
            addBtn.setOnMouseEntered(e -> addBtn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.45);" +
                "-fx-background-radius:20; -fx-padding:5 12;" +
                "-fx-font-size:13px; -fx-text-fill:white;" +
                "-fx-border-color:white; -fx-border-radius:20; -fx-border-width:1;" +
                "-fx-cursor:hand;"));
            addBtn.setOnMouseExited(e -> addBtn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.25);" +
                "-fx-background-radius:20; -fx-padding:5 12;" +
                "-fx-font-size:13px; -fx-text-fill:white;" +
                "-fx-border-color:rgba(255,255,255,0.4); -fx-border-radius:20; -fx-border-width:1;" +
                "-fx-cursor:hand;"));
            addBtn.setOnMouseClicked(e -> showEmojiPicker(m, me, addBtn));
            row.getChildren().add(addBtn);

        } catch (Exception ignored) {}
        return row;
    }

    /** Affiche le picker d'emojis style Facebook sous la bulle */
    private void showEmojiPicker(Message m, int me, javafx.scene.Node anchor) {
        Stage pickerStage = new Stage();
        pickerStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        pickerStage.initOwner(anchor.getScene().getWindow());

        Label hint = new Label("Tap to react");
        hint.setStyle("-fx-font-size:11px; -fx-text-fill:#9ca3af; -fx-padding:0 0 6 0;");

        // Emoji → [symbole, couleur fond, couleur texte]
        String[][] reactions = {
            {"❤",  "#fee2e2", "#ef4444"},   // rouge
            {"�", "#fef9c3", "#ca8a04"},   // jaune
            {"😮", "#fef3c7", "#d97706"},   // orange clair
            {"😢", "#dbeafe", "#3b82f6"},   // bleu
            {"🔥", "#ffedd5", "#ea580c"},   // orange
            {"👍", "#ede9fe", "#6c63ff"},   // violet
        };
        // Emojis réels correspondants pour la BD
        String[] emojiKeys = {"❤️", "�", "😮", "😢", "🔥", "👍"};

        HBox emojisRow = new HBox(6);
        emojisRow.setAlignment(javafx.geometry.Pos.CENTER);

        for (int i = 0; i < reactions.length; i++) {
            String symbol    = reactions[i][0];
            String bgColor   = reactions[i][1];
            String textColor = reactions[i][2];
            String emojiKey  = emojiKeys[i];

            StackPane btn = new StackPane();
            btn.setMinSize(46, 46); btn.setMaxSize(46, 46);
            btn.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                "-fx-background-radius:50%; -fx-cursor:hand;");

            Label lbl = new Label(symbol);
            lbl.setStyle(
                "-fx-font-size:20px; -fx-font-weight:bold;" +
                "-fx-text-fill:" + textColor + ";");
            btn.getChildren().add(lbl);

            btn.setOnMouseEntered(e -> {
                btn.setScaleX(1.25); btn.setScaleY(1.25);
                btn.setStyle(
                    "-fx-background-color:" + textColor + ";" +
                    "-fx-background-radius:50%; -fx-cursor:hand;");
                lbl.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:white;");
            });
            btn.setOnMouseExited(e -> {
                btn.setScaleX(1.0); btn.setScaleY(1.0);
                btn.setStyle(
                    "-fx-background-color:" + bgColor + ";" +
                    "-fx-background-radius:50%; -fx-cursor:hand;");
                lbl.setStyle(
                    "-fx-font-size:20px; -fx-font-weight:bold;" +
                    "-fx-text-fill:" + textColor + ";");
            });
            btn.setOnMouseClicked(e -> {
                pickerStage.close();
                onReact(m.getId(), me, emojiKey);
            });
            emojisRow.getChildren().add(btn);
        }

        // Bouton +
        StackPane plusBtn = new StackPane();
        plusBtn.setMinSize(46, 46); plusBtn.setMaxSize(46, 46);
        plusBtn.setStyle("-fx-background-color:#f0f1f8; -fx-background-radius:50%; -fx-cursor:hand;");
        Label plusLbl = new Label("+");
        plusLbl.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#6c63ff;");
        plusBtn.getChildren().add(plusLbl);
        plusBtn.setOnMouseEntered(e -> plusBtn.setStyle("-fx-background-color:#ede9fe; -fx-background-radius:50%; -fx-cursor:hand;"));
        plusBtn.setOnMouseExited(e -> plusBtn.setStyle("-fx-background-color:#f0f1f8; -fx-background-radius:50%; -fx-cursor:hand;"));
        emojisRow.getChildren().add(plusBtn);

        VBox container = new VBox(6, hint, emojisRow);
        container.setAlignment(javafx.geometry.Pos.CENTER);
        container.setPadding(new Insets(14, 18, 14, 18));
        container.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:32;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.20),18,0,0,6);");

        javafx.scene.Scene ps = new javafx.scene.Scene(container);
        ps.setFill(javafx.scene.paint.Color.TRANSPARENT);
        pickerStage.setScene(ps);

        javafx.geometry.Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        pickerStage.setX(bounds.getMinX() - 80);
        pickerStage.setY(bounds.getMinY() - 100);

        pickerStage.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) pickerStage.close();
        });

        container.setOpacity(0);
        container.setScaleX(0.75); container.setScaleY(0.75);
        pickerStage.show();

        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(180), container);
        ft.setToValue(1);
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(180), container);
        st.setToX(1); st.setToY(1);
        st.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        new javafx.animation.ParallelTransition(ft, st).play();
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

package controllers.chatroom;

import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ChatroomHubController {

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

        Integer uid = AppSession.getCurrentUser().map(User::getId).orElse(null);
        if (uid == null) {
            headerTitleLabel.setText("Non connecté");
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
        }));
        messagePollTimeline.setCycleCount(Timeline.INDEFINITE);
        messagePollTimeline.play();
    }

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

            boolean open = chatroomService.findById(item.chatroomId())
                    .map(c -> "active".equalsIgnoreCase(c.getState()))
                    .orElse(false);
            messageField.setDisable(!open);
            messageField.setPromptText(open ? "Écrire un message…" : "Salon verrouillé ou archivé — envoi désactivé.");
        } catch (SQLException e) {
            headerSubLabel.setText("");
            messageField.setDisable(true);
        }
        refreshMessages();
    }

    private void refreshMessages() {
        messagesBox.getChildren().clear();
        if (current == null) {
            messagesBox.getChildren().add(new Label("Sélectionnez un chat."));
            return;
        }
        try {
            List<Message> msgs = messageService.findByChatroomId(current.chatroomId());
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
        }
        scrollMessagesToBottom();
    }

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
            new Alert(Alert.AlertType.WARNING, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onPendingRequests() {
        if (current == null) {
            return;
        }
        int goalId = current.goalId();
        try {
            List<GoalParticipation> pending = participationService.findPendingByGoal(goalId);
            if (pending.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Aucune demande en attente.").showAndWait();
                return;
            }
            ButtonType accept = new ButtonType("Accepter");
            ButtonType refuse = new ButtonType("Refuser");
            for (GoalParticipation gp : pending) {
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
                } catch (SQLException ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            }
            if (current != null) {
                selectRoom(current);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

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
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onDeleteRoom() {
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
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }
}

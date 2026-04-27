package controllers.account;

import controllers.navigation.NavigationManager;
import enums.PostStatus;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.interaction.Post;
import model.profile.ProfileAnalysisResult;
import model.profile.AiArchetypeProfile;
import model.profile.OnboardingAnswers;
import model.user.User;
import services.account.AccountSecurityService;
import services.account.IpGeolocationService;
import services.account.UserService;
import services.chatroom.ChatroomService;
import services.chatroom.ChatroomService.ChatroomListItem;
import services.interaction.PostService;
import services.interaction.PostService.PostWithStats;
import services.interaction.SavedPostService;
import services.profile.ProfileAnalyzerService;
import services.profile.AiProfileGeneratorService;
import services.profile.UserAiProfileStorageService;
import session.AppSession;
import session.ChatroomNav;
import utils.HtmlPlainText;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Page profil utilisateur : infos, statistiques posts, onglets activités (sans bloc « IA / visionary »).
 */
public class UserProfileController {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private final UserService userService = new UserService();
    private final PostService postService = new PostService();
    private final SavedPostService savedPostService = new SavedPostService();
    private final ChatroomService chatroomService = new ChatroomService();
    private final ProfileAnalyzerService profileAnalyzerService = new ProfileAnalyzerService();
    private final AiProfileGeneratorService aiProfileGeneratorService = new AiProfileGeneratorService();
    private final UserAiProfileStorageService userAiProfileStorageService = new UserAiProfileStorageService();
    private final AccountSecurityService accountSecurityService = new AccountSecurityService();
    private final IpGeolocationService ipGeolocationService = new IpGeolocationService();

    private final ToggleGroup tabGroup = new ToggleGroup();
    private int userId;
    private User loadedUser;

    @FXML
    private Label heroAvatarLabel;
    @FXML
    private Label heroNameLabel;
    @FXML
    private Label heroEmailLabel;
    @FXML
    private Label sideEmailLabel;
    @FXML
    private Label sideNameLabel;
    @FXML
    private Label sidePhoneLabel;
    @FXML
    private Label sideAgeLabel;
    @FXML
    private Label sideMemberLabel;
    @FXML
    private TextField editFirstNameField;
    @FXML
    private TextField editLastNameField;
    @FXML
    private TextField editPhoneField;
    @FXML
    private TextField editAgeField;
    @FXML
    private Button editPersonalInfoButton;
    @FXML
    private Button savePersonalInfoButton;
    @FXML
    private Label statPublishedLabel;
    @FXML
    private Label statSavedLabel;
    @FXML
    private Label statDraftsLabel;
    @FXML
    private Label statScheduledLabel;
    @FXML
    private ToggleButton tabPosts;
    @FXML
    private ToggleButton tabSaved;
    @FXML
    private ToggleButton tabScheduled;
    @FXML
    private ToggleButton tabDrafts;
    @FXML
    private ToggleButton tabChatrooms;
    @FXML
    private ToggleButton tabLoginSecurity;
    @FXML
    private Hyperlink openFeedLink;
    @FXML
    private VBox contentBox;
    @FXML
    private Button runAnalysisButton;
    @FXML
    private Label aiSummaryLabel;
    @FXML
    private Label archetypeNameLabel;
    @FXML
    private Label archetypeDescriptionLabel;
    @FXML
    private Label securityStatusLabel;
    @FXML
    private VBox activeSessionsBox;
    @FXML
    private VBox loginHistoryBox;
    @FXML
    private Button logoutAllDevicesButton;
    @FXML
    private VBox loginSecuritySection;

    @FXML
    private void initialize() {
        Optional<User> session = AppSession.getCurrentUser();
        if (session.isEmpty() || session.get().getId() == null) {
            contentBox.getChildren().setAll(new Label("Veuillez vous connecter pour voir votre profil."));
            return;
        }
        userId = session.get().getId();

        for (ToggleButton b : List.of(tabPosts, tabSaved, tabScheduled, tabDrafts, tabChatrooms, tabLoginSecurity)) {
            b.setToggleGroup(tabGroup);
        }
        tabGroup.selectedToggleProperty().addListener((obs, prev, sel) -> {
            if (sel == null) {
                tabPosts.setSelected(true);
                return;
            }
            refreshTabContent();
        });

        openFeedLink.setOnAction(e -> onOpenFeed());
        runAnalysisButton.setOnAction(e -> onRunProfileAnalysis());
        editPersonalInfoButton.setOnAction(e -> onEditPersonalInfo());
        savePersonalInfoButton.setOnAction(e -> onSavePersonalInfo());
        logoutAllDevicesButton.setOnAction(e -> onLogoutAllDevices());

        loadUserHeader();
        refreshStatsAndTabs();
        loadSecurityCards();
        setLoginSecuritySectionVisible(false);
        loadSavedAiProfile();
        tabPosts.setSelected(true);
    }

    private void loadSavedAiProfile() {
        try {
            Optional<UserAiProfileStorageService.StoredAiProfile> saved = userAiProfileStorageService.findByUserId(userId);
            if (saved.isEmpty()) {
                return;
            }
            UserAiProfileStorageService.StoredAiProfile data = saved.get();
            renderArchetype(data.profile());
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Cannot load saved AI profile: " + e.getMessage()).showAndWait();
        }
    }

    private void renderArchetype(AiArchetypeProfile profile) {
        archetypeNameLabel.setText(profile.getArchetypeName());
        archetypeDescriptionLabel.setText(profile.getDescription());
        if (profile.getDescription() == null || profile.getDescription().isBlank()) {
            archetypeDescriptionLabel.setText(profile.getShortBio() == null ? "" : profile.getShortBio());
        }
    }

    private void onRunProfileAnalysis() {
        try {
            Optional<OnboardingAnswers> answersOpt = askOnboardingAnswers();
            if (answersOpt.isEmpty()) {
                return;
            }
            ProfileAnalysisResult result = profileAnalyzerService.analyzeCurrentUserProfile(userId);
            AiArchetypeProfile archetype = aiProfileGeneratorService.generateProfile(answersOpt.get());
            userAiProfileStorageService.saveOrUpdate(userId, answersOpt.get(), archetype);
            renderArchetype(archetype);
            String firstReco = result.getRecommendations().isEmpty() ? "No recommendation." : result.getRecommendations().getFirst();
            aiSummaryLabel.setText("Score " + result.getScore() + "/100 - " + firstReco);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Cannot analyze profile: " + e.getMessage()).showAndWait();
        }
    }

    private Optional<OnboardingAnswers> askOnboardingAnswers() throws SQLException {
        Optional<UserAiProfileStorageService.StoredAiProfile> saved = userAiProfileStorageService.findByUserId(userId);
        OnboardingAnswers defaults = saved.map(UserAiProfileStorageService.StoredAiProfile::answers)
                .orElse(new OnboardingAnswers("", "", "", "", ""));

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("AI Profile Form");
        dialog.setHeaderText("Answer quickly to generate your archetype.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        TextArea goals = new TextArea(defaults.goals());
        TextArea challenges = new TextArea(defaults.challenges());
        TextArea motivation = new TextArea(defaults.motivationStyle());
        TextArea planning = new TextArea(defaults.planningStyle());
        TextArea interests = new TextArea(defaults.interests());

        goals.setPromptText("Goals");
        challenges.setPromptText("Challenges");
        motivation.setPromptText("Motivation style");
        planning.setPromptText("Planning style");
        interests.setPromptText("Interests");

        goals.setPrefRowCount(2);
        challenges.setPrefRowCount(2);
        motivation.setPrefRowCount(1);
        planning.setPrefRowCount(1);
        interests.setPrefRowCount(2);

        grid.add(new Label("Goals"), 0, 0);
        grid.add(goals, 1, 0);
        grid.add(new Label("Challenges"), 0, 1);
        grid.add(challenges, 1, 1);
        grid.add(new Label("Motivation"), 0, 2);
        grid.add(motivation, 1, 2);
        grid.add(new Label("Planning"), 0, 3);
        grid.add(planning, 1, 3);
        grid.add(new Label("Interests"), 0, 4);
        grid.add(interests, 1, 4);

        dialog.getDialogPane().setContent(grid);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return Optional.empty();
        }
        return Optional.of(new OnboardingAnswers(
                goals.getText(),
                challenges.getText(),
                motivation.getText(),
                planning.getText(),
                interests.getText()
        ));
    }

    private void loadUserHeader() {
        try {
            AppSession.getSessionToken().ifPresent(token -> {
                try {
                    accountSecurityService.touchSession(userId, token);
                } catch (SQLException ignored) {
                }
            });
            Optional<User> u = userService.findById(userId);
            if (u.isEmpty()) {
                heroNameLabel.setText("Utilisateur introuvable");
                return;
            }
            User user = u.get();
            loadedUser = user;
            heroAvatarLabel.setText(initials(user.getFirstName(), user.getLastName()));
            heroNameLabel.setText(fullName(user));
            heroEmailLabel.setText(user.getEmail() != null ? user.getEmail() : "");

            sideNameLabel.setText("NOM : " + fullName(user));
            sideEmailLabel.setText("EMAIL : " + (user.getEmail() != null ? user.getEmail() : "—"));
            sidePhoneLabel.setText("TEL : " + ((user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank())
                    ? user.getPhoneNumber()
                    : "—"));
            if (user.getAge() != null) {
                sideAgeLabel.setText("ÂGE : " + user.getAge() + " ans");
            } else {
                sideAgeLabel.setText("ÂGE : —");
            }
            if (user.getCreatedAt() != null) {
                sideMemberLabel.setText("MEMBRE DEPUIS : " + user.getCreatedAt().format(DF));
            } else {
                sideMemberLabel.setText("MEMBRE DEPUIS : —");
            }
            setPersonalInfoEditMode(false);
        } catch (SQLException e) {
            heroNameLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void onEditPersonalInfo() {
        setPersonalInfoEditMode(true);
    }

    private void onSavePersonalInfo() {
        savePersonalInfo();
    }

    private void setPersonalInfoEditMode(boolean editMode) {
        editFirstNameField.setVisible(editMode);
        editFirstNameField.setManaged(editMode);
        editLastNameField.setVisible(editMode);
        editLastNameField.setManaged(editMode);
        editPhoneField.setVisible(editMode);
        editPhoneField.setManaged(editMode);
        editAgeField.setVisible(editMode);
        editAgeField.setManaged(editMode);

        sideNameLabel.setVisible(!editMode);
        sideNameLabel.setManaged(!editMode);
        sidePhoneLabel.setVisible(!editMode);
        sidePhoneLabel.setManaged(!editMode);
        sideAgeLabel.setVisible(!editMode);
        sideAgeLabel.setManaged(!editMode);

        savePersonalInfoButton.setVisible(editMode);
        savePersonalInfoButton.setManaged(editMode);
        if (editMode && loadedUser != null) {
            editFirstNameField.setText(loadedUser.getFirstName() == null ? "" : loadedUser.getFirstName());
            editLastNameField.setText(loadedUser.getLastName() == null ? "" : loadedUser.getLastName());
            editPhoneField.setText(loadedUser.getPhoneNumber() == null ? "" : loadedUser.getPhoneNumber());
            editAgeField.setText(loadedUser.getAge() == null ? "" : String.valueOf(loadedUser.getAge()));
        }
    }

    private void savePersonalInfo() {
        if (loadedUser == null || loadedUser.getId() == null) {
            return;
        }
        try {
            String firstName = editFirstNameField.getText() == null ? "" : editFirstNameField.getText().trim();
            String lastName = editLastNameField.getText() == null ? "" : editLastNameField.getText().trim();
            String phone = editPhoneField.getText() == null ? "" : editPhoneField.getText().trim();
            String ageText = editAgeField.getText() == null ? "" : editAgeField.getText().trim();
            if (firstName.isBlank() || lastName.isBlank()) {
                throw new IllegalArgumentException("First name and last name are required.");
            }
            Integer age = null;
            if (!ageText.isBlank()) {
                age = Integer.parseInt(ageText);
                if (age < 13 || age > 120) {
                    throw new IllegalArgumentException("Age must be between 13 and 120.");
                }
            }

            loadedUser.setFirstName(firstName);
            loadedUser.setLastName(lastName);
            loadedUser.setPhoneNumber(phone.isBlank() ? null : phone);
            loadedUser.setAge(age);
            userService.update(loadedUser);
            loadUserHeader();
            aiSummaryLabel.setText("Personal info updated.");
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Age must be a valid number.").showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Cannot update personal info: " + e.getMessage()).showAndWait();
        }
    }

    private void refreshStatsAndTabs() {
        try {
            int pub = postService.countPostsByAuthorAndStatus(userId, PostStatus.PUBLISHED);
            int draft = postService.countPostsByAuthorAndStatus(userId, PostStatus.DRAFT);
            int sched = postService.countPostsByAuthorAndStatus(userId, PostStatus.SCHEDULED);
            int saved = savedPostService.findByUserId(Integer.valueOf(userId)).size();
            int rooms = chatroomService.findAccessibleForUser(userId).size();

            statPublishedLabel.setText(String.valueOf(pub));
            statSavedLabel.setText(String.valueOf(saved));
            statDraftsLabel.setText(String.valueOf(draft));
            statScheduledLabel.setText(String.valueOf(sched));

            tabPosts.setText("Mes posts (" + pub + ")");
            tabSaved.setText("Sauvegardés (" + saved + ")");
            tabScheduled.setText("Planifiés (" + sched + ")");
            tabDrafts.setText("Brouillons (" + draft + ")");
            tabChatrooms.setText("Chatrooms (" + rooms + ")");
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void loadSecurityCards() {
        activeSessionsBox.getChildren().clear();
        loginHistoryBox.getChildren().clear();
        try {
            String currentSession = AppSession.getSessionToken().orElse("");
            List<AccountSecurityService.ActiveSessionView> sessions = accountSecurityService.findActiveSessions(userId, currentSession);
            List<AccountSecurityService.LoginHistoryView> logins = accountSecurityService.findRecentLogins(userId);

            long suspiciousCount = logins.stream().filter(AccountSecurityService.LoginHistoryView::suspicious).count();
            if (suspiciousCount > 0) {
                securityStatusLabel.setText("Security alert: " + suspiciousCount + " suspicious login(s) found.");
            } else {
                securityStatusLabel.setText("Your account looks safe. No suspicious login detected.");
            }

            if (sessions.isEmpty()) {
                activeSessionsBox.getChildren().add(new Label("No active session found."));
            } else {
                for (AccountSecurityService.ActiveSessionView s : sessions) {
                    activeSessionsBox.getChildren().add(buildSessionCard(s));
                }
            }

            if (logins.isEmpty()) {
                loginHistoryBox.getChildren().add(new Label("No login history yet."));
            } else {
                for (AccountSecurityService.LoginHistoryView h : logins) {
                    loginHistoryBox.getChildren().add(buildLoginEventCard(h));
                }
            }
        } catch (SQLException e) {
            securityStatusLabel.setText("Cannot load security insights: " + e.getMessage());
        }
    }

    private VBox buildSessionCard(AccountSecurityService.ActiveSessionView sessionView) {
        VBox card = new VBox(4);
        card.getStyleClass().add("profile-security-item");
        Label device = new Label((sessionView.currentSession() ? "This device • " : "") + sessionView.deviceLabel());
        device.getStyleClass().add("profile-security-item-title");
        Label details = new Label("Connected: " + sessionView.connectedAt() + "  •  Last seen: " + sessionView.lastSeenAt());
        details.getStyleClass().add("profile-security-item-subtitle");
        card.getChildren().addAll(device, details);
        return card;
    }

    private VBox buildLoginEventCard(AccountSecurityService.LoginHistoryView historyView) {
        VBox card = new VBox(4);
        card.getStyleClass().add("profile-security-item");
        String status = historyView.success() ? "Login success" : "Login failed";
        if (historyView.suspicious()) {
            status += " • Suspicious";
        }
        Label title = new Label(status + " • " + historyView.attemptedAt());
        title.getStyleClass().add("profile-security-item-title");
        String reason = historyView.suspiciousReason() == null ? "" : (" • " + historyView.suspiciousReason());
        IpGeolocationService.GeoInfo geo = ipGeolocationService.resolve(historyView.ipAddress());
        String ipPart = (geo.ipAddress() == null || geo.ipAddress().isBlank()) ? "" : (" • IP: " + geo.ipAddress());
        String locationPart = (geo.locationLabel() == null || geo.locationLabel().isBlank()) ? "" : (" • " + geo.locationLabel());
        Label subtitle = new Label(historyView.deviceLabel() + ipPart + locationPart + reason);
        subtitle.getStyleClass().add("profile-security-item-subtitle");
        card.getChildren().addAll(title, subtitle);
        return card;
    }

    private void onLogoutAllDevices() {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "This will end all sessions on all devices. Continue?",
                ButtonType.YES,
                ButtonType.NO
        );
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }
        try {
            int revoked = accountSecurityService.logoutAllDevices(userId);
            AppSession.clear();
            new Alert(Alert.AlertType.INFORMATION, "Done. " + revoked + " session(s) revoked. Please login again.").showAndWait();
            AuthNavigation.showLogin();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Cannot revoke sessions: " + e.getMessage()).showAndWait();
        }
    }

    private void refreshTabContent() {
        contentBox.getChildren().clear();
        ToggleButton sel = (ToggleButton) tabGroup.getSelectedToggle();
        if (sel == null) {
            return;
        }
        setLoginSecuritySectionVisible(sel == tabLoginSecurity);
        try {
            if (sel == tabPosts) {
                fillPostCards(postService.findProfilePostsByAuthorAndStatus(userId, PostStatus.PUBLISHED));
            } else if (sel == tabSaved) {
                fillPostCards(postService.findProfileSavedPosts(userId));
            } else if (sel == tabScheduled) {
                fillPostCards(postService.findProfilePostsByAuthorAndStatus(userId, PostStatus.SCHEDULED));
            } else if (sel == tabDrafts) {
                fillDraftCards(postService.findProfilePostsByAuthorAndStatus(userId, PostStatus.DRAFT));
            } else if (sel == tabChatrooms) {
                fillChatroomCards(chatroomService.findAccessibleForUser(userId));
            } else if (sel == tabLoginSecurity) {
                loadSecurityCards();
            }
        } catch (SQLException e) {
            contentBox.getChildren().add(new Label("Erreur : " + e.getMessage()));
        }
    }

    private void setLoginSecuritySectionVisible(boolean visible) {
        loginSecuritySection.setVisible(visible);
        loginSecuritySection.setManaged(visible);
    }

    private void fillPostCards(List<PostWithStats> rows) {
        if (rows.isEmpty()) {
            contentBox.getChildren().add(new Label("Aucun élément dans cette catégorie."));
            return;
        }
        for (PostWithStats row : rows) {
            contentBox.getChildren().add(buildPostCard(row));
        }
    }

    private void fillDraftCards(List<PostWithStats> rows) {
        if (rows.isEmpty()) {
            contentBox.getChildren().add(new Label("Aucun brouillon pour le moment."));
            return;
        }
        for (PostWithStats row : rows) {
            contentBox.getChildren().add(buildDraftCard(row));
        }
    }

    private VBox buildPostCard(PostWithStats row) {
        Post p = row.post();
        VBox card = new VBox(8);
        card.getStyleClass().add("profile-post-card");

        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(p.getTitle() != null ? p.getTitle() : "—");
        title.getStyleClass().add("profile-post-title");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Label badge = new Label(statusFr(p.getStatus()));
        badge.getStyleClass().add(statusBadgeClass(p.getStatus()));
        head.getChildren().addAll(title, grow, badge);

        String body = HtmlPlainText.toPlain(p.getContent());
        if (body.length() > 200) {
            body = body.substring(0, 197) + "…";
        }
        Label content = new Label(body);
        content.setWrapText(true);
        content.getStyleClass().add("profile-post-body");

        String dateStr = p.getCreatedAt() != null ? p.getCreatedAt().format(DF) : "—";
        HBox foot = new HBox(20);
        foot.getStyleClass().add("profile-post-footer");
        foot.setAlignment(Pos.CENTER_LEFT);
        foot.getChildren().addAll(
                new Label("📅  " + dateStr),
                new Label("❤️  " + row.likeCount()),
                new Label("💬  " + row.commentCount()));

        card.getChildren().addAll(head, content, foot);
        return card;
    }

    private VBox buildDraftCard(PostWithStats row) {
        Post p = row.post();
        VBox card = buildPostCard(row);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button openEditBtn = new Button("Ouvrir / Modifier");
        openEditBtn.getStyleClass().add("profile-draft-edit-btn");
        openEditBtn.setOnAction(e -> openDraftEditor(p));

        Button publishBtn = new Button("Publier");
        publishBtn.getStyleClass().add("profile-draft-publish-btn");
        publishBtn.setOnAction(e -> publishDraft(p));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().add("profile-draft-delete-btn");
        deleteBtn.setOnAction(e -> deleteDraft(p));

        actions.getChildren().addAll(openEditBtn, publishBtn, deleteBtn);
        card.getChildren().add(actions);
        return card;
    }

    private void openDraftEditor(Post draft) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le brouillon");
        dialog.setHeaderText("Brouillon #" + draft.getId());
        ButtonType saveType = new ButtonType("Enregistrer");
        ButtonType publishType = new ButtonType("Publier");
        ButtonType deleteType = new ButtonType("Supprimer");
        dialog.getDialogPane().getButtonTypes().addAll(saveType, publishType, deleteType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField titleField = new TextField(draft.getTitle() == null ? "" : draft.getTitle());
        titleField.setPromptText("Titre");
        TextArea contentArea = new TextArea(draft.getContent() == null ? "" : HtmlPlainText.toPlain(draft.getContent()));
        contentArea.setPromptText("Contenu");
        contentArea.setPrefRowCount(8);
        contentArea.setWrapText(true);

        grid.add(new Label("Titre"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Contenu"), 0, 1);
        grid.add(contentArea, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
            return;
        }
        if (result.get() == deleteType) {
            deleteDraft(draft);
            return;
        }

        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String content = contentArea.getText() == null ? "" : contentArea.getText().trim();
        if (title.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Le titre est obligatoire.").showAndWait();
            return;
        }

        draft.setTitle(title);
        draft.setContent(content.isBlank() ? null : content);
        draft.setUpdatedAt(LocalDateTime.now());

        if (result.get() == publishType) {
            publishDraft(draft);
        } else {
            saveDraft(draft);
        }
    }

    private void saveDraft(Post draft) {
        try {
            draft.setStatus(PostStatus.DRAFT);
            draft.setScheduledAt(null);
            postService.updatePost(draft);
            refreshStatsAndTabs();
            refreshTabContent();
            new Alert(Alert.AlertType.INFORMATION, "Brouillon mis à jour.").showAndWait();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Sauvegarde brouillon impossible : " + e.getMessage()).showAndWait();
        }
    }

    private void publishDraft(Post draft) {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Publier ce brouillon maintenant ?",
                ButtonType.YES,
                ButtonType.NO
        );
        Optional<ButtonType> confirmResult = confirm.showAndWait();
        if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.YES) {
            return;
        }
        try {
            draft.setStatus(PostStatus.PUBLISHED);
            draft.setScheduledAt(null);
            draft.setUpdatedAt(LocalDateTime.now());
            postService.updatePost(draft);
            refreshStatsAndTabs();
            refreshTabContent();
            new Alert(Alert.AlertType.INFORMATION, "Brouillon publié avec succès.").showAndWait();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Publication impossible : " + e.getMessage()).showAndWait();
        }
    }

    private void deleteDraft(Post draft) {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Supprimer définitivement ce brouillon ?",
                ButtonType.YES,
                ButtonType.NO
        );
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }
        try {
            postService.deletePost(draft.getId());
            refreshStatsAndTabs();
            refreshTabContent();
            new Alert(Alert.AlertType.INFORMATION, "Brouillon supprimé.").showAndWait();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Suppression impossible : " + e.getMessage()).showAndWait();
        }
    }

    private static String statusFr(PostStatus s) {
        if (s == null) {
            return "—";
        }
        return switch (s) {
            case PUBLISHED -> "PUBLIÉ";
            case DRAFT -> "BROUILLON";
            case SCHEDULED -> "PLANIFIÉ";
            case HIDDEN -> "MASQUÉ";
        };
    }

    private static String statusBadgeClass(PostStatus s) {
        if (s == null) {
            return "badge-post-draft";
        }
        return switch (s) {
            case PUBLISHED -> "badge-post-pub";
            case DRAFT -> "badge-post-draft";
            case SCHEDULED -> "badge-post-sched";
            case HIDDEN -> "badge-post-hidden";
        };
    }

    private void fillChatroomCards(List<ChatroomListItem> rooms) {
        if (rooms.isEmpty()) {
            contentBox.getChildren().add(new Label("Aucun salon accessible pour le moment."));
            return;
        }
        for (ChatroomListItem it : rooms) {
            VBox card = new VBox(8);
            card.getStyleClass().add("profile-chat-card");
            Label t = new Label(it.goalTitle() != null ? it.goalTitle() : "Objectif #" + it.goalId());
            t.getStyleClass().add("profile-chat-title");
            String snip = it.lastMessageSnippet();
            Label sub = new Label(snip != null && !snip.isBlank()
                    ? truncate(snip, 80)
                    : "Pas encore de message.");
            sub.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");
            sub.setWrapText(true);
            Button open = new Button("Ouvrir le salon");
            open.getStyleClass().add("btn-open-chat");
            int gid = it.goalId();
            open.setOnAction(e -> openChatroom(gid));
            card.getChildren().addAll(t, sub, open);
            contentBox.getChildren().add(card);
        }
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }

    private void openChatroom(int goalId) {
        ChatroomNav.setOpenGoalId(goalId);
        try {
            NavigationManager.show("/user/chatroom/chatroom_hub.fxml", "DayFlow — Chat");
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onOpenFeed() {
        try {
            NavigationManager.show("/user/interaction/posts_feed.fxml", "DayFlow — Posts");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private static String initials(String first, String last) {
        String a = (first != null && !first.isBlank()) ? first.substring(0, 1).toUpperCase(Locale.ROOT) : "";
        String b = (last != null && !last.isBlank()) ? last.substring(0, 1).toUpperCase(Locale.ROOT) : "";
        String s = a + b;
        return s.isEmpty() ? "?" : s;
    }

    private static String fullName(User u) {
        String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String ln = u.getLastName() != null ? u.getLastName().trim() : "";
        String s = (fn + " " + ln).trim();
        return s.isEmpty() ? "Utilisateur" : s;
    }

}

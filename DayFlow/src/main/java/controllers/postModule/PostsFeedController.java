package controllers.postModule;

import enums.PostStatus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import model.interaction.Comment;
import model.interaction.Post;
import model.interaction.Tag;
import model.user.User;
import services.UserServices.UserService;
import services.comment.CommentService;
import services.post.PostLikeService;
import services.post.PostService;
import services.post.SavedPostService;
import services.tag.TagService;
import session.AppSession;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Fil de posts (tri, filtres, tags, likes, signets, commentaires).
 */
public class PostsFeedController {

    /** Entrée « tous les tags » dans le filtre par tag. */
    private static final String TAG_ALL = "Tous les tags";
    /** Format d’affichage des dates (création / modification) sur une carte post. */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.FRENCH);

    // --- Contrôles FXML (posts_feed.fxml) ---
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private ComboBox<String> filterCombo;
    @FXML
    private ComboBox<String> tagCombo;
    @FXML
    private Button createPostBtn;
    @FXML
    private VBox postsContainer;
    @FXML
    private ScrollPane scrollPane;

    // --- Services et sous-contrôleur d’interactions (likes, signets) ---
    private final PostService postService = new PostService();
    private final TagService tagService = new TagService();
    private final CommentService commentService = new CommentService();
    private final PostLikeService postLikeService = new PostLikeService();
    private final SavedPostService savedPostService = new SavedPostService();
    private final UserService userService = new UserService();
    private final InteractionController interaction = new InteractionController();

    // Pagination « scroll infini » : pages de posts ajoutées au conteneur
    private int currentPage = 0;
    private final int pageSize = 5;
    private boolean isLoading = false;
    private List<Post> allFilteredPosts = new ArrayList<>();

    /** Par post : la liste des commentaires est-elle entièrement dépliée. */
    private final java.util.Map<Integer, Boolean> commentsExpandedState = new java.util.HashMap<>();
    /** Par commentaire : les réponses sont-elles visibles (état UI). */
    private final java.util.Map<Integer, Boolean> repliesVisibleState = new java.util.HashMap<>();

    /** Initialise les combos, le scroll infini et charge le fil d’actualité. */
    @FXML
    private void initialize() {
        // Tri : ordre chronologique du fil
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier : Plus récents",
                "Trier : Plus anciens"
        ));
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setOnAction(e -> refreshFeed());

        // Filtre : tous / avec images / texte seul
        filterCombo.setItems(FXCollections.observableArrayList(
                "Filtre : Tous les posts",
                "Filtre : Avec images",
                "Filtre : Texte seul"
        ));
        filterCombo.getSelectionModel().selectFirst();
        filterCombo.setOnAction(e -> refreshFeed());

        tagCombo.setOnAction(e -> refreshFeed());

        // Quand le layout est prêt : écouter le scroll pour charger la page suivante
        Platform.runLater(() -> {
            if (scrollPane != null) {
                scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                    System.out.println("Scroll: " + newVal);
                    // Bas du ScrollPane atteint : page suivante si des posts restent
                    if (newVal.doubleValue() >= 0.95 && !isLoading
                            && currentPage * pageSize < allFilteredPosts.size()) {
                        System.out.println("Loading more posts...");
                        loadMorePosts();
                    }
                });
            }
        });

        reloadTagFilterChoices();
        refreshFeed();
    }

    /** Reconstruit les choix du filtre tag (BDD + « Tous ») en conservant la sélection si possible. */
    private void reloadTagFilterChoices() {
        String prev = tagCombo.getSelectionModel().getSelectedItem();
        List<String> items = new ArrayList<>();
        items.add(TAG_ALL);
        try {
            for (Tag t : tagService.getAllTags()) {
                if (t.getName() != null && !t.getName().isBlank()) {
                    items.add(t.getName());
                }
            }
        } catch (SQLException e) {
            showError("Tags", e);
        }
        tagCombo.setItems(FXCollections.observableArrayList(items));
        if (prev != null && items.contains(prev)) {
            tagCombo.getSelectionModel().select(prev);
        } else {
            tagCombo.getSelectionModel().selectFirst();
        }
    }

    /** Ouvre le dialogue de création, valide les champs, enregistre le post et attache les tags. */
    @FXML
    private void onCreatePost() {
        // Session requise
        if (AppSession.getCurrentUser().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Connectez-vous pour créer un post.").showAndWait();
            return;
        }

        // Dialogue : formulaire titre, contenu, tags, statut / programmation
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouveau post");
        dialog.setHeaderText("Créer un post");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("Titre");
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Contenu");
        contentArea.setPrefRowCount(6);
        contentArea.setWrapText(true);
        TextField tagsField = new TextField();
        tagsField.setPromptText("Tags (optionnel, séparés par des virgules)");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.setItems(FXCollections.observableArrayList(
                "Brouillon",
                "Publier maintenant",
                "Programmer"
        ));
        statusCombo.getSelectionModel().select("Publier maintenant");

        DatePicker datePicker = new DatePicker();
        datePicker.setVisible(false);
        datePicker.setManaged(false);

        TextField timeField = new TextField();
        timeField.setPromptText("HH:mm");
        timeField.setVisible(false);
        timeField.setManaged(false);

        // Afficher date/heure seulement si « Programmer » est choisi
        statusCombo.setOnAction(e -> {
            boolean isScheduled = "Programmer".equals(statusCombo.getValue());
            datePicker.setVisible(isScheduled);
            datePicker.setManaged(isScheduled);
            timeField.setVisible(isScheduled);
            timeField.setManaged(isScheduled);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Titre :"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Contenu :"), 0, 1);
        grid.add(contentArea, 1, 1);
        grid.add(new Label("Tags :"), 0, 2);
        grid.add(tagsField, 1, 2);
        grid.add(new Label("Statut :"), 0, 3);
        grid.add(statusCombo, 1, 3);
        grid.add(new Label("Date :"), 0, 4);
        grid.add(datePicker, 1, 4);
        grid.add(new Label("Heure :"), 0, 5);
        grid.add(timeField, 1, 5);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        // Saisie obligatoire : titre et contenu
        String title = titleField.getText() != null ? titleField.getText().trim() : "";
        String content = contentArea.getText() != null ? contentArea.getText().trim() : "";
        if (title.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Le titre est obligatoire.").showAndWait();
            return;
        }
        if (content.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Le contenu est obligatoire.").showAndWait();
            return;
        }

        // Construire le Post : auteur, compteurs, statut (brouillon / publié / programmé)
        int uid = AppSession.getCurrentUser().get().getId();
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setCreatedById(uid);
        post.setImages(List.of());
        post.setViewCount(0);
        post.setClickCount(0);
        LocalDateTime now = LocalDateTime.now();
        String selectedStatus = statusCombo.getValue();
        if ("Brouillon".equals(selectedStatus)) {
            post.setStatus(PostStatus.DRAFT);
            post.setCreatedAt(now);
            post.setScheduledAt(null);
        } else if ("Programmer".equals(selectedStatus)) {
            if (datePicker.getValue() == null || timeField.getText() == null || timeField.getText().isBlank()) {
                new Alert(Alert.AlertType.WARNING, "La date et l'heure sont obligatoires pour programmer un post.").showAndWait();
                return;
            }
            try {
                LocalTime time = LocalTime.parse(timeField.getText().trim());
                LocalDateTime scheduled = LocalDateTime.of(datePicker.getValue(), time);
                post.setStatus(PostStatus.SCHEDULED);
                post.setScheduledAt(scheduled);
                post.setCreatedAt(now);
            } catch (Exception ex) {
                new Alert(Alert.AlertType.WARNING, "Heure invalide. Utilisez le format HH:mm.").showAndWait();
                return;
            }
        } else {
            post.setStatus(PostStatus.PUBLISHED);
            post.setCreatedAt(now);
            post.setScheduledAt(null);
        }
        if (post.getSlug() == null) {
            post.setSlug(title.toLowerCase(Locale.FRENCH).replace(" ", "-").replaceAll("[^a-z0-9\\-]", ""));
        }

        // Insertion puis liaison des tags (création du tag si besoin)
        try {
            postService.insert(post);
            int pid = post.getId();
            String tagsCsv = tagsField.getText();
            if (tagsCsv != null && !tagsCsv.isBlank()) {
                for (String part : tagsCsv.split(",")) {
                    String name = part.trim();
                    if (name.isEmpty()) {
                        continue;
                    }
                    Tag t = findTagByName(name);
                    if (t == null) {
                        t = new Tag();
                        t.setName(name);
                        tagService.addTag(t);
                    }
                    tagService.attachTagToPost(pid, t.getId());
                }
            }
            reloadTagFilterChoices();
            refreshFeed();
            new Alert(Alert.AlertType.INFORMATION, "Post publié.").showAndWait();
        } catch (SQLException e) {
            showError("Création du post", e);
        }
    }

    /** Retourne un tag existant par nom (sans respect de la casse), ou null. */
    private Tag findTagByName(String name) throws SQLException {
        for (Tag t : tagService.getAllTags()) {
            if (t.getName() != null && t.getName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    /** Remet à zéro la pagination, applique filtres / tri / tag, puis affiche la première page. */
    private void refreshFeed() {
        currentPage = 0;
        isLoading = false;
        postsContainer.getChildren().clear();
        commentsExpandedState.clear();
        repliesVisibleState.clear();
        
        try {
            // Base : uniquement les posts publiés
            allFilteredPosts = postService.getAllPosts().stream()
                    .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
                    .collect(Collectors.toList());

            // Filtre média : avec images ou texte seul
            String filter = filterCombo.getSelectionModel().getSelectedItem();
            if ("Filtre : Avec images".equals(filter)) {
                allFilteredPosts = allFilteredPosts.stream()
                        .filter(p -> p.getImages() != null && !p.getImages().isEmpty())
                        .collect(Collectors.toList());
            } else if ("Filtre : Texte seul".equals(filter)) {
                allFilteredPosts = allFilteredPosts.stream()
                        .filter(p -> p.getImages() == null || p.getImages().isEmpty())
                        .collect(Collectors.toList());
            }

            // Filtre par un tag précis (hors « Tous les tags »)
            String tagPick = tagCombo.getSelectionModel().getSelectedItem();
            if (tagPick != null && !TAG_ALL.equals(tagPick)) {
                final String tagName = tagPick;
                List<Post> filtered = new ArrayList<>();
                for (Post p : allFilteredPosts) {
                    List<Tag> tags = tagService.getTagsByPost(p.getId());
                    boolean match = tags.stream().anyMatch(t -> tagName.equalsIgnoreCase(t.getName()));
                    if (match) {
                        filtered.add(p);
                    }
                }
                allFilteredPosts = filtered;
            }

            // Tri par date de création (plus récent ou plus ancien en premier)
            boolean newestFirst = "Trier : Plus récents".equals(sortCombo.getSelectionModel().getSelectedItem());
            allFilteredPosts.sort((a, b) -> {
                java.time.LocalDateTime da = a.getCreatedAt() != null ? a.getCreatedAt() : java.time.LocalDateTime.MIN;
                java.time.LocalDateTime db = b.getCreatedAt() != null ? b.getCreatedAt() : java.time.LocalDateTime.MIN;
                return newestFirst ? db.compareTo(da) : da.compareTo(db);
            });

            // Première « page » de cartes dans le VBox
            loadMorePosts();
            
        } catch (SQLException e) {
            showError("Chargement des posts", e);
        }
    }

    /** Ajoute au conteneur la tranche [page courante × taille …] sans recharger toute la liste. */
    private void loadMorePosts() {
        if (isLoading) return;
        isLoading = true;

        try {
            Integer currentUserId = AppSession.getCurrentUser().map(User::getId).orElse(null);
            
            int start = currentPage * pageSize;
            int end = Math.min(start + pageSize, allFilteredPosts.size());

            // Plus rien à afficher pour cette page
            if (start >= allFilteredPosts.size()) {
                isLoading = false;
                return;
            }

            // Message si aucun résultat après filtres (uniquement au premier chargement)
            if (currentPage == 0 && allFilteredPosts.isEmpty()) {
                Label empty = new Label("Aucun post pour ces critères.");
                empty.getStyleClass().add("post-meta");
                postsContainer.getChildren().add(empty);
            }

            // Une carte UI par post de la page courante
            for (int i = start; i < end; i++) {
                Post p = allFilteredPosts.get(i);
                postsContainer.getChildren().add(buildPostCard(p, currentUserId));
            }

            currentPage++;
            isLoading = false;
        } catch (SQLException e) {
            showError("Chargement des posts", e);
            isLoading = false;
        }
    }

    /** Assemble une carte complète : en-tête, tags, corps, likes, commentaires. */
    private VBox buildPostCard(Post post, Integer currentUserId) throws SQLException {
        VBox card = new VBox(12);
        card.getStyleClass().add("post-card");
        card.setMaxWidth(720);

        // Auteur : nom affiché + initiales pour l’avatar
        User author = post.getCreatedById() != null
                ? userService.findById(post.getCreatedById()).orElse(null)
                : null;
        String authorName = author != null
                ? ((author.getFirstName() != null ? author.getFirstName() : "") + " "
                + (author.getLastName() != null ? author.getLastName() : "")).trim()
                : "Utilisateur";
        if (authorName.isBlank()) {
            authorName = "Utilisateur #" + post.getCreatedById();
        }

        Label avatar = new Label(author != null ? initials(author.getFirstName(), author.getLastName()) : "?");
        avatar.getStyleClass().add("post-avatar");
        avatar.setMinSize(44, 44);

        Label nameLbl = new Label(authorName);
        nameLbl.getStyleClass().add("post-author-name");

        String meta = formatPostMeta(post);
        Label metaLbl = new Label(meta);
        metaLbl.getStyleClass().add("post-meta");

        VBox nameCol = new VBox(2);
        nameCol.getChildren().addAll(nameLbl, metaLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Signet : bascule enregistrer / retirer pour l’utilisateur connecté
        Button bookmark = new Button("🔖");
        bookmark.getStyleClass().add("post-icon-btn");
        if (currentUserId != null && isPostSaved(currentUserId, post.getId())) {
            bookmark.getStyleClass().add("saved");
        }
        bookmark.setOnAction(e -> {
            if (currentUserId == null) {
                new Alert(Alert.AlertType.INFORMATION, "Connectez-vous pour enregistrer un post.").showAndWait();
                return;
            }
            try {
                if (isPostSaved(currentUserId, post.getId())) {
                    String msg = interaction.unsavePost(post.getId(), currentUserId);
                    if (msg != null && msg.startsWith("Error")) {
                        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
                        return;
                    }
                    bookmark.getStyleClass().remove("saved");
                } else {
                    String msg = interaction.savePost(post.getId(), currentUserId);
                    if (msg != null && msg.startsWith("Error")) {
                        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
                        return;
                    }
                    if (!bookmark.getStyleClass().contains("saved")) {
                        bookmark.getStyleClass().add("saved");
                    }
                }
            } catch (SQLException ex) {
                showError("Signet", ex);
            }
        });

        // Titre et corps créés tôt : le menu contextuel (propriétaire) y réfère pour l’édition
        Label titleLbl = new Label(post.getTitle());
        titleLbl.getStyleClass().add("post-title");
        titleLbl.setWrapText(true);

        Text bodyText = new Text(htmlToPlainText(post.getContent()));
        bodyText.getStyleClass().add("post-body");
        TextFlow bodyFlow = new TextFlow(bodyText);
        bodyFlow.setMaxWidth(680);

        Button menu = new Button("⋯");
        menu.getStyleClass().add("post-icon-btn");
        menu.setDisable(true);

        // Menu ⋯ : modifier / supprimer réservé au créateur du post
        if (currentUserId != null && currentUserId.equals(post.getCreatedById())) {
            menu.setDisable(false);
            
            ContextMenu contextMenu = new ContextMenu();
            
            MenuItem editItem = new MenuItem("Modifier");
            editItem.setOnAction(e -> {
                try {
                    enterEditMode(card, post, titleLbl, bodyFlow);
                } catch (SQLException ex) {
                    showError("Edit mode", ex);
                }
            });
            
            MenuItem deleteItem = new MenuItem("Supprimer");
            deleteItem.setOnAction(e -> {
                try {
                    deletePost(post);
                } catch (SQLException ex) {
                    showError("Suppression", ex);
                }
            });
            
            contextMenu.getItems().addAll(editItem, deleteItem);
            menu.setOnAction(e -> contextMenu.show(menu, Side.BOTTOM, 0, 0));
        }

        // Ligne d’en-tête : avatar, nom, méta, signet, menu
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(avatar, nameCol, spacer, bookmark, menu);

        // Pastilles de tags liés au post
        javafx.scene.layout.FlowPane tagsFlow = new javafx.scene.layout.FlowPane();
        tagsFlow.getStyleClass().add("post-tags-flow");
        tagsFlow.setPrefWrapLength(680);
        for (Tag t : tagService.getTagsByPost(post.getId())) {
            Label pill = new Label(t.getName());
            pill.getStyleClass().add("post-tag-pill");
            tagsFlow.getChildren().add(pill);
        }

        // Compteurs likes et commentaires
        int likeCount = postLikeService.findByPostId(post.getId()).size();
        int commentCount = commentService.getCommentsByPost(post.getId()).size();

        Button likeBtn = new Button("♡  " + likeCount);
        likeBtn.getStyleClass().add("like-btn");
        if (currentUserId != null && isPostLikedBy(post.getId(), currentUserId)) {
            likeBtn.getStyleClass().add("liked");
            likeBtn.setText("♥  " + likeCount);
        }
        likeBtn.setOnAction(e -> {
            if (currentUserId == null) {
                new Alert(Alert.AlertType.INFORMATION, "Connectez-vous pour aimer un post.").showAndWait();
                return;
            }
            try {
                String msg;
                if (isPostLikedBy(post.getId(), currentUserId)) {
                    msg = interaction.unlikePost(post.getId(), currentUserId);
                } else {
                    msg = interaction.likePost(post.getId(), currentUserId);
                }
                if (msg != null && msg.startsWith("Error")) {
                    new Alert(Alert.AlertType.ERROR, msg).showAndWait();
                    return;
                }
                refreshFeed();
            } catch (SQLException ex) {
                showError("Like", ex);
            }
        });

        // Ligne de stats sous le contenu
        Label statsComments = new Label("💬  " + commentCount + " commentaire" + (commentCount != 1 ? "s" : ""));
        statsComments.getStyleClass().add("post-stat-text");

        HBox stats = new HBox(20);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(likeBtn, statsComments);
        stats.getStyleClass().add("post-stats");

        Separator sep = new Separator();

        // Saisie d’un nouveau commentaire sur ce post
        Label avatarSm = new Label(AppSession.getCurrentUser()
                .map(u -> initials(u.getFirstName(), u.getLastName()))
                .orElse("?"));
        avatarSm.getStyleClass().add("comment-avatar-sm");

        TextField commentField = new TextField();
        commentField.setPromptText("Écrire un commentaire…");
        commentField.getStyleClass().add("comment-field");
        HBox.setHgrow(commentField, Priority.ALWAYS);

        Button sendComment = new Button("Commenter");
        sendComment.getStyleClass().add("btn-comment-send");
        sendComment.setOnAction(e -> submitComment(post.getId(), commentField));

        HBox commentRow = new HBox(10);
        commentRow.setAlignment(Pos.CENTER_LEFT);
        commentRow.getChildren().addAll(avatarSm, commentField, sendComment);
        commentRow.getStyleClass().add("comment-row");

        // Liste des commentaires (aperçu limité + bouton afficher plus)
        VBox commentsBlock = new VBox(8);
        List<Comment> comments = commentService.getCommentsByPost(post.getId());
        int maxInitialComments = 3;
        
        if (comments.isEmpty()) {
            Label empty = new Label("Pas encore de commentaire. Soyez le premier !");
            empty.getStyleClass().add("post-empty-comments");
            commentsBlock.getChildren().add(empty);
        } else {
            boolean isExpanded = commentsExpandedState.getOrDefault(post.getId(), false);

            int commentsToShow = isExpanded ? comments.size() : Math.min(maxInitialComments, comments.size());

            for (int i = 0; i < commentsToShow; i++) {
                commentsBlock.getChildren().add(buildCommentLine(comments.get(i)));
            }

            if (comments.size() > maxInitialComments) {
                Button toggleCommentsBtn = new Button(isExpanded ? 
                    "Masquer les commentaires" : 
                    ("Afficher plus de commentaires (" + (comments.size() - maxInitialComments) + ")"));
                toggleCommentsBtn.getStyleClass().add("like-btn");
                toggleCommentsBtn.setStyle("-fx-font-size: 12px;");
                
                toggleCommentsBtn.setOnAction(e -> {
                    try {
                        boolean expanded = !commentsExpandedState.getOrDefault(post.getId(), false);
                        commentsExpandedState.put(post.getId(), expanded);

                        // Reconstruire uniquement le bloc commentaires (sans rafraîchir tout le fil)
                        commentsBlock.getChildren().clear();
                        
                        int newCommentsToShow = expanded ? comments.size() : Math.min(maxInitialComments, comments.size());
                        for (int i = 0; i < newCommentsToShow; i++) {
                            commentsBlock.getChildren().add(buildCommentLine(comments.get(i)));
                        }

                        String newBtnText = expanded ? 
                            "Masquer les commentaires" : 
                            "Afficher plus de commentaires (" + (comments.size() - maxInitialComments) + ")";
                        toggleCommentsBtn.setText(newBtnText);
                        commentsBlock.getChildren().add(toggleCommentsBtn);
                    } catch (SQLException ex) {
                        showError("Toggle comments", ex);
                    }
                });
                
                commentsBlock.getChildren().add(toggleCommentsBtn);
            }
        }

        // Assemblage final de la carte
        card.getChildren().addAll(header);
        if (!tagsFlow.getChildren().isEmpty()) {
            card.getChildren().add(tagsFlow);
        }
        card.getChildren().addAll(titleLbl, bodyFlow, stats, sep, commentRow, commentsBlock);

        return card;
    }

    /** Une ligne de commentaire : texte, like, réponses locales (UI). */
    private VBox buildCommentLine(Comment c) throws SQLException {
        User u = userService.findById(c.getCommenterId()).orElse(null);
        String name = u != null
                ? ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                + (u.getLastName() != null ? u.getLastName() : "")).trim()
                : ("#" + c.getCommenterId());
        if (name.isBlank()) {
            name = "Utilisateur";
        }

        Integer currentUserId = AppSession.getCurrentUser().map(User::getId).orElse(null);

        VBox commentCard = new VBox(8);
        commentCard.getStyleClass().add("comment-card");
        commentCard.setPadding(new Insets(8, 10, 8, 10));
        commentCard.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 4; -fx-background-color: #f9f9f9;");

        Label contentLbl = new Label(name + " — " + htmlToPlainText(c.getContent()));
        contentLbl.setWrapText(true);
        contentLbl.getStyleClass().add("post-body");

        int likeCount = getCommentLikeCount(c.getId());

        // Like sur le commentaire (même style que le like post)
        Button likeBtn = new Button("♡ " + likeCount);
        likeBtn.getStyleClass().add("like-btn");
        if (currentUserId != null && isCommentLikedBy(c.getId(), currentUserId)) {
            likeBtn.getStyleClass().add("liked");
            likeBtn.setText("♥ " + likeCount);
        }

        likeBtn.setOnAction(e -> {
            if (currentUserId == null) {
                new Alert(Alert.AlertType.INFORMATION, "Connectez-vous pour aimer un commentaire.").showAndWait();
                return;
            }
            try {
                String msg;
                if (isCommentLikedBy(c.getId(), currentUserId)) {
                    msg = interaction.unlikeComment(c.getId(), currentUserId);
                } else {
                    msg = interaction.likeComment(c.getId(), currentUserId);
                }
                
                if (msg != null && msg.startsWith("Error")) {
                    new Alert(Alert.AlertType.ERROR, msg).showAndWait();
                    return;
                }
                refreshFeed();
            } catch (SQLException ex) {
                showError("Like commentaire", ex);
            }
        });

        Button replyBtn = new Button("💬 Répondre");
        replyBtn.getStyleClass().add("like-btn");
        replyBtn.setStyle("-fx-font-size: 13;");

        HBox actionsRow = new HBox(12);
        actionsRow.setAlignment(Pos.CENTER_LEFT);
        actionsRow.getChildren().addAll(likeBtn, replyBtn);
        actionsRow.getStyleClass().add("comment-actions");

        // Réponses affichées sous le commentaire (masquées jusqu’à présence / toggle)
        VBox repliesContainer = new VBox(6);
        repliesContainer.getStyleClass().add("comment-replies");
        repliesContainer.setPadding(new Insets(8, 0, 0, 16));
        repliesContainer.setStyle("-fx-border-left: 2 solid #ddd;");
        repliesContainer.setVisible(false);
        repliesContainer.setManaged(false);

        TextField replyField = new TextField();
        replyField.setPromptText("Répondre...");
        replyField.getStyleClass().add("comment-reply-field");
        replyField.setPrefHeight(32);
        HBox.setHgrow(replyField, Priority.ALWAYS);

        Button sendReplyBtn = new Button("Envoyer");
        sendReplyBtn.getStyleClass().add("btn-comment-reply");
        sendReplyBtn.setStyle("-fx-padding: 6 12;");

        HBox replyInputRow = new HBox(8);
        replyInputRow.setAlignment(Pos.CENTER_LEFT);
        replyInputRow.getChildren().addAll(replyField, sendReplyBtn);
        replyInputRow.setPadding(new Insets(8, 0, 0, 0));
        replyInputRow.setManaged(false);
        replyInputRow.setVisible(false);
        replyInputRow.getStyleClass().add("comment-input-row");

        // Bouton pour afficher / masquer la liste des réponses (apparaît s’il y a des réponses)
        Button toggleRepliesBtn = new Button("Afficher les réponses (0)");
        toggleRepliesBtn.getStyleClass().add("like-btn");
        toggleRepliesBtn.setStyle("-fx-font-size: 12px;");
        toggleRepliesBtn.setVisible(false);
        toggleRepliesBtn.setManaged(false);

        Runnable updateToggleButton = () -> {
            int replyCount = repliesContainer.getChildren().size();
            if (replyCount > 0) {
                if (!toggleRepliesBtn.isVisible()) {
                    toggleRepliesBtn.setVisible(true);
                    toggleRepliesBtn.setManaged(true);
                    if (!actionsRow.getChildren().contains(toggleRepliesBtn)) {
                        actionsRow.getChildren().add(toggleRepliesBtn);
                    }
                }
                boolean isVisible = repliesContainer.isVisible();
                toggleRepliesBtn.setText(isVisible ? "Masquer les réponses" : "Afficher les réponses (" + replyCount + ")");
            }
        };

        toggleRepliesBtn.setOnAction(e -> {
            boolean isVisible = repliesContainer.isVisible();
            repliesContainer.setVisible(!isVisible);
            repliesContainer.setManaged(!isVisible);
            updateToggleButton.run();
        });

        // Réponse affichée dans le fil du commentaire (saisie non vide)
        sendReplyBtn.setOnAction(e -> {
            String replyText = replyField.getText();
            if (replyText != null && !replyText.isBlank()) {
                String replyAuthor = AppSession.getCurrentUser()
                        .map(us -> {
                            String fn = us.getFirstName() != null ? us.getFirstName() : "";
                            String ln = us.getLastName() != null ? us.getLastName() : "";
                            return (fn + " " + ln).trim();
                        })
                        .orElse("Vous");

                Label replyContentLbl = new Label(replyAuthor + " — " + replyText);
                replyContentLbl.setWrapText(true);
                replyContentLbl.getStyleClass().add("post-body");

                VBox replyItem = new VBox(4);
                replyItem.getChildren().add(replyContentLbl);
                replyItem.setPadding(new Insets(6, 0, 6, 0));

                repliesContainer.getChildren().add(replyItem);
                replyField.clear();

                updateToggleButton.run();
            } else {
                new Alert(Alert.AlertType.WARNING, "La réponse ne peut pas être vide.").showAndWait();
            }
        });

        // Afficher ou masquer le champ « Répondre » sous le commentaire
        replyBtn.setOnAction(e -> {
            boolean isVisible = replyInputRow.isVisible();
            replyInputRow.setVisible(!isVisible);
            replyInputRow.setManaged(!isVisible);
            if (!isVisible) {
                replyField.requestFocus();
            }
        });

        commentCard.getChildren().addAll(contentLbl, actionsRow, repliesContainer, replyInputRow);

        return commentCard;
    }

    /** Valide la saisie, persiste le commentaire racine (sans parent) et rafraîchit le fil. */
    private void submitComment(int postId, TextField field) {
        if (AppSession.getCurrentUser().isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Connectez-vous pour commenter.").showAndWait();
            return;
        }
        // Texte non vide requis avant appel service
        String text = field.getText() != null ? field.getText().trim() : "";
        if (text.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Le commentaire ne peut pas être vide.").showAndWait();
            return;
        }
        int uid = AppSession.getCurrentUser().get().getId();
        Comment c = new Comment();
        c.setContent(text);
        c.setCreatedAt(java.time.LocalDateTime.now());
        c.setPostId(postId);
        c.setCommenterId(uid);
        c.setParentCommentId(null);
        try {
            commentService.addComment(c);
            field.clear();
            refreshFeed();
        } catch (SQLException e) {
            showError("Commentaire", e);
        }
    }

    /** Indique si le post est dans les signets de l’utilisateur. */
    private boolean isPostSaved(int userId, int postId) throws SQLException {
        return savedPostService.findByUserId(userId).stream()
                .anyMatch(s -> s.getPostId() != null && s.getPostId() == postId);
    }

    private boolean isPostLikedBy(int postId, int userId) throws SQLException {
        return postLikeService.findByPostId(postId).stream()
                .anyMatch(l -> Objects.equals(l.getLikerId(), userId));
    }

    /** Indique si l’utilisateur a liké ce commentaire. */
    private boolean isCommentLikedBy(int commentId, int userId) throws SQLException {
        try {
            services.comment.CommentLikeService commentLikeService = new services.comment.CommentLikeService();
            return commentLikeService.findByCommentId(commentId).stream()
                    .anyMatch(cl -> Objects.equals(cl.getUserId(), userId));
        } catch (SQLException e) {
            return false;
        }
    }

    /** Nombre de likes sur un commentaire. */
    private int getCommentLikeCount(int commentId) throws SQLException {
        try {
            services.comment.CommentLikeService commentLikeService = new services.comment.CommentLikeService();
            return commentLikeService.findByCommentId(commentId).size();
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Le contenu peut venir du web (Symfony / éditeur riche) avec des balises HTML.
     * {@link Text} JavaFX n'interprète pas le HTML — on affiche du texte lisible sans balises.
     */
    private static String htmlToPlainText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw
                .replaceAll("(?i)<\\s*br\\s*/?>", "\n")
                .replaceAll("(?i)</p>\\s*", "\n")
                .replaceAll("(?i)<\\s*p[^>]*>", "");
        s = s.replaceAll("<[^>]+>", "");
        s = s.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"");
        return s.trim();
    }

    /** Libellé « Publié … » ou « Modifié … » selon les dates du post. */
    private static String formatPostMeta(Post p) {
        java.time.LocalDateTime ref = p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt();
        if (ref == null) {
            return "";
        }
        String prefix = p.getUpdatedAt() != null ? "Modifié " : "Publié ";
        return prefix + ref.format(DATE_FMT);
    }

    /** Initiales pour l’avatar (2 lettres max, « ? » si aucun nom). */
    private static String initials(String firstName, String lastName) {
        String a = (firstName != null && !firstName.isBlank()) ? firstName.substring(0, 1).toUpperCase(Locale.FRENCH) : "";
        String b = (lastName != null && !lastName.isBlank()) ? lastName.substring(0, 1).toUpperCase(Locale.FRENCH) : "";
        String s = a + b;
        return s.isEmpty() ? "?" : s;
    }

    /** Boîte d’erreur standard pour les échecs SQL. */
    private static void showError(String ctx, SQLException e) {
        new Alert(Alert.AlertType.ERROR, ctx + " : " + e.getMessage()).showAndWait();
    }

    /**
     * Transform post card to edit mode: replace title label and body text flow with editable fields
     */
    private void enterEditMode(VBox card, Post post, Label titleLbl, TextFlow bodyFlow) throws SQLException {
        TextField titleField = new TextField(post.getTitle());
        titleField.getStyleClass().add("post-title");
        titleField.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        
        TextArea contentArea = new TextArea(htmlToPlainText(post.getContent()));
        contentArea.getStyleClass().add("post-body");
        contentArea.setPrefRowCount(6);
        contentArea.setWrapText(true);
        contentArea.setMaxWidth(680);

        int titleIndex = card.getChildren().indexOf(titleLbl);
        int bodyIndex = card.getChildren().indexOf(bodyFlow);
        
        if (titleIndex >= 0) {
            card.getChildren().set(titleIndex, titleField);
        }
        if (bodyIndex >= 0) {
            card.getChildren().set(bodyIndex, contentArea);
        }

        Button saveBtn = new Button("Enregistrer");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                savePost(card, post, titleField, contentArea, titleLbl, bodyFlow);
            } catch (SQLException ex) {
                showError("Saving post", ex);
            }
        });
        
        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> exitEditMode(card, titleField, contentArea, titleLbl, bodyFlow));
        
        HBox editActions = new HBox(10);
        editActions.setAlignment(Pos.CENTER_LEFT);
        editActions.getChildren().addAll(saveBtn, cancelBtn);
        editActions.setPadding(new Insets(10, 0, 0, 0));

        if (bodyIndex >= 0) {
            card.getChildren().add(bodyIndex + 1, editActions);
        }
    }

    /**
     * Restore post card to normal view (cancel edit without saving)
     */
    private void exitEditMode(VBox card, TextField titleField, TextArea contentArea, Label titleLbl, TextFlow bodyFlow) {
        refreshFeed();
    }

    /**
     * Save edited post
     */
    private void savePost(VBox card, Post post, TextField titleField, TextArea contentArea, Label titleLbl, TextFlow bodyFlow) throws SQLException {
        String newTitle = titleField.getText() != null ? titleField.getText().trim() : "";
        String newContent = contentArea.getText() != null ? contentArea.getText().trim() : "";

        if (newTitle.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Le titre est obligatoire.").showAndWait();
            return;
        }

        post.setTitle(newTitle);
        post.setContent(newContent.isEmpty() ? null : newContent);
        post.setUpdatedAt(java.time.LocalDateTime.now());

        try {
            postService.update(post);
            refreshFeed();
            new Alert(Alert.AlertType.INFORMATION, "Post mis à jour.").showAndWait();
        } catch (SQLException e) {
            showError("Mise à jour du post", e);
        }
    }

    /**
     * Delete post with confirmation (marks as HIDDEN, doesn't remove from database)
     */
    private void deletePost(Post post) throws SQLException {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer le post ?");
        confirm.setContentText("Voulez-vous vraiment supprimer ce post ?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Soft delete : statut masqué plutôt que suppression en base
                post.setStatus(PostStatus.HIDDEN);
                post.setUpdatedAt(java.time.LocalDateTime.now());
                postService.update(post);
                refreshFeed();
                new Alert(Alert.AlertType.INFORMATION, "Post supprimé.").showAndWait();
            } catch (SQLException e) {
                showError("Suppression du post", e);
            }
        }
    }

}

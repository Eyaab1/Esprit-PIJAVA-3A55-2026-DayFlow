package controllers.postModule;

import enums.PostStatus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

    private static final String TAG_ALL = "Tous les tags";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.FRENCH);

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

    private final PostService postService = new PostService();
    private final TagService tagService = new TagService();
    private final CommentService commentService = new CommentService();
    private final PostLikeService postLikeService = new PostLikeService();
    private final SavedPostService savedPostService = new SavedPostService();
    private final UserService userService = new UserService();
    private final InteractionController interaction = new InteractionController();

    @FXML
    private void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier : Plus récents",
                "Trier : Plus anciens"
        ));
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setOnAction(e -> refreshFeed());

        filterCombo.setItems(FXCollections.observableArrayList(
                "Filtre : Tous les posts",
                "Filtre : Avec images",
                "Filtre : Texte seul"
        ));
        filterCombo.getSelectionModel().selectFirst();
        filterCombo.setOnAction(e -> refreshFeed());

        tagCombo.setOnAction(e -> refreshFeed());

        reloadTagFilterChoices();
        refreshFeed();
    }

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

    @FXML
    private void onCreatePost() {
        if (AppSession.getCurrentUser().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Connectez-vous pour créer un post.").showAndWait();
            return;
        }

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
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        String title = titleField.getText() != null ? titleField.getText().trim() : "";
        String content = contentArea.getText() != null ? contentArea.getText().trim() : "";
        if (title.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Le titre est obligatoire.").showAndWait();
            return;
        }

        int uid = AppSession.getCurrentUser().get().getId();
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content.isEmpty() ? null : content);
        post.setStatus(PostStatus.PUBLISHED);
        post.setCreatedById(uid);
        post.setCreatedAt(java.time.LocalDateTime.now());
        post.setImages(List.of());
        post.setViewCount(0);
        post.setClickCount(0);
        if (post.getSlug() == null) {
            post.setSlug(title.toLowerCase(Locale.FRENCH).replace(" ", "-").replaceAll("[^a-z0-9\\-]", ""));
        }

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

    private Tag findTagByName(String name) throws SQLException {
        for (Tag t : tagService.getAllTags()) {
            if (t.getName() != null && t.getName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    private void refreshFeed() {
        postsContainer.getChildren().clear();
        try {
            List<Post> posts = postService.getAllPosts().stream()
                    .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
                    .filter(p -> p.getDeletedAt() == null)
                    .collect(Collectors.toList());

            String filter = filterCombo.getSelectionModel().getSelectedItem();
            if ("Filtre : Avec images".equals(filter)) {
                posts = posts.stream()
                        .filter(p -> p.getImages() != null && !p.getImages().isEmpty())
                        .collect(Collectors.toList());
            } else if ("Filtre : Texte seul".equals(filter)) {
                posts = posts.stream()
                        .filter(p -> p.getImages() == null || p.getImages().isEmpty())
                        .collect(Collectors.toList());
            }

            String tagPick = tagCombo.getSelectionModel().getSelectedItem();
            if (tagPick != null && !TAG_ALL.equals(tagPick)) {
                final String tagName = tagPick;
                List<Post> filtered = new ArrayList<>();
                for (Post p : posts) {
                    List<Tag> tags = tagService.getTagsByPost(p.getId());
                    boolean match = tags.stream().anyMatch(t -> tagName.equalsIgnoreCase(t.getName()));
                    if (match) {
                        filtered.add(p);
                    }
                }
                posts = filtered;
            }

            boolean newestFirst = "Trier : Plus récents".equals(sortCombo.getSelectionModel().getSelectedItem());
            posts.sort((a, b) -> {
                java.time.LocalDateTime da = a.getCreatedAt() != null ? a.getCreatedAt() : java.time.LocalDateTime.MIN;
                java.time.LocalDateTime db = b.getCreatedAt() != null ? b.getCreatedAt() : java.time.LocalDateTime.MIN;
                return newestFirst ? db.compareTo(da) : da.compareTo(db);
            });

            Integer currentUserId = AppSession.getCurrentUser().map(User::getId).orElse(null);

            for (Post p : posts) {
                postsContainer.getChildren().add(buildPostCard(p, currentUserId));
            }

            if (posts.isEmpty()) {
                Label empty = new Label("Aucun post pour ces critères.");
                empty.getStyleClass().add("post-meta");
                postsContainer.getChildren().add(empty);
            }
        } catch (SQLException e) {
            showError("Chargement des posts", e);
        }
    }

    private VBox buildPostCard(Post post, Integer currentUserId) throws SQLException {
        VBox card = new VBox(12);
        card.getStyleClass().add("post-card");
        card.setMaxWidth(720);

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

        Button menu = new Button("⋯");
        menu.getStyleClass().add("post-icon-btn");
        menu.setDisable(true);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(avatar, nameCol, spacer, bookmark, menu);

        javafx.scene.layout.FlowPane tagsFlow = new javafx.scene.layout.FlowPane();
        tagsFlow.getStyleClass().add("post-tags-flow");
        tagsFlow.setPrefWrapLength(680);
        for (Tag t : tagService.getTagsByPost(post.getId())) {
            Label pill = new Label(t.getName());
            pill.getStyleClass().add("post-tag-pill");
            tagsFlow.getChildren().add(pill);
        }

        Label titleLbl = new Label(post.getTitle());
        titleLbl.getStyleClass().add("post-title");
        titleLbl.setWrapText(true);

        Text bodyText = new Text(post.getContent() != null ? post.getContent() : "");
        bodyText.getStyleClass().add("post-body");
        TextFlow bodyFlow = new TextFlow(bodyText);
        bodyFlow.setMaxWidth(680);

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

        Label statsComments = new Label("💬  " + commentCount + " commentaire" + (commentCount != 1 ? "s" : ""));
        statsComments.getStyleClass().add("post-stat-text");

        HBox stats = new HBox(20);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(likeBtn, statsComments);
        stats.getStyleClass().add("post-stats");

        Separator sep = new Separator();

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

        VBox commentsBlock = new VBox(8);
        List<Comment> comments = commentService.getCommentsByPost(post.getId());
        if (comments.isEmpty()) {
            Label empty = new Label("Pas encore de commentaire. Soyez le premier !");
            empty.getStyleClass().add("post-empty-comments");
            commentsBlock.getChildren().add(empty);
        } else {
            for (Comment c : comments) {
                commentsBlock.getChildren().add(buildCommentLine(c));
            }
        }

        card.getChildren().addAll(header);
        if (!tagsFlow.getChildren().isEmpty()) {
            card.getChildren().add(tagsFlow);
        }
        card.getChildren().addAll(titleLbl, bodyFlow, stats, sep, commentRow, commentsBlock);

        return card;
    }

    private HBox buildCommentLine(Comment c) throws SQLException {
        User u = userService.findById(c.getCommenterId()).orElse(null);
        String name = u != null
                ? ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                + (u.getLastName() != null ? u.getLastName() : "")).trim()
                : ("#" + c.getCommenterId());
        if (name.isBlank()) {
            name = "Utilisateur";
        }
        Label lbl = new Label(name + " — " + (c.getContent() != null ? c.getContent() : ""));
        lbl.setWrapText(true);
        lbl.getStyleClass().add("post-body");
        HBox row = new HBox(lbl);
        row.setPadding(new Insets(4, 0, 0, 0));
        return row;
    }

    private void submitComment(int postId, TextField field) {
        if (AppSession.getCurrentUser().isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Connectez-vous pour commenter.").showAndWait();
            return;
        }
        String text = field.getText() != null ? field.getText().trim() : "";
        if (text.isEmpty()) {
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

    private boolean isPostSaved(int userId, int postId) throws SQLException {
        return savedPostService.findByUserId(userId).stream()
                .anyMatch(s -> s.getPostId() != null && s.getPostId() == postId);
    }

    private boolean isPostLikedBy(int postId, int userId) throws SQLException {
        return postLikeService.findByPostId(postId).stream()
                .anyMatch(l -> Objects.equals(l.getLikerId(), userId));
    }

    private static String formatPostMeta(Post p) {
        java.time.LocalDateTime ref = p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt();
        if (ref == null) {
            return "";
        }
        String prefix = p.getUpdatedAt() != null ? "Modifié " : "Publié ";
        return prefix + ref.format(DATE_FMT);
    }

    private static String initials(String firstName, String lastName) {
        String a = (firstName != null && !firstName.isBlank()) ? firstName.substring(0, 1).toUpperCase(Locale.FRENCH) : "";
        String b = (lastName != null && !lastName.isBlank()) ? lastName.substring(0, 1).toUpperCase(Locale.FRENCH) : "";
        String s = a + b;
        return s.isEmpty() ? "?" : s;
    }

    private static void showError(String ctx, SQLException e) {
        new Alert(Alert.AlertType.ERROR, ctx + " : " + e.getMessage()).showAndWait();
    }
}

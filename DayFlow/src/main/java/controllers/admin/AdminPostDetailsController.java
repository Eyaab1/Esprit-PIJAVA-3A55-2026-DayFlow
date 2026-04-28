package controllers.admin;

import enums.PostStatus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.interaction.Comment;
import model.user.User;
import services.account.UserService;
import services.admin.AdminPostService;
import services.admin.AdminPostService.AdminPostDetailsRow;
import services.interaction.CommentService;
import utils.HtmlPlainText;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdminPostDetailsController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.FRENCH);

    private final AdminPostService adminPostService = new AdminPostService();
    private final CommentService commentService = new CommentService();
    private final UserService userService = new UserService();

    private AdminShellController shell;
    private Integer postId;

    @FXML
    private Label pageTitle;
    @FXML
    private Label postTitleLabel;
    @FXML
    private Label authorAvatar;
    @FXML
    private Label authorNameLabel;
    @FXML
    private Label authorEmailLabel;
    @FXML
    private Label publishDateLabel;
    @FXML
    private Label publishTimeLabel;
    @FXML
    private Label postStatusLabel;
    @FXML
    private Label postTagsLabel;
    @FXML
    private Label viewsLabel;
    @FXML
    private Label clicksLabel;
    @FXML
    private Label likesLabel;
    @FXML
    private Label commentsCountLabel;
    @FXML
    private Label ctrLabel;
    @FXML
    private Label trendLabel;
    @FXML
    private BarChart<String, Number> analyticsChart;
    @FXML
    private Label postContentLabel;
    @FXML
    private ComboBox<String> commentsSortCombo;
    @FXML
    private VBox commentsBox;
    @FXML
    private Button backBtn;

    private List<Comment> loadedComments = new ArrayList<>();
    private AdminPostDetailsRow currentPostDetails;

    @FXML
    private void initialize() {
        commentsSortCombo.setItems(FXCollections.observableArrayList(
                "Plus récents",
                "Plus anciens"
        ));
        commentsSortCombo.getSelectionModel().selectFirst();
    }

    public void setContext(AdminShellController shell, int postId) {
        this.shell = shell;
        this.postId = postId;
        loadData();
    }

    @FXML
    private void onBack() {
        if (shell == null) {
            return;
        }
        try {
            shell.loadPosts();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Retour publications : " + e.getMessage()).showAndWait();
        }
    }

    private void loadData() {
        if (postId == null) {
            return;
        }
        try {
            AdminPostDetailsRow details = adminPostService.findPostDetails(postId);
            if (details == null) {
                new Alert(Alert.AlertType.WARNING, "Post introuvable.").showAndWait();
                onBack();
                return;
            }

            currentPostDetails = details;

            pageTitle.setText("Détail du post #" + details.id());
            postTitleLabel.setText(details.title().isBlank() ? "Sans titre" : details.title());
            
            // Author info
            String authorName = details.authorFullName().isBlank() ? "Utilisateur" : details.authorFullName();
            String authorEmail = details.authorEmail().isBlank() ? "—" : details.authorEmail();
            authorNameLabel.setText(authorName);
            authorEmailLabel.setText(authorEmail);
            
            // Avatar initials
            String initials = getInitials(authorName);
            authorAvatar.setText(initials);
            
            // Date and time
            if (details.createdAt() != null) {
                publishDateLabel.setText(details.createdAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)));
                publishTimeLabel.setText(details.createdAt().format(DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)));
            } else {
                publishDateLabel.setText("—");
                publishTimeLabel.setText("");
            }
            
            // Status badge
            postStatusLabel.setText(statusFr(PostStatus.fromValue(details.statusRaw()), details.statusRaw()));
            updateStatusBadgeStyle(PostStatus.fromValue(details.statusRaw()));
            
            // Tags
            postTagsLabel.setText(details.tagsSummary().isBlank() ? "Aucun tag" : details.tagsSummary());

            // Analytics
            viewsLabel.setText(Integer.toString(details.viewCount()));
            clicksLabel.setText(Integer.toString(details.clickCount()));
            likesLabel.setText(Integer.toString(details.likeCount()));
            commentsCountLabel.setText(Integer.toString(details.commentCount()));
            ctrLabel.setText(AdminPostService.ctrLabel(details.viewCount(), details.clickCount()));
            trendLabel.setText(AdminPostService.trendLabel(details.viewCount(), details.clickCount()));
            
            // Content
            postContentLabel.setText(HtmlPlainText.toPlain(details.content()));
            
            updateAnalyticsChart(details.viewCount(), details.clickCount());

            loadedComments = new ArrayList<>(commentService.getCommentsByPost(details.id()));
            applyCommentsSort();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Chargement détails post : " + e.getMessage()).showAndWait();
        }
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private void updateStatusBadgeStyle(PostStatus status) {
        postStatusLabel.getStyleClass().removeAll("badge-published", "badge-draft-post", "badge-scheduled", "badge-hidden");
        if (status == PostStatus.PUBLISHED) {
            postStatusLabel.getStyleClass().add("badge-published");
        } else if (status == PostStatus.DRAFT) {
            postStatusLabel.getStyleClass().add("badge-draft-post");
        } else if (status == PostStatus.SCHEDULED) {
            postStatusLabel.getStyleClass().add("badge-scheduled");
        } else if (status == PostStatus.HIDDEN) {
            postStatusLabel.getStyleClass().add("badge-hidden");
        }
    }

    @FXML
    private void onCommentsSortChange() {
        applyCommentsSort();
    }

    private void applyCommentsSort() {
        List<Comment> sorted = new ArrayList<>(loadedComments);
        String selectedSort = commentsSortCombo != null ? commentsSortCombo.getValue() : "Plus récents";
        Comparator<Comment> comparator = Comparator.comparing(
                Comment::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        );
        if ("Plus récents".equals(selectedSort)) {
            comparator = comparator.reversed();
        }
        sorted.sort(comparator);
        renderComments(sorted);
    }

    private void updateAnalyticsChart(int viewCount, int clickCount) {
        analyticsChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Interactions");
        series.getData().add(new XYChart.Data<>("Vues", viewCount));
        series.getData().add(new XYChart.Data<>("Clics", clickCount));
        analyticsChart.getData().add(series);
        analyticsChart.setLegendVisible(false);
        analyticsChart.setAnimated(false);
    }

    private void renderComments(List<Comment> comments) {
        commentsBox.getChildren().clear();
        if (comments == null || comments.isEmpty()) {
            Label empty = new Label("Aucun commentaire pour le moment.");
            empty.getStyleClass().add("details-empty-state");
            commentsBox.getChildren().add(empty);
            return;
        }
        for (Comment c : comments) {
            commentsBox.getChildren().add(buildCommentCard(c));
        }
    }

    private VBox buildCommentCard(Comment comment) {
        String author = "Utilisateur #" + comment.getCommenterId();
        try {
            User u = userService.findById(comment.getCommenterId()).orElse(null);
            if (u != null) {
                String fullName = ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                        + (u.getLastName() != null ? u.getLastName() : "")).trim();
                if (!fullName.isBlank()) {
                    author = fullName;
                }
            }
        } catch (SQLException ignored) {
        }

        VBox card = new VBox(8);
        card.getStyleClass().add("details-comment-item");
        
        String dateStr = comment.getCreatedAt() != null ? comment.getCreatedAt().format(DATE_FMT) : "—";
        Label meta = new Label(author + " • " + dateStr);
        meta.getStyleClass().add("details-comment-meta");
        
        Label content = new Label(HtmlPlainText.toPlain(comment.getContent()));
        content.setWrapText(true);
        content.getStyleClass().add("details-comment-content");
        
        card.getChildren().addAll(meta, content);
        return card;
    }

    @FXML
    private void onDeletePost() {
        if (currentPostDetails == null) {
            return;
        }
        new Alert(Alert.AlertType.INFORMATION, "Fonctionnalité Supprimer : à implémenter").showAndWait();
    }

    private static String statusFr(PostStatus st, String raw) {
        if (st == PostStatus.PUBLISHED) {
            return "Publié";
        }
        if (st == PostStatus.DRAFT) {
            return "Brouillon";
        }
        if (st == PostStatus.SCHEDULED) {
            return "Planifié";
        }
        if (st == PostStatus.HIDDEN) {
            return "Masqué";
        }
        return raw == null || raw.isBlank() ? "—" : raw;
    }
}

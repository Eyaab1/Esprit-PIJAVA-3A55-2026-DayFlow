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
    private Label postMetaLabel;
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

            pageTitle.setText("Détail du post #" + details.id());
            postTitleLabel.setText(details.title().isBlank() ? "Sans titre" : details.title());
            String author = details.authorFullName().isBlank() ? "—" : details.authorFullName();
            String date = details.createdAt() != null ? details.createdAt().format(DATE_FMT) : "—";
            postMetaLabel.setText(author + " • " + date + " • " + (details.authorEmail().isBlank() ? "—" : details.authorEmail()));
            postStatusLabel.setText(statusFr(PostStatus.fromValue(details.statusRaw()), details.statusRaw()));
            postTagsLabel.setText(details.tagsSummary().isBlank() ? "—" : details.tagsSummary());

            viewsLabel.setText(Integer.toString(details.viewCount()));
            clicksLabel.setText(Integer.toString(details.clickCount()));
            likesLabel.setText(Integer.toString(details.likeCount()));
            commentsCountLabel.setText(Integer.toString(details.commentCount()));
            ctrLabel.setText(AdminPostService.ctrLabel(details.viewCount(), details.clickCount()));
            trendLabel.setText(AdminPostService.trendLabel(details.viewCount(), details.clickCount()));
            postContentLabel.setText(HtmlPlainText.toPlain(details.content()));
            updateAnalyticsChart(details.viewCount(), details.clickCount());

            loadedComments = new ArrayList<>(commentService.getCommentsByPost(details.id()));
            applyCommentsSort();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Chargement détails post : " + e.getMessage()).showAndWait();
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
            Label empty = new Label("Aucun commentaire.");
            empty.getStyleClass().add("admin-page-sub");
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

        VBox card = new VBox(6);
        card.getStyleClass().add("admin-card");
        Label meta = new Label(author + " • " + (comment.getCreatedAt() != null ? comment.getCreatedAt().format(DATE_FMT) : "—"));
        meta.setStyle("-fx-text-fill:#64748b; -fx-font-size:12px;");
        Label content = new Label(HtmlPlainText.toPlain(comment.getContent()));
        content.setWrapText(true);
        content.setStyle("-fx-font-size:13px; -fx-text-fill:#1f2937;");
        card.getChildren().addAll(meta, content);
        return card;
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

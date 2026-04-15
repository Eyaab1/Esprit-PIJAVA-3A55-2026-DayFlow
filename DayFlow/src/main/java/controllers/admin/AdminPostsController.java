package controllers.admin;

import enums.PostStatus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.interaction.Tag;
import services.admin.AdminPostService;
import services.admin.AdminPostService.AdminPostRow;
import services.admin.AdminPostService.SortOrder;
import services.admin.AdminPostService.TrendFilter;
import services.interaction.TagService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Administration des publications (filtres + tableau).
 */
public class AdminPostsController {

    private static final int FETCH_LIMIT = 400;

    private final AdminPostService postService = new AdminPostService();
    private final TagService tagService = new TagService();

    @FXML
    private TextField authorNameField;
    @FXML
    private TextField authorEmailField;
    @FXML
    private ComboBox<TagOption> tagCombo;
    @FXML
    private ComboBox<TrendOption> trendCombo;
    @FXML
    private ComboBox<SortOption> sortCombo;
    @FXML
    private VBox postsRowsBox;

    public record TagOption(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public record TrendOption(TrendFilter filter, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public record SortOption(SortOrder order, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    @FXML
    private void initialize() {
        trendCombo.setItems(FXCollections.observableArrayList(
                new TrendOption(TrendFilter.ALL, "Tous les posts"),
                new TrendOption(TrendFilter.TRENDING, "Tendance"),
                new TrendOption(TrendFilter.STABLE, "Stable"),
                new TrendOption(TrendFilter.DECLINING, "En baisse")
        ));
        trendCombo.getSelectionModel().selectFirst();

        sortCombo.setItems(FXCollections.observableArrayList(
                new SortOption(SortOrder.NEWEST, "Plus récents"),
                new SortOption(SortOrder.OLDEST, "Plus anciens")
        ));
        sortCombo.getSelectionModel().selectFirst();

        List<TagOption> tags = new ArrayList<>();
        tags.add(new TagOption(0, "Tous les tags"));
        try {
            for (Tag t : tagService.findAll()) {
                if (t.getId() != null) {
                    tags.add(new TagOption(t.getId(), t.getName() != null ? t.getName() : ("#" + t.getId())));
                }
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.WARNING, "Tags : " + e.getMessage()).showAndWait();
        }
        tagCombo.setItems(FXCollections.observableArrayList(tags));
        tagCombo.getSelectionModel().selectFirst();

        onFilter();
    }

    @FXML
    private void onFilter() {
        String name = authorNameField != null ? authorNameField.getText() : "";
        String email = authorEmailField != null ? authorEmailField.getText() : "";
        TagOption tag = tagCombo != null && tagCombo.getValue() != null ? tagCombo.getValue() : new TagOption(0, "");
        TrendFilter trend = trendCombo != null && trendCombo.getValue() != null
                ? trendCombo.getValue().filter()
                : TrendFilter.ALL;
        SortOrder sort = sortCombo != null && sortCombo.getValue() != null
                ? sortCombo.getValue().order()
                : SortOrder.NEWEST;

        try {
            List<AdminPostRow> raw = postService.searchPosts(name, email, tag.id(), sort, FETCH_LIMIT);
            List<AdminPostRow> filtered = raw.stream()
                    .filter(r -> AdminPostService.matchesTrend(r, trend))
                    .limit(150)
                    .toList();
            renderRows(filtered);
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Publications : " + ex.getMessage()).showAndWait();
        }
    }

    private void renderRows(List<AdminPostRow> rows) {
        postsRowsBox.getChildren().clear();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);
        for (AdminPostRow r : rows) {
            postsRowsBox.getChildren().add(buildPostRow(r, df));
        }
    }

    private HBox buildPostRow(AdminPostRow r, DateTimeFormatter df) {
        VBox titleCol = new VBox(4);
        titleCol.setMinWidth(280);
        titleCol.setPrefWidth(320);
        HBox.setHgrow(titleCol, Priority.SOMETIMES);
        Label title = new Label("📄 " + (r.title().isBlank() ? "Sans titre" : r.title()));
        title.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");
        title.setWrapText(true);
        String snip = r.contentSnippet().isBlank() ? "—" : r.contentSnippet();
        Label snippet = new Label(snip);
        snippet.setStyle("-fx-text-fill:#64748b;-fx-font-size:11px;");
        snippet.setWrapText(true);
        titleCol.getChildren().addAll(title, snippet);

        VBox authorCol = new VBox(8);
        authorCol.setMinWidth(150);
        authorCol.setPrefWidth(170);
        String initials = AdminPostService.initials(r.authorFirstName(), r.authorLastName());
        StackPane av = new StackPane(new Label(initials.isBlank() ? "?" : initials));
        av.getStyleClass().add("admin-avatar");
        av.setMaxWidth(40);
        av.setMaxHeight(40);
        Label an = new Label(r.authorFullName().isBlank() ? "—" : r.authorFullName());
        an.setStyle("-fx-font-size:13px;");
        authorCol.getChildren().addAll(av, an);

        String dateStr = r.createdAt() != null ? df.format(r.createdAt()) : "—";
        Label dateLbl = new Label(dateStr);
        dateLbl.setMinWidth(110);
        dateLbl.setStyle("-fx-font-size:12px;");

        Label ctr = new Label(AdminPostService.ctrLabel(r.viewCount(), r.clickCount()));
        ctr.setMinWidth(72);
        ctr.setStyle("-fx-font-size:12px;");

        Label tr = new Label(trendEmoji(r) + " " + AdminPostService.trendLabel(r.viewCount(), r.clickCount()));
        tr.setMinWidth(100);
        tr.setStyle("-fx-font-size:12px;");

        PostStatus st = PostStatus.fromValue(r.statusRaw());
        Label status = new Label(statusFr(st, r.statusRaw()));
        status.setMinWidth(92);
        status.getStyleClass().addAll("admin-badge", statusBadgeClass(st, r.statusRaw()));

        Button more = new Button("⋯");
        more.setStyle("-fx-background-color:#f1f5f9;-fx-background-radius:8;-fx-cursor:hand;");
        more.setOnAction(e -> onPostActions(r));

        HBox row = new HBox(12, titleCol, authorCol, dateLbl, ctr, tr, status, more);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(12, 10, 12, 10));
        row.getStyleClass().add("admin-post-row");
        return row;
    }

    private static String trendEmoji(AdminPostRow r) {
        String t = AdminPostService.trendLabel(r.viewCount(), r.clickCount());
        if ("Tendance".equals(t)) {
            return "🔥";
        }
        if ("En baisse".equals(t)) {
            return "↓";
        }
        return "—";
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

    private static String statusBadgeClass(PostStatus st, String raw) {
        if (st == PostStatus.PUBLISHED) {
            return "badge-published";
        }
        if (st == PostStatus.DRAFT) {
            return "badge-draft-post";
        }
        return "badge-default";
    }

    private static void onPostActions(AdminPostRow r) {
        String tags = r.tagsSummary() == null || r.tagsSummary().isBlank() ? "—" : r.tagsSummary();
        new Alert(Alert.AlertType.INFORMATION,
                "Post #" + r.id() + "\nTags : " + tags + "\nVues : " + r.viewCount() + " · Clics : " + r.clickCount()
        ).showAndWait();
    }
}

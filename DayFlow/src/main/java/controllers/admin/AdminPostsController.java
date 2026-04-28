package controllers.admin;

import enums.PostStatus;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import model.interaction.Tag;
import services.admin.AdminPostService;
import services.admin.AdminPostService.AdminPostRow;
import services.admin.AdminPostService.SortOrder;
import services.admin.AdminPostService.TrendFilter;
import services.interaction.TagService;

import java.sql.SQLException;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Administration des publications (filtres + tableau).
 */
public class AdminPostsController {

    private static final int FETCH_LIMIT = 400;
    private static final double COL_TITLE = 360;
    private static final double COL_AUTHOR = 170;
    private static final double COL_DATE = 110;
    private static final double COL_VIEWS = 60;
    private static final double COL_CLICKS = 60;
    private static final double COL_CTR = 70;
    private static final double COL_TREND = 120;
    private static final double COL_STATUS = 95;
    private static final double COL_ACTIONS = 44;

    private final AdminPostService adminPostService = new AdminPostService();
    private final TagService tagService = new TagService();
    private final Map<Integer, Integer> pendingViewIncrements = new HashMap<>();
    private final PauseTransition flushViewsDebounce = new PauseTransition(Duration.millis(250));

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
    @FXML
    private ScrollPane postsRowsScrollPane;

    private final Map<Integer, HBox> renderedRows = new HashMap<>();
    private final Set<Integer> currentlyVisiblePostIds = new HashSet<>();
    private AdminShellController shell;

    public void setShell(AdminShellController shell) {
        this.shell = shell;
    }

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

        flushViewsDebounce.setOnFinished(e -> flushPendingViewIncrements());
        if (postsRowsScrollPane != null) {
            postsRowsScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> evaluateVisibleRowsAndQueueViews());
            postsRowsScrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> evaluateVisibleRowsAndQueueViews());
        }
        postsRowsBox.heightProperty().addListener((obs, oldVal, newVal) -> evaluateVisibleRowsAndQueueViews());
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
            List<AdminPostRow> raw = adminPostService.searchPosts(name, email, tag.id(), sort, FETCH_LIMIT);
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
        renderedRows.clear();
        currentlyVisiblePostIds.clear();
        pendingViewIncrements.clear();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);
        for (AdminPostRow r : rows) {
            HBox rowNode = buildPostRow(r, df);
            renderedRows.put(r.id(), rowNode);
            postsRowsBox.getChildren().add(rowNode);
        }
        javafx.application.Platform.runLater(this::evaluateVisibleRowsAndQueueViews);
    }

    private HBox buildPostRow(AdminPostRow r, DateTimeFormatter df) {
        VBox titleCol = new VBox(4);
        setFixedWidth(titleCol, COL_TITLE);
        HBox.setHgrow(titleCol, Priority.NEVER);
        Label title = new Label("📄 " + (r.title().isBlank() ? "Sans titre" : r.title()));
        title.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");
        title.setWrapText(true);
        title.setMaxWidth(COL_TITLE);
        String snip = r.contentSnippet().isBlank() ? "—" : r.contentSnippet();
        Label snippet = new Label(snip);
        snippet.setStyle("-fx-text-fill:#64748b;-fx-font-size:11px;");
        snippet.setWrapText(true);
        snippet.setMaxWidth(COL_TITLE);
        titleCol.getChildren().addAll(title, snippet);

        VBox authorCol = new VBox(8);
        setFixedWidth(authorCol, COL_AUTHOR);
        String initials = AdminPostService.initials(r.authorFirstName(), r.authorLastName());
        StackPane av = new StackPane(new Label(initials.isBlank() ? "?" : initials));
        av.getStyleClass().add("admin-avatar");
        av.setMaxWidth(40);
        av.setMaxHeight(40);
        Label an = new Label(r.authorFullName().isBlank() ? "—" : r.authorFullName());
        an.setStyle("-fx-font-size:13px;");
        an.setWrapText(true);
        an.setMaxWidth(COL_AUTHOR);
        authorCol.getChildren().addAll(av, an);

        String dateStr = r.createdAt() != null ? df.format(r.createdAt()) : "—";
        Label dateLbl = new Label(dateStr);
        setFixedWidth(dateLbl, COL_DATE);
        dateLbl.setStyle("-fx-font-size:12px;");

        Label views = new Label(Integer.toString(r.viewCount()));
        setFixedWidth(views, COL_VIEWS);
        views.setStyle("-fx-font-size:12px;");

        Label clicks = new Label(Integer.toString(r.clickCount()));
        setFixedWidth(clicks, COL_CLICKS);
        clicks.setStyle("-fx-font-size:12px;");

        Label ctr = new Label(AdminPostService.ctrLabel(r.viewCount(), r.clickCount()));
        setFixedWidth(ctr, COL_CTR);
        ctr.setStyle("-fx-font-size:12px;");

        Label tr = new Label(trendEmoji(r) + " " + AdminPostService.trendLabel(r.viewCount(), r.clickCount()));
        setFixedWidth(tr, COL_TREND);
        tr.setStyle("-fx-font-size:12px;");

        PostStatus st = PostStatus.fromValue(r.statusRaw());
        Label status = new Label(statusFr(st, r.statusRaw()));
        setFixedWidth(status, COL_STATUS);
        status.getStyleClass().addAll("admin-badge", statusBadgeClass(st, r.statusRaw()));

        Button more = new Button("⋯");
        setFixedWidth(more, COL_ACTIONS);
        more.setStyle("-fx-background-color:#f1f5f9;-fx-background-radius:8;-fx-cursor:hand;");
        more.setOnAction(e -> {
            incrementClickAnalytics(r.id());
            openPostDetails(r.id());
        });

        HBox row = new HBox(12, titleCol, authorCol, dateLbl, views, clicks, ctr, tr, status, more);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(12, 10, 12, 10));
        row.getStyleClass().add("admin-post-row");
        row.setOnMouseClicked(e -> {
            Node target = e.getPickResult() != null ? e.getPickResult().getIntersectedNode() : null;
            while (target != null) {
                if (target instanceof Button) {
                    return;
                }
                target = target.getParent();
            }
            incrementClickAnalytics(r.id());
        });
        return row;
    }

    private static void setFixedWidth(Region region, double width) {
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }

    private void flushPendingViewIncrements() {
        if (pendingViewIncrements.isEmpty()) {
            return;
        }
        Map<Integer, Integer> batch = new HashMap<>(pendingViewIncrements);
        pendingViewIncrements.clear();
        try {
            adminPostService.batchIncrementViewCounts(batch);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Analytics vues : " + e.getMessage()).showAndWait();
        }
    }

    private void evaluateVisibleRowsAndQueueViews() {
        if (postsRowsScrollPane == null || postsRowsBox == null || renderedRows.isEmpty()) {
            return;
        }
        double contentHeight = postsRowsBox.getBoundsInLocal().getHeight();
        double viewportHeight = postsRowsScrollPane.getViewportBounds().getHeight();
        if (contentHeight <= 0 || viewportHeight <= 0) {
            return;
        }
        double maxScroll = Math.max(0, contentHeight - viewportHeight);
        double visibleMinY = postsRowsScrollPane.getVvalue() * maxScroll;
        double visibleMaxY = visibleMinY + viewportHeight;

        Set<Integer> visibleNow = new HashSet<>();
        for (Map.Entry<Integer, HBox> entry : renderedRows.entrySet()) {
            Integer postId = entry.getKey();
            HBox row = entry.getValue();
            if (postId == null || row == null) {
                continue;
            }
            double rowMinY = row.getBoundsInParent().getMinY();
            double rowMaxY = row.getBoundsInParent().getMaxY();
            boolean visible = rowMaxY > visibleMinY && rowMinY < visibleMaxY;
            if (visible) {
                visibleNow.add(postId);
                if (!currentlyVisiblePostIds.contains(postId)) {
                    pendingViewIncrements.merge(postId, 1, Integer::sum);
                }
            }
        }

        currentlyVisiblePostIds.clear();
        currentlyVisiblePostIds.addAll(visibleNow);
        if (!pendingViewIncrements.isEmpty()) {
            flushViewsDebounce.playFromStart();
        }
    }

    private void incrementClickAnalytics(int postId) {
        try {
            adminPostService.incrementClickCount(postId);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Analytics clics : " + e.getMessage()).showAndWait();
        }
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

    private void openPostDetails(int postId) {
        if (shell == null) {
            return;
        }
        try {
            shell.loadPostDetails(postId);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Ouverture détail post : " + e.getMessage()).showAndWait();
        }
    }
}
